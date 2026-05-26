import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

public class ExamClientGUI_AES_DH_RSA extends JFrame {
    private JTextField classField, courseCodeField, courseNameField, semField;
    private JTextArea qpArea;

    public ExamClientGUI_AES_DH_RSA() {
        setTitle("Exam Client (AES + DH + RSA)");
        setSize(550, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(5, 2));
        panel.add(new JLabel(" Class:")); classField = new JTextField("10"); panel.add(classField);
        panel.add(new JLabel(" Course Code:")); courseCodeField = new JTextField("CS101"); panel.add(courseCodeField);
        panel.add(new JLabel(" Course Name:")); courseNameField = new JTextField("Intro Java"); panel.add(courseNameField);
        panel.add(new JLabel(" Semester:")); semField = new JTextField("1"); panel.add(semField);
        JButton btn = new JButton("Request Secure QP");
        panel.add(btn);
        add(panel, BorderLayout.NORTH);

        qpArea = new JTextArea();
        qpArea.setEditable(false);
        add(new JScrollPane(qpArea), BorderLayout.CENTER);

        btn.addActionListener(e -> requestQP());
        setVisible(true);
    }

    private void requestQP() {
        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 8080);
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                // Send Metadata
                dos.writeUTF(classField.getText());
                dos.writeUTF(courseCodeField.getText());
                dos.writeUTF(courseNameField.getText());
                dos.writeUTF(semField.getText());
                dos.flush();

                // 1. DH Key Exchange
                int serverKeyLen = dis.readInt();
                byte[] serverPubBytes = new byte[serverKeyLen];
                dis.readFully(serverPubBytes);

                KeyPairGenerator kpg = KeyPairGenerator.getInstance("DiffieHellman");
                kpg.initialize(2048);
                KeyPair clientPair = kpg.generateKeyPair();
                KeyAgreement ka = KeyAgreement.getInstance("DiffieHellman");
                ka.init(clientPair.getPrivate());

                // Send Client DH Public Key
                byte[] clientPubBytes = clientPair.getPublic().getEncoded();
                dos.writeInt(clientPubBytes.length);
                dos.write(clientPubBytes);
                dos.flush();

                // Generate Secret
                KeyFactory kf = KeyFactory.getInstance("DiffieHellman");
                PublicKey serverPub = kf.generatePublic(new X509EncodedKeySpec(serverPubBytes));
                ka.doPhase(serverPub, true);
                byte[] secret = ka.generateSecret();
                byte[] aesBytes = Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(secret), 16);
                SecretKey aesKey = new SecretKeySpec(aesBytes, "AES");

                // 2. Receive Encrypted Data
                String status = dis.readUTF();
                if (!status.equals("OK")) {
                    updateUI("Server Error: " + status);
                    return;
                }

                int encLen = dis.readInt();
                byte[] encData = new byte[encLen];
                dis.readFully(encData);

                String serverSha = dis.readUTF();
                String serverMd5 = dis.readUTF();
                String signatureStr = dis.readUTF();
                String rsaPubStr = dis.readUTF();

                // 3. Decrypt
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, aesKey);
                String qp = new String(cipher.doFinal(encData), "UTF-8");

                // 4. Verify Integrity and Signature
                boolean integrity = computeHash(qp, "SHA-256").equals(serverSha) && 
                                   computeHash(qp, "MD5").equals(serverMd5);

                KeyFactory rsaKF = KeyFactory.getInstance("RSA");
                PublicKey rsaPub = rsaKF.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(rsaPubStr)));
                Signature verify = Signature.getInstance("SHA256withRSA");
                verify.initVerify(rsaPub);
                verify.update(qp.getBytes("UTF-8"));
                boolean sigOK = verify.verify(Base64.getDecoder().decode(signatureStr));

                if (integrity && sigOK) {
                    updateUI("DECRYPTED QUESTION PAPER:\n\n" + qp + "\n\n[Verified: Integrity & RSA Signature OK]");
                } else {
                    updateUI("SECURITY ALERT: Verification Failed!\nIntegrity: " + integrity + "\nSignature: " + sigOK);
                }

            } catch (Exception ex) { updateUI("Error: " + ex.getMessage()); }
        }).start();
    }

    private String computeHash(String data, String algo) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algo);
        return Base64.getEncoder().encodeToString(md.digest(data.getBytes("UTF-8")));
    }

    private void updateUI(String text) {
        SwingUtilities.invokeLater(() -> qpArea.setText(text));
    }

    public static void main(String[] args) { new ExamClientGUI_AES_DH_RSA(); }
}
