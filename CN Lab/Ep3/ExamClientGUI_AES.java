import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

public class ExamClientGUI_AES extends JFrame {
    private JTextField classField, courseField, semField;
    private JTextArea logArea;

    public ExamClientGUI_AES() {
        setTitle("Secure Client (DH Handshake)");
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel p = new JPanel(new GridLayout(4, 2));
        p.add(new JLabel("Class:")); classField = new JTextField("10"); p.add(classField);
        p.add(new JLabel("Course:")); courseField = new JTextField("CS101"); p.add(courseField);
        p.add(new JLabel("Sem:")); semField = new JTextField("1"); p.add(semField);
        JButton btn = new JButton("Get Secure QP");
        btn.addActionListener(e -> fetch());
        p.add(btn);
        add(p, BorderLayout.NORTH);
        logArea = new JTextArea();
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        setVisible(true);
    }

    private void fetch() {
        new Thread(() -> {
            try (Socket s = new Socket("localhost", 8080);
                 ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

                // 1. Generate Client DH Keys
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
                kpg.initialize(512);
                KeyPair clientKP = kpg.generateKeyPair();
                
                appendLog("\n--- DIFFIE-HELLMAN HANDSHAKE ---");
                appendLog("CLIENT PRIVATE KEY: " + encode(clientKP.getPrivate().getEncoded()));
                appendLog("CLIENT PUBLIC KEY:  " + encode(clientKP.getPublic().getEncoded()));

                // 2. Exchange
                out.writeObject(clientKP.getPublic().getEncoded());
                out.flush();
                
                byte[] serverPubKeyEnc = (byte[]) in.readObject();
                appendLog("RECEIVED SERVER PUB: " + encode(serverPubKeyEnc));

                // 3. Compute Secret
                KeyFactory kf = KeyFactory.getInstance("DH");
                PublicKey serverPubKey = kf.generatePublic(new X509EncodedKeySpec(serverPubKeyEnc));
                KeyAgreement ka = KeyAgreement.getInstance("DH");
                ka.init(clientKP.getPrivate());
                ka.doPhase(serverPubKey, true);
                byte[] sharedSecret = ka.generateSecret();
                
                appendLog("CLIENT SHARED SECRET: " + encode(sharedSecret));

                // 4. Request
                out.writeUTF(classField.getText() + "-" + courseField.getText() + "-" + semField.getText());
                out.flush();

                byte[] encrypted = (byte[]) in.readObject();
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sharedSecret, 0, 16, "AES"));
                
                appendLog("\nDECRYPTED CONTENT:\n" + new String(cipher.doFinal(encrypted), "UTF-8"));

            } catch (Exception e) { appendLog("Error: " + e.getMessage()); }
        }).start();
    }

    private String encode(byte[] data) { return Base64.getEncoder().encodeToString(data).substring(0, 40) + "..."; }
    private void appendLog(String m) { SwingUtilities.invokeLater(() -> logArea.append(m + "\n")); }
    public static void main(String[] args) { SwingUtilities.invokeLater(ExamClientGUI_AES::new); }
}
