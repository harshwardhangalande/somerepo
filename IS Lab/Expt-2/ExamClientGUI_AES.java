import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.util.Base64;

public class ExamClientGUI_AES extends JFrame {

    private JTextField classField, courseCodeField, semField, keyField;
    private JTextArea qpArea;
    private JButton requestBtn, decryptBtn;

    private byte[] encryptedQP;

    public ExamClientGUI_AES() {
        setTitle("IS Lab Exam Client - AES Secure QP Download");
        setSize(500, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(5, 2));

        panel.add(new JLabel("Class:"));
        classField = new JTextField("10");
        panel.add(classField);

        panel.add(new JLabel("Course Code:"));
        courseCodeField = new JTextField("CS101");
        panel.add(courseCodeField);

        panel.add(new JLabel("Semester:"));
        semField = new JTextField("1");
        panel.add(semField);

        panel.add(new JLabel("AES Key (Base64):"));
        keyField = new JTextField();
        panel.add(keyField);

        requestBtn = new JButton("Request QP");
        decryptBtn = new JButton("Decrypt & Download");
        decryptBtn.setEnabled(false);

        requestBtn.addActionListener(e -> requestQP());
        decryptBtn.addActionListener(e -> decryptQP());

        panel.add(requestBtn);
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
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                out.println(classField.getText());
                out.println(courseCodeField.getText());
                out.println("NA");
                out.println(semField.getText());

                String status = dis.readUTF();
                if (!"OK".equals(status)) {
                    qpArea.setText(dis.readUTF());
                    return;
                }

                int len = dis.readInt();
                String keyBase64 = dis.readUTF();

                encryptedQP = new byte[len];
                dis.readFully(encryptedQP);

                SwingUtilities.invokeLater(() -> {
                    keyField.setText(keyBase64);
                    qpArea.setText("Encrypted QP received (" + len + " bytes)");
                    decryptBtn.setEnabled(true);
                });

            } catch (Exception e) {
                qpArea.setText("Error: " + e.getMessage());
            }
        }).start();
    }

    private void decryptQP() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyField.getText());
            SecretKey key = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decrypted = cipher.doFinal(encryptedQP);
            String qp = new String(decrypted, "UTF-8");

            qpArea.setText("DECRYPTED QP:\n\n" + qp);

            String fileName = "QP_" + courseCodeField.getText() + ".txt";
            Files.write(Paths.get(fileName), qp.getBytes());
            JOptionPane.showMessageDialog(this, "Downloaded: " + fileName);

        } catch (Exception e) {
            qpArea.setText("Decryption failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExamClientGUI_AES::new);
    }
}