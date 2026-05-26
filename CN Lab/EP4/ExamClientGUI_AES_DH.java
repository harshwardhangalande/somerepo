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

public class ExamClientGUI_AES_DH extends JFrame {

    private JTextField classField, courseCodeField, courseNameField, semField;
    private JTextArea qpArea;
    private JButton requestBtn;

    public ExamClientGUI_AES_DH() {

        setTitle("Client - AES + DH + SHA + MD5 Integrity Check");
        setSize(550, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(5, 2));

        panel.add(new JLabel("Class:"));
        classField = new JTextField("10");
        panel.add(classField);

        panel.add(new JLabel("Course Code:"));
        courseCodeField = new JTextField("CS101");
        panel.add(courseCodeField);

        panel.add(new JLabel("Course Name:"));
        courseNameField = new JTextField("Intro Programming");
        panel.add(courseNameField);

        panel.add(new JLabel("Semester:"));
        semField = new JTextField("1");
        panel.add(semField);

        requestBtn = new JButton("Request Secure QP");
        panel.add(requestBtn);

        add(panel, BorderLayout.NORTH);

        qpArea = new JTextArea();
        add(new JScrollPane(qpArea), BorderLayout.CENTER);

        requestBtn.addActionListener(e -> requestQP());

        setVisible(true);
    }

    private void requestQP() {

        new Thread(() -> {

            try {

                Socket socket = new Socket("localhost", 8080);

                PrintWriter out =
                        new PrintWriter(socket.getOutputStream(), true);

                BufferedReader in =
                        new BufferedReader(new InputStreamReader(socket.getInputStream()));

                DataInputStream dis =
                        new DataInputStream(socket.getInputStream());

                /* ---------- Send Request ---------- */

                out.println(classField.getText());
                out.println(courseCodeField.getText());
                out.println(courseNameField.getText());
                out.println(semField.getText());

                /* ---------- DH Exchange ---------- */

                byte[] serverPubBytes =
                        Base64.getDecoder().decode(in.readLine());

                KeyFactory kf = KeyFactory.getInstance("DiffieHellman");

                PublicKey serverPub =
                        kf.generatePublic(new X509EncodedKeySpec(serverPubBytes));

                KeyPairGenerator kpg =
                        KeyPairGenerator.getInstance("DiffieHellman");

                kpg.initialize(2048);

                KeyPair clientPair = kpg.generateKeyPair();

                KeyAgreement clientAgree =
                        KeyAgreement.getInstance("DiffieHellman");

                clientAgree.init(clientPair.getPrivate());

                out.println(Base64.getEncoder()
                        .encodeToString(clientPair.getPublic().getEncoded()));

                clientAgree.doPhase(serverPub, true);

                byte[] sharedSecret = clientAgree.generateSecret();

                /* ---------- AES Key ---------- */

                MessageDigest sha256 =
                        MessageDigest.getInstance("SHA-256");

                byte[] aesKeyBytes =
                        Arrays.copyOf(sha256.digest(sharedSecret), 16);

                SecretKey aesKey =
                        new SecretKeySpec(aesKeyBytes, "AES");

                /* ---------- Receive Data ---------- */

                if (!"OK".equals(in.readLine())) {
                    qpArea.setText("Error receiving QP");
                    return;
                }

                int len = Integer.parseInt(in.readLine());

                byte[] encryptedData = new byte[len];
                dis.readFully(encryptedData);

                String shaHashServer = in.readLine();
                String md5HashServer = in.readLine();

                /* ---------- AES Decrypt ---------- */

                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, aesKey);

                String decryptedQP =
                        new String(cipher.doFinal(encryptedData), "UTF-8");

                /* ---------- Integrity Check ---------- */

                String shaClient =
                        computeHash(decryptedQP, "SHA-256");

                String md5Client =
                        computeHash(decryptedQP, "MD5");

                boolean integrityOK =
                        shaHashServer.equals(shaClient) &&
                        md5HashServer.equals(md5Client);

                if (integrityOK)
                    qpArea.setText("DECRYPTED QUESTION PAPER:\n"
                            + decryptedQP + "\n\nIntegrity Check PASSED");
                else
                    qpArea.setText("Integrity Check FAILED");

                Files.write(Paths.get(
                        "QP_" + courseCodeField.getText() + ".txt"),
                        decryptedQP.getBytes());

                socket.close();

            } catch (Exception ex) {
                qpArea.setText("Error: " + ex.getMessage());
            }

        }).start();
    }

    private String computeHash(String data, String algo) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algo);
        byte[] digest = md.digest(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(digest);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExamClientGUI_AES_DH::new);
    }
}