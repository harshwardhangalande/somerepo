import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

public class ExamClientGUI_AES extends JFrame {

    private JTextField classField, courseCodeField, courseNameField, semField;
    private JTextArea qpArea;
    private JButton requestBtn;

    public ExamClientGUI_AES() {
        setTitle("IS Lab Exam Client - AES Secure QP Download");
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
        courseNameField = new JTextField("Intro to Programming");
        panel.add(courseNameField);

        panel.add(new JLabel("Semester:"));
        semField = new JTextField("1");
        panel.add(semField);

        requestBtn = new JButton("Request & Decrypt QP");
        panel.add(requestBtn);

        add(panel, BorderLayout.NORTH);

        qpArea = new JTextArea();
        qpArea.setEditable(false);
        add(new JScrollPane(qpArea), BorderLayout.CENTER);

        requestBtn.addActionListener(e -> requestQP());

        setVisible(true);
    }

    private void requestQP() {
        new Thread(() -> {
            try (
                Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataInputStream dis = new DataInputStream(socket.getInputStream())
            ) {
                out.println(classField.getText());
                out.println(courseCodeField.getText());
                out.println(courseNameField.getText());
                out.println(semField.getText());

                if (!"OK".equals(in.readLine())) {
                    qpArea.setText("Error receiving QP");
                    return;
                }

                int len = Integer.parseInt(in.readLine());
                byte[] keyBytes = Base64.getDecoder().decode(in.readLine());

                byte[] encryptedData = new byte[len];
                dis.readFully(encryptedData);

                // ===== AES DECRYPT =====
                SecretKey key = new SecretKeySpec(keyBytes, "AES");
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, key);

                String decryptedQP = new String(cipher.doFinal(encryptedData), "UTF-8");

                qpArea.setText("DECRYPTED QUESTION PAPER:\n\n" + decryptedQP);

                Files.write(
                    Paths.get("QP_" + courseCodeField.getText() + ".txt"),
                    decryptedQP.getBytes()
                );

            } catch (Exception ex) {
                qpArea.setText("Error: " + ex.getMessage());
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExamClientGUI_AES::new);
    }
}

