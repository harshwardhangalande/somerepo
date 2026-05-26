import javax.swing.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Arrays;

public class ExamClientGUI_AES_dh extends JFrame {

    JTextField classField, courseField, semField;
    JTextArea outputArea;

    public ExamClientGUI_AES_dh() {

        setTitle("Exam Client - Secure QP Fetch");
        setSize(500, 400);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(3, 2));

        inputPanel.add(new JLabel("Class"));
        classField = new JTextField("10");
        inputPanel.add(classField);

        inputPanel.add(new JLabel("Course Code"));
        courseField = new JTextField("CS101");
        inputPanel.add(courseField);

        inputPanel.add(new JLabel("Semester"));
        semField = new JTextField("1");
        inputPanel.add(semField);

        add(inputPanel, BorderLayout.NORTH);

        JButton fetchBtn = new JButton("Fetch Question Paper");
        add(fetchBtn, BorderLayout.CENTER);

        outputArea = new JTextArea();
        add(new JScrollPane(outputArea), BorderLayout.SOUTH);

        fetchBtn.addActionListener(e -> fetchQP());

        setVisible(true);
    }

    private void fetchQP() {

        try {

            Socket socket = new Socket("localhost", 8080);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);

            byte[] serverPubKeyEnc = (byte[]) in.readObject();

            KeyFactory kf = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(serverPubKeyEnc);
            PublicKey serverPubKey = kf.generatePublic(x509Spec);

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
            kpg.initialize(512);
            KeyPair clientKP = kpg.generateKeyPair();

            out.writeObject(clientKP.getPublic().getEncoded());
            out.flush();

            KeyAgreement ka = KeyAgreement.getInstance("DH");
            ka.init(clientKP.getPrivate());
            ka.doPhase(serverPubKey, true);
            byte[] sharedSecret = ka.generateSecret();

            SecretKey aesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");

            pw.println(classField.getText());
            pw.println(courseField.getText());
            pw.println("dummy");
            pw.println(semField.getText());

            String status = (String) in.readObject();

            if (status.equals("ERROR")) {
                outputArea.setText((String) in.readObject());
                return;
            }

            byte[] encryptedQP = (byte[]) in.readObject();
            byte[] receivedHash = (byte[]) in.readObject();

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] computedHash = md.digest(encryptedQP);

            if (!Arrays.equals(receivedHash, computedHash)) {
                outputArea.setText("Integrity verification failed!");
                return;
            }

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);

            byte[] decryptedQP = cipher.doFinal(encryptedQP);

            outputArea.setText(new String(decryptedQP));

            socket.close();

        } catch (Exception ex) {
            outputArea.setText("Error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        new ExamClientGUI_AES_dh();
    }
}