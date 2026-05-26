import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

public class ExamServerRSA extends JFrame {

        private ServerSocket serverSocket;
        private JTextArea logArea;
        private Map<String, String> qpDatabase;

        private PrivateKey privateKey;
        private PublicKey publicKey;

        public ExamServerRSA() {
                setTitle("Exam Server - RSA Digital Signature");
                setSize(600, 400);
                setDefaultCloseOperation(EXIT_ON_CLOSE);
                setLayout(new BorderLayout());

                logArea = new JTextArea();
                add(new JScrollPane(logArea), BorderLayout.CENTER);

                JButton startBtn = new JButton("Start Server");
                startBtn.addActionListener(e -> startServer());
                add(startBtn, BorderLayout.SOUTH);

                // Sample QP DB
                qpDatabase = new HashMap<>();
                qpDatabase.put("10-CS101-1", "Q1: What is Java?\nQ2: Explain OOP.");

                // Generate RSA Keys
                generateKeys();

                setVisible(true);
        }

        private void generateKeys() {
                try {
                        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                        keyGen.initialize(2048);
                        KeyPair pair = keyGen.generateKeyPair();

                        privateKey = pair.getPrivate();
                        publicKey = pair.getPublic();

                        appendLog("RSA Keys Generated");
                } catch (Exception e) {
                        appendLog("Key Error: " + e.getMessage());
                }
        }

        private void startServer() {
                new Thread(() -> {
                        try {
                                serverSocket = new ServerSocket(8080);
                                appendLog("Server Started...");

                                while (true) {
                                        Socket client = serverSocket.accept();
                                        appendLog("Client Connected");
                                        handleClient(client);
                                }
                        } catch (Exception e) {
                                appendLog("Server Error: " + e.getMessage());
                        }
                }).start();
        }

        private void handleClient(Socket client) {
                new Thread(() -> {
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                                        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                                        DataOutputStream dos = new DataOutputStream(client.getOutputStream())) {

                                String cls = in.readLine();
                                String code = in.readLine();
                                String name = in.readLine();
                                String sem = in.readLine();

                                String key = cls + "-" + code + "-" + sem;
                                String qp = qpDatabase.getOrDefault(key, "ERROR: Not Found");

                                if (qp.startsWith("ERROR")) {
                                        out.println("ERROR");
                                        out.println(qp);
                                        return;
                                }

                                // 🔐 Create Digital Signature
                                byte[] signature = signData(qp, privateKey);
                                String signatureStr = Base64.getEncoder().encodeToString(signature);

                                String pubKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());

                                out.println("OK");
                                out.println(qp.length());
                                out.println(signatureStr);
                                out.println(pubKeyStr);

                                dos.write(qp.getBytes("UTF-8"));

                                appendLog("QP sent with digital signature");

                        } catch (Exception e) {
                                appendLog("Client Error: " + e.getMessage());
                        }
                }).start();
        }

        // SIGNING METHOD
        private byte[] signData(String data, PrivateKey key) throws Exception {
                Signature sign = Signature.getInstance("SHA256withRSA");
                sign.initSign(key);
                sign.update(data.getBytes());
                return sign.sign();
        }

        private void appendLog(String msg) {
                logArea.append(msg + "\n");
        }

        public static void main(String[] args) {
                SwingUtilities.invokeLater(ExamServerRSA::new);
        }
}