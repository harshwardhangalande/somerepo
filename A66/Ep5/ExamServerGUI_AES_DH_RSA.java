import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

public class ExamServerGUI_AES_DH_RSA extends JFrame {
    private ServerSocket serverSocket;
    private Map<String, String> qpDatabase = new HashMap<>();
    private JTextArea logArea;
    private KeyPair rsaKeyPair;

    public ExamServerGUI_AES_DH_RSA() {
        // Setup Dummy Database
        qpDatabase.put("10-CS101-1", "1. Define JVM.\n2. Explain AES vs RSA.\n3. Write a Socket program.");

        // Generate RSA Keys for Signing
        try {
            KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
            rsaGen.initialize(2048);
            rsaKeyPair = rsaGen.generateKeyPair();
        } catch (Exception e) { e.printStackTrace(); }

        setTitle("Exam Server (AES + DH + RSA)");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
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
                appendLog("Server started on 8080...\n");
                while (true) {
                    Socket client = serverSocket.accept();
                    new Thread(() -> handleClient(client)).start();
                }
            } catch (Exception ex) { appendLog("Server Error: " + ex.getMessage()); }
        }).start();
    }

    private void handleClient(Socket client) {
        try (DataInputStream dis = new DataInputStream(client.getInputStream());
             DataOutputStream dos = new DataOutputStream(client.getOutputStream())) {

            appendLog("Client Connected: " + client.getInetAddress() + "\n");

            // Read Request Metadata
            String cls = dis.readUTF();
            String code = dis.readUTF();
            String name = dis.readUTF();
            String sem = dis.readUTF();
            String key = cls + "-" + code + "-" + sem;
            
            String qp = qpDatabase.get(key);
            if (qp == null) {
                dos.writeUTF("ERROR: QP Not Found");
                return;
            }

            // 1. DH Key Exchange
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("DiffieHellman");
            kpg.initialize(2048);
            KeyPair serverPair = kpg.generateKeyPair();
            KeyAgreement ka = KeyAgreement.getInstance("DiffieHellman");
            ka.init(serverPair.getPrivate());

            // Send Server DH Public Key FIRST
            byte[] serverPubBytes = serverPair.getPublic().getEncoded();
            dos.writeInt(serverPubBytes.length);
            dos.write(serverPubBytes);
            dos.flush();

            // Receive Client DH Public Key
            int clientKeyLen = dis.readInt();
            byte[] clientPubBytes = new byte[clientKeyLen];
            dis.readFully(clientPubBytes);
            KeyFactory kf = KeyFactory.getInstance("DiffieHellman");
            PublicKey clientPub = kf.generatePublic(new X509EncodedKeySpec(clientPubBytes));
            ka.doPhase(clientPub, true);

            // Derive AES Key
            byte[] sharedSecret = ka.generateSecret();
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] aesKeyBytes = Arrays.copyOf(sha256.digest(sharedSecret), 16);
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

            // 2. Prepare Security Layers (Hashing & Signing)
            String shaHash = computeHash(qp, "SHA-256");
            String md5Hash = computeHash(qp, "MD5");

            Signature rsaSign = Signature.getInstance("SHA256withRSA");
            rsaSign.initSign(rsaKeyPair.getPrivate());
            rsaSign.update(qp.getBytes("UTF-8"));
            byte[] digitalSignature = rsaSign.sign();

            // 3. Encrypt QP
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] encryptedQP = cipher.doFinal(qp.getBytes("UTF-8"));

            // 4. Send Everything
            dos.writeUTF("OK");
            dos.writeInt(encryptedQP.length);
            dos.write(encryptedQP);
            dos.writeUTF(shaHash);
            dos.writeUTF(md5Hash);
            dos.writeUTF(Base64.getEncoder().encodeToString(digitalSignature));
            dos.writeUTF(Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded()));
            dos.flush();

            appendLog("Sent Secure QP for: " + key + "\n");
        } catch (Exception ex) { appendLog("Client Error: " + ex.getMessage() + "\n"); }
    }

    private String computeHash(String data, String algo) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algo);
        return Base64.getEncoder().encodeToString(md.digest(data.getBytes("UTF-8")));
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg));
    }

    public static void main(String[] args) { new ExamServerGUI_AES_DH_RSA(); }
}
