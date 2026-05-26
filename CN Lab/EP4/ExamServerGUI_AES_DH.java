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

public class ExamServerGUI_AES_DH extends JFrame {

    private ServerSocket serverSocket;
    private Map<String, String> qpDatabase;
    private JTextArea logArea;

    public ExamServerGUI_AES_DH() {

        qpDatabase = new HashMap<>();
        try {
            String qpContent = new String(Files.readAllBytes(Paths.get("QP_CS101.txt")));
            qpDatabase.put("10-CS101-1", qpContent);
        } catch (Exception e) {
            qpDatabase.put("10-CS101-1",
                    "Question 1: What is Java?\nQuestion 2: Explain OOP concepts.");
        }

        setTitle("Server - AES + DH + SHA + MD5 Integrity");
        setSize(650, 420);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
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
                appendLog("Server started on port 8080...\n");

                while (true) {
                    Socket client = serverSocket.accept();
                    appendLog("Client Connected...\n");
                    handleClient(client);
                }

            } catch (Exception ex) {
                appendLog("Server Error: " + ex.getMessage() + "\n");
            }
        }).start();
    }

    private void handleClient(Socket client) {

        new Thread(() -> {
            try {

                BufferedReader in =
                        new BufferedReader(new InputStreamReader(client.getInputStream()));

                PrintWriter out =
                        new PrintWriter(client.getOutputStream(), true);

                DataOutputStream dos =
                        new DataOutputStream(client.getOutputStream());

                String cls = in.readLine();
                String courseCode = in.readLine();
                in.readLine();
                String sem = in.readLine();

                String dbKey = cls + "-" + courseCode + "-" + sem;

                appendLog("Request Key: " + dbKey + "\n");

                String qpContent = qpDatabase.get(dbKey);

                if (qpContent == null) {
                    out.println("ERROR");
                    return;
                }

                /* ---------- DH Key Exchange ---------- */

                KeyPairGenerator kpg = KeyPairGenerator.getInstance("DiffieHellman");
                kpg.initialize(2048);

                KeyPair serverPair = kpg.generateKeyPair();

                KeyAgreement serverAgree =
                        KeyAgreement.getInstance("DiffieHellman");

                serverAgree.init(serverPair.getPrivate());

                // Send server public key
                out.println(Base64.getEncoder()
                        .encodeToString(serverPair.getPublic().getEncoded()));

                // Receive client public key
                byte[] clientPubBytes =
                        Base64.getDecoder().decode(in.readLine());

                KeyFactory kf =
                        KeyFactory.getInstance("DiffieHellman");

                PublicKey clientPub =
                        kf.generatePublic(new X509EncodedKeySpec(clientPubBytes));

                serverAgree.doPhase(clientPub, true);

                byte[] sharedSecret = serverAgree.generateSecret();

                /* ---------- AES Key ---------- */

                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                byte[] aesKeyBytes =
                        Arrays.copyOf(sha256.digest(sharedSecret), 16);

                SecretKey aesKey =
                        new SecretKeySpec(aesKeyBytes, "AES");

                /* ---------- Hash Integrity ---------- */

                String shaHash = computeHash(qpContent, "SHA-256");
                String md5Hash = computeHash(qpContent, "MD5");

                /* ---------- Encrypt QP ---------- */

                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, aesKey);

                byte[] encryptedQP =
                        cipher.doFinal(qpContent.getBytes("UTF-8"));

                /* ---------- Send Data ---------- */

                out.println("OK");
                out.println(encryptedQP.length);

                dos.write(encryptedQP);
                dos.flush();

                // Send hashes
                out.println(shaHash);
                out.println(md5Hash);

                appendLog("Encrypted QP + Hash Sent\n");

                client.close();

            } catch (Exception ex) {
                appendLog("Client Error: " + ex.getMessage() + "\n");
            }
        }).start();
    }

    private String computeHash(String data, String algo) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algo);
        byte[] digest = md.digest(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(digest);
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExamServerGUI_AES_DH::new);
    }
}