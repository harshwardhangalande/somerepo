package ExamClient;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import javax.swing.*;

public class ExamClientGUI extends JFrame {
    private JTextField classField, courseCodeField, courseNameField, semField, keyField;
    private JTextArea qpArea;
    private JButton requestBtn, decryptBtn;

    public ExamClientGUI() {
        setTitle("IS Lab Exam Client - Secure QP Download");
        setSize(500, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Input panel
        JPanel inputPanel = new JPanel(new GridLayout(5, 2));
        inputPanel.add(new JLabel("Class:"));
        classField = new JTextField("10");
        inputPanel.add(classField);
        inputPanel.add(new JLabel("Course Code:"));
        courseCodeField = new JTextField("CS101");
        inputPanel.add(courseCodeField);
        inputPanel.add(new JLabel("Course Name:"));
        courseNameField = new JTextField("Intro to Programming");
        inputPanel.add(courseNameField);
        inputPanel.add(new JLabel("Semester:"));
        semField = new JTextField("1");
        inputPanel.add(semField);
        inputPanel.add(new JLabel("Decrypt Key (shift):"));
        keyField = new JTextField("3");
        inputPanel.add(keyField);

        requestBtn = new JButton("Request Question Paper");
        requestBtn.addActionListener(e -> requestQP());
        inputPanel.add(requestBtn);

        decryptBtn = new JButton("Decrypt & Download");
        decryptBtn.setEnabled(false);
        decryptBtn.addActionListener(e -> decryptAndDownload());
        inputPanel.add(decryptBtn);

        add(inputPanel, BorderLayout.NORTH);

        // QP display
        qpArea = new JTextArea();
        qpArea.setEditable(false);
        add(new JScrollPane(qpArea), BorderLayout.CENTER);

        setVisible(true);
    }

    private String encryptedQP;
    private int receivedShift;

    private void requestQP() {
        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 8080);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                // Send     inputs
                out.println(classField.getText());
                out.println(courseCodeField.getText());
                out.println(courseNameField.getText());
                out.println(semField.getText());

                String status = in.readLine();
                if ("ERROR".equals(status)) {
                    qpArea.setText("Error: " + in.readLine());
                    return;
                }

                int len = Integer.parseInt(in.readLine());
                receivedShift = Integer.parseInt(in.readLine());

                // Receive encrypted bytes
                byte[] buffer = new byte[len];
                dis.readFully(buffer);
                encryptedQP = new String(buffer, "UTF-8");

                SwingUtilities.invokeLater(() -> {
                    qpArea.setText("Encrypted QP received (length: " + len + ")\nKey (shift): " + receivedShift + "\n\n" + encryptedQP);
                    decryptBtn.setEnabled(true);
                    keyField.setText(String.valueOf(receivedShift));
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                qpArea.setText("Connection error: " + ex.getMessage());
            }
        }).start();
    }

    private void decryptAndDownload() {
        try {
            int shift = Integer.parseInt(keyField.getText());
            String decrypted = caesarDecrypt(encryptedQP, shift);
            qpArea.setText("DECRYPTED QP:\n\n" + decrypted);

            // Download to file
            String filename = "QP_" + courseCodeField.getText() + "_" + semField.getText() + ".txt";
            Files.write(Paths.get(filename), decrypted.getBytes());
            JOptionPane.showMessageDialog(this, "Downloaded: " + filename);
        } catch (Exception ex) {
            qpArea.setText("Decrypt error: " + ex.getMessage());
        }
    }

    // STUDENT CUSTOMIZE: Caesar Cipher Decryption (negative shift for decrypt)
    private String caesarDecrypt(String text, int shift) {
        return caesarEncrypt(text, 26 - (shift % 26)); // Equivalent to negative shift
    }

    // Reuse encrypt logic for decrypt (modular)
    private String caesarEncrypt(String text, int shift) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                result.append((char) ((c - base + shift) % 26 + base));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExamClientGUI::new);
    }
}
