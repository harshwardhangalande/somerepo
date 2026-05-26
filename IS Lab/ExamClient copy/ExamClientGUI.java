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

        inputPanel.add(new JLabel("Decrypt Key (Vigenère):"));
        keyField = new JTextField();
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
    private String receivedKey;

    private void requestQP() {
        new Thread(() -> {
            try (
                    Socket socket = new Socket("localhost", 8080);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                // Send inputs
                out.println(classField.getText());
                out.println(courseCodeField.getText());
                out.println(courseNameField.getText());
                out.println(semField.getText());

                // Read server status
                String status = dis.readUTF();
                if (!"OK".equals(status)) {
                    String errMsg = dis.readUTF();
                    qpArea.setText("Error from server: " + errMsg);
                    return;
                }

                int len = dis.readInt();
                receivedKey = dis.readUTF();

                // Receive encrypted bytes
                byte[] buffer = new byte[len];
                dis.readFully(buffer);
                encryptedQP = new String(buffer, "UTF-8");

                SwingUtilities.invokeLater(() -> {
                    qpArea.setText(
                            "Encrypted QP received (length: " + len + ")\n" +
                                    "Key (Vigenère): " + receivedKey + "\n\n" +
                                    encryptedQP);
                    decryptBtn.setEnabled(true);
                    keyField.setText(receivedKey);
                });

            } catch (Exception ex) {
                qpArea.setText("Connection error: " + ex.getMessage());
            }
        }).start();
    }

    private void decryptAndDownload() {
        try {
            String key = keyField.getText();
            String decrypted = vigenereDecrypt(encryptedQP, key);
            qpArea.setText("DECRYPTED QP:\n\n" + decrypted);

            // Download to file
            String filename = "QP_" + courseCodeField.getText() + "_" + semField.getText() + ".txt";
            Files.write(Paths.get(filename), decrypted.getBytes());
            JOptionPane.showMessageDialog(this, "Downloaded: " + filename);
        } catch (Exception ex) {
            qpArea.setText("Decrypt error: " + ex.getMessage());
        }
    }

    // Vigenère Cipher Decryption (Polyalphabetic)
    static String vigenereDecrypt(String text, String key) {
        StringBuilder result = new StringBuilder();
        key = key.toUpperCase();
        int keyIndex = 0;

        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                int shift = key.charAt(keyIndex % key.length()) - 'A';
                result.append((char) ((c - base - shift + 26) % 26 + base));
                keyIndex++;
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