import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import javax.swing.*;

public class Client1 extends JFrame {

    private JTextField classField, courseCodeField, courseNameField, semField, keyField;
    private JTextArea qpArea;
    private JButton requestBtn, decryptBtn;

    private String encryptedQP;
    private int receivedKey;

    // FIX 1: Constructor name MUST match the class name "Client1"
    public Client1() {
        setTitle("IS Lab Exam Client - Secure QP Download");
        setSize(550, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ================= INPUT PANEL =================
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 5, 5));

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

        inputPanel.add(new JLabel("Transposition Key (columns):"));
        keyField = new JTextField("4");
        inputPanel.add(keyField);

        requestBtn = new JButton("Request Question Paper");
        decryptBtn = new JButton("Decrypt & Download");
        decryptBtn.setEnabled(false);

        inputPanel.add(requestBtn);
        inputPanel.add(decryptBtn);

        add(inputPanel, BorderLayout.NORTH);

        // ================= TEXT AREA =================
        qpArea = new JTextArea();
        qpArea.setEditable(false);
        qpArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        add(new JScrollPane(qpArea), BorderLayout.CENTER);

        // ================= BUTTON ACTIONS =================
        requestBtn.addActionListener(e -> requestQP());
        decryptBtn.addActionListener(e -> decryptAndDownload());

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
                // Send metadata to server
                out.println(classField.getText());
                out.println(courseCodeField.getText());
                out.println(courseNameField.getText());
                out.println(semField.getText());

                String status = in.readLine();
                if ("ERROR".equals(status)) {
                    String msg = in.readLine();
                    SwingUtilities.invokeLater(() -> qpArea.setText("Error: " + msg));
                    return;
                }

                int length = Integer.parseInt(in.readLine());
                receivedKey = Integer.parseInt(in.readLine());

                byte[] buffer = new byte[length];
                dis.readFully(buffer);
                encryptedQP = new String(buffer, "UTF-8");

                SwingUtilities.invokeLater(() -> {
                    qpArea.setText("ENCRYPTED QP RECEIVED:\n\n" + encryptedQP);
                    keyField.setText(String.valueOf(receivedKey));
                    decryptBtn.setEnabled(true);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> qpArea.setText("Connection Error: " + ex.getMessage()));
            }
        }).start();
    }

    private void decryptAndDownload() {
        try {
            int key = Integer.parseInt(keyField.getText());
            String decrypted = transpositionDecrypt(encryptedQP, key);

            // Clean up the formatting for display
            String displayTxt = decrypted.replace("_", " ");
            qpArea.setText("DECRYPTED QUESTION PAPER:\n\n" + displayTxt);

            String filename = "QP_" + courseCodeField.getText() + ".txt";
            Files.write(Paths.get(filename), displayTxt.getBytes());

            JOptionPane.showMessageDialog(this, "File Saved as: " + filename);

        } catch (Exception ex) {
            qpArea.setText("Decryption Error: " + ex.getMessage());
        }
    }

    private String transpositionDecrypt(String cipher, int key) {
        int rows = (int) Math.ceil((double) cipher.length() / key);
        char[][] matrix = new char[rows][key];
        int index = 0;

        // Fill Column-wise (Reverse of Encryption)
        for (int c = 0; c < key; c++) {
            for (int r = 0; r < rows; r++) {
                if (index < cipher.length()) {
                    matrix[r][c] = cipher.charAt(index++);
                }
            }
        }

        // Read Row-wise to reconstruct the text
        StringBuilder plain = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < key; c++) {
                plain.append(matrix[r][c]);
            }
        }
        // Remove trailing 'X' padding
        return plain.toString().replaceAll("X+$", "");
    }

    public static void main(String[] args) {
        // FIX 2: Must reference the correct class name "Client1"
        SwingUtilities.invokeLater(Client1::new);
    }
}
