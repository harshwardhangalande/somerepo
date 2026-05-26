import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

public class ExamServerGUI_AES extends JFrame {
    private ServerSocket serverSocket;
    private Map<String, String> qpDatabase;
    private JTextArea logArea;

    public ExamServerGUI_AES() {
        qpDatabase = new HashMap<>();
        qpDatabase.put("10-CS101-1", "Question 1: What is Java?\nQuestion 2: Explain OOP.");
        
        setTitle("Secure Server (DH Handshake)");
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        logArea = new JTextArea();
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        JButton btn = new JButton("Start Server");
        btn.addActionListener(e -> startServer());
        add(btn, BorderLayout.SOUTH);
        setVisible(true);
    }

    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(8080);
                appendLog("Server active on 8080...");
                while (true) { handleClient(serverSocket.accept()); }
            } catch (Exception ex) { appendLog("Error: " + ex.getMessage()); }
        }).start();
    }

    private void handleClient(Socket client) {
        new Thread(() -> {
            try (ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(client.getInputStream())) {

                // 1. Generate Server DH Keys
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
                kpg.initialize(512);
                KeyPair serverKP = kpg.generateKeyPair();
                
                appendLog("\n--- DIFFIE-HELLMAN HANDSHAKE ---");
                appendLog("SERVER PRIVATE KEY: " + encode(serverKP.getPrivate().getEncoded()));
                appendLog("SERVER PUBLIC KEY:  " + encode(serverKP.getPublic().getEncoded()));

                // 2. Exchange
                byte[] clientPubKeyEnc = (byte[]) in.readObject();
                appendLog("RECEIVED CLIENT PUB: " + encode(clientPubKeyEnc));
                
                out.writeObject(serverKP.getPublic().getEncoded());
                out.flush();

                // 3. Compute Secret
                KeyFactory kf = KeyFactory.getInstance("DH");
                PublicKey clientPubKey = kf.generatePublic(new X509EncodedKeySpec(clientPubKeyEnc));
                KeyAgreement ka = KeyAgreement.getInstance("DH");
                ka.init(serverKP.getPrivate());
                ka.doPhase(clientPubKey, true);
                byte[] sharedSecret = ka.generateSecret();
                
                appendLog("SERVER SHARED SECRET: " + encode(sharedSecret));

                // 4. Decrypt Request & Encrypt Response
                String dbKey = in.readUTF();
                String qp = qpDatabase.getOrDefault(dbKey, "Error: QP Not Found");
                
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sharedSecret, 0, 16, "AES"));
                out.writeObject(cipher.doFinal(qp.getBytes("UTF-8")));
                
                appendLog("Encrypted QP sent to client.");

            } catch (Exception e) { appendLog("Client Error: " + e.getMessage()); }
        }).start();
    }

    private String encode(byte[] data) { return Base64.getEncoder().encodeToString(data).substring(0, 40) + "..."; }
    private void appendLog(String m) { SwingUtilities.invokeLater(() -> logArea.append(m + "\n")); }
    public static void main(String[] args) { SwingUtilities.invokeLater(ExamServerGUI_AES::new); }
}
