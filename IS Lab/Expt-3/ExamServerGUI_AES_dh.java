import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class ExamServerGUI_AES_dh extends JFrame {

    private ServerSocket serverSocket;
    private Map<String, String> qpDatabase;
    private JTextArea logArea;

    public ExamServerGUI_AES_dh() {

        qpDatabase = new HashMap<>();
        qpDatabase.put("10-CS101-1",
                "Question 1: What is Java? (5 marks)\nQuestion 2: Explain OOP. (10 marks)");
        qpDatabase.put("10-CS301-3",
                "Question 1: Socket Programming. (8 marks)\nQuestion 2: Caesar Cipher impl. (12 marks)");

        setTitle("IS Lab Exam Server - DH + AES Secure QP Delivery");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JButton startBtn = new JButton("Start Server (Port 8080)");
        startBtn.addActionListener(e -> startServer());
        add(startBtn, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(8080);
                appendLog("Server started on port 8080...");
                while (true) {
                    Socket client = serverSocket.accept();
                    appendLog("Client connected: " + client.getInetAddress());
                    handleClient(client);
                }
            } catch (Exception ex) {
                appendLog("Server error: " + ex.getMessage());
            }
        }).start();
    }

    private void handleClient(Socket client) {
        new Thread(() -> {
            try (
                    ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(client.getInputStream());
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(client.getInputStream()))) {



                                

                KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
                kpg.initialize(512);
                KeyPair serverKP = kpg.generateKeyPair();

                out.writeObject(serverKP.getPublic().getEncoded());
                out.flush();

                byte[] clientPubKeyEnc = (byte[]) in.readObject();
                KeyFactory kf = KeyFactory.getInstance("DH");
                X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(clientPubKeyEnc);
                PublicKey clientPubKey = kf.generatePublic(x509Spec);

                KeyAgreement ka = KeyAgreement.getInstance("DH");
                ka.init(serverKP.getPrivate());
                ka.doPhase(clientPubKey, true);
                byte[] sharedSecret = ka.generateSecret();

                String sharedSecretBase64 =
                        Base64.getEncoder().encodeToString(sharedSecret);

                appendLog("Shared Secret (Base64): " + sharedSecretBase64);

                SecretKey aesKey =
                        new SecretKeySpec(sharedSecret, 0, 16, "AES");





                String cls = br.readLine();
                String courseCode = br.readLine();
                br.readLine();
                String sem = br.readLine();

                String key = cls + "-" + courseCode + "-" + sem;
                appendLog("Request: " + key);

                String qp = qpDatabase.getOrDefault(key, "ERROR: QP not found");
                if (qp.startsWith("ERROR")) {
                    out.writeObject("ERROR");
                    out.writeObject(qp);
                    return;
                }




                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, aesKey);
                byte[] encryptedQP = cipher.doFinal(qp.getBytes("UTF-8"));

                out.writeObject("OK");
                out.writeObject(encryptedQP);
                out.flush();

                appendLog("Encrypted QP sent.");

            } catch (Exception e) {
                appendLog("Client error: " + e.getMessage());
            }
        }).start();
    }

    private void appendLog(String msg) {
        logArea.append(msg + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExamServerGUI_AES_dh::new);
    }
}