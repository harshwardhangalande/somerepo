import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

public class ExamServerGUI_AES extends JFrame {

    private ServerSocket serverSocket;
    private Map<String, String> qpDatabase;
    private JTextArea logArea;

    public ExamServerGUI_AES() {

        qpDatabase = new HashMap<>();
        qpDatabase.put(
            "10-CS101-1",
            "Question 1: What is Java?\nQuestion 2: Explain OOP concepts."
        );
        qpDatabase.put(
            "10-CS301-3",
            "Question 1: Socket Programming.\nQuestion 2: Explain AES Algorithm."
        );

        setTitle("IS Lab Exam Server - AES Secure QP Delivery");
        setSize(650, 420);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JButton startBtn = new JButton("Start Server (Port 8080)");
        startBtn.addActionListener(e -> startServer());
        add(startBtn, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(8080);
                appendLog("Server started on port 8080...\n");

                while (true) {
                    Socket client = serverSocket.accept();
                    appendLog("Client connected: " + client.getInetAddress());
                    handleClient(client);
                }
            } catch (Exception ex) {
                appendLog("Server Error: " + ex.getMessage());
            }
        }).start();
    }

    private void handleClient(Socket client) {
        new Thread(() -> {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                DataOutputStream dos = new DataOutputStream(client.getOutputStream())
            ) {
                String cls = in.readLine();
                String courseCode = in.readLine();
                in.readLine(); // course name (ignored)
                String sem = in.readLine();

                String dbKey = cls + "-" + courseCode + "-" + sem;
                appendLog("Request: " + dbKey);

                String qpContent = qpDatabase.get(dbKey);
                if (qpContent == null) {
                    out.println("ERROR");
                    out.println("QP not found");
                    return;
                }

                // ===== AES KEY GENERATION =====
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(128);
                SecretKey secretKey = keyGen.generateKey();

                // ===== AES ENCRYPT =====
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                byte[] encryptedBytes = cipher.doFinal(qpContent.getBytes("UTF-8"));

                appendLog("QP encrypted using AES");

                // ===== SEND DATA =====
                out.println("OK");
                out.println(encryptedBytes.length);
                out.println(Base64.getEncoder().encodeToString(secretKey.getEncoded()));

                dos.write(encryptedBytes);
                dos.flush();

                appendLog("Encrypted QP sent\n");

            } catch (Exception ex) {
                appendLog("Client Error: " + ex.getMessage());
            }
        }).start();
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExamServerGUI_AES::new);
    }
}

