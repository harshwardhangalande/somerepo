import java.awt.*;
import java.io.*;
import java.net.*;
import javax.crypto.*;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class ExamClientGUI_AES_dh extends JFrame {

    private JTextField classField, courseCodeField, semField;
    private JTextArea qpArea;
    private JButton requestBtn;
    private JButton decryptBtn;

    private byte[] receivedEncryptedQP;
    private SecretKey sessionAESKey;

    public ExamClientGUI_AES_dh() {

        setTitle("IS Lab Exam Client - DH + AES Secure QP");
        setSize(500, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(4, 2));

        panel.add(new JLabel("Class:"));
        classField = new JTextField("10");
        panel.add(classField);

        panel.add(new JLabel("Course Code:"));
        courseCodeField = new JTextField("CS101");
        panel.add(courseCodeField);

        panel.add(new JLabel("Semester:"));
        semField = new JTextField("1");
        panel.add(semField);

        requestBtn = new JButton("Request QP");
        requestBtn.addActionListener(e -> requestQP());
        panel.add(requestBtn);

        decryptBtn = new JButton("Decrypt QP");
        decryptBtn.setEnabled(false);
        decryptBtn.addActionListener(e -> decryptQP());
        panel.add(decryptBtn);

        add(panel, BorderLayout.NORTH);

        qpArea = new JTextArea();
        qpArea.setEditable(false);
        add(new JScrollPane(qpArea), BorderLayout.CENTER);

        setVisible(true);
    }

    private void requestQP() {

        new Thread(() -> {
            try (
                    Socket socket = new Socket("localhost", 8080);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true)) {


                        

                byte[] serverPubKeyEnc = (byte[]) in.readObject();

                KeyFactory kf = KeyFactory.getInstance("DH");
                X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(serverPubKeyEnc);
                PublicKey serverPubKey = kf.generatePublic(x509Spec);

                DHParameterSpec dhParamSpec = ((javax.crypto.interfaces.DHPublicKey) serverPubKey).getParams();

                KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
                kpg.initialize(dhParamSpec);
                KeyPair clientKP = kpg.generateKeyPair();

                out.writeObject(clientKP.getPublic().getEncoded());
                out.flush();

                KeyAgreement ka = KeyAgreement.getInstance("DH");
                ka.init(clientKP.getPrivate());
                ka.doPhase(serverPubKey, true);
                byte[] sharedSecret = ka.generateSecret();

                String sharedSecretBase64 = Base64.getEncoder().encodeToString(sharedSecret);

                sessionAESKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");



                pw.println(classField.getText());
                pw.println(courseCodeField.getText());
                pw.println("NA");
                pw.println(semField.getText());

                String status = (String) in.readObject();

                if (!"OK".equals(status)) {
                    qpArea.setText((String) in.readObject());
                    return;
                }

                receivedEncryptedQP = (byte[]) in.readObject();

                String encryptedBase64 = Base64.getEncoder().encodeToString(receivedEncryptedQP);

                qpArea.setText(
                        "SHARED SECRET (Base64):\n" + sharedSecretBase64 +
                                "\n\nENCRYPTED QP (Base64 Format):\n\n" + encryptedBase64);

                decryptBtn.setEnabled(true);

            } catch (Exception e) {
                qpArea.setText("Error: " + e.getMessage());
            }
        }).start();
    }

    private void decryptQP() {
        try {

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, sessionAESKey);
            byte[] decrypted = cipher.doFinal(receivedEncryptedQP);

            qpArea.setText("DECRYPTED QP:\n\n" + new String(decrypted));

            decryptBtn.setEnabled(false);

        } catch (Exception e) {
            qpArea.setText("Decryption Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExamClientGUI_AES_dh::new);
    }
}