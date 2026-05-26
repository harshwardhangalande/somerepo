import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.*;
import javax.swing.*;
import java.security.*;
import java.util.Base64;

public class ExamServerGUI_AES extends JFrame {

    private ServerSocket serverSocket;
    private Map<String, String> qpDatabase;
    private JTextArea logArea;

    // AES key (generated once)
    private SecretKey aesKey;

    public ExamServerGUI_AES() {

        qpDatabase = new HashMap<>();
        qpDatabase.put("10-CS101-1",
                "Question 1: What is Java? (5 marks)\nQuestion 2: Explain OOP. (10 marks)");
        qpDatabase.put("10-CS301-3",
                "Question 1: Socket Programming. (8 marks)\nQuestion 2: Caesar Cipher impl. (12 marks)");

        setTitle("IS Lab Exam Server - AES Secure QP Delivery");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        generateAESKey();

        JButton startBtn = new JButton("Start Server (Port 8080)");
        startBtn.addActionListener(e -> startServer());
        add(startBtn, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void generateAESKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            aesKey = keyGenerator.generateKey();
            appendLog("AES Key Generated (Base64): "
                    + Base64.getEncoder().encodeToString(aesKey.getEncoded()));
        } catch (Exception e) {
            appendLog("AES Key generation failed: " + e.getMessage());
        }
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
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(client.getInputStream()));
                    DataOutputStream dos = new DataOutputStream(client.getOutputStream())) {

                String cls = in.readLine();
                String courseCode = in.readLine();
                in.readLine();
                String sem = in.readLine();

                String key = cls + "-" + courseCode + "-" + sem;
                appendLog("Request: " + key);

                String qp = qpDatabase.getOrDefault(key, "ERROR: QP not found");
                if (qp.startsWith("ERROR")) {
                    dos.writeUTF("ERROR");
                    dos.writeUTF(qp);
                    return;
                }

                byte[] encryptedQP = encryptAES(qp, aesKey);

                dos.writeUTF("OK");
                dos.writeInt(encryptedQP.length);
                dos.writeUTF(Base64.getEncoder().encodeToString(aesKey.getEncoded()));
                dos.write(encryptedQP);

                appendLog("Encrypted QP sent (" + encryptedQP.length + " bytes)");

            } catch (Exception e) {
                appendLog("Client error: " + e.getMessage());
            }
        }).start();
    }

    private byte[] encryptAES(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data.getBytes("UTF-8"));
    }

    private void appendLog(String msg) {
        logArea.append(msg + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExamServerGUI_AES::new);
    }
}