package ExamClient;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class ExamServerGUI extends JFrame {
    private ServerSocket serverSocket;
    private Map<String, String> qpDatabase; // Simulated DB: key = "class-course-semester", value = QP content
    private JTextArea logArea;

    public ExamServerGUI() {
        // STUDENT CUSTOMIZE: Initialize simulated database with sample QPs
        // qpDatabase = new HashMap<>();
        // qpDatabase.put("10-CS101-1", "Question 1: What is Java? (5 marks)\nQuestion 2: Explain OOP. (10 marks)");
        // qpDatabase.put("10-CS301-3", "Question 1: Socket Programming. (8 marks)\nQuestion 2: Caesar Cipher impl. (12 marks)");
        // ADD MORE QPs HERE for other class/course/semester combos

        setTitle("IS Lab Exam Server - Caesar Cipher Secure QP Delivery");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Log area for server events
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
                appendLog("Server started on port 8080. Waiting for clients...");
                while (true) {
                    Socket client = serverSocket.accept();
                    appendLog("Client connected: " + client.getInetAddress());
                    handleClient(client);
                }
            } catch (Exception ex) {
                appendLog("Server error: " + ex.getMessage());
            }
        }).start();
    }

    private void handleClient(Socket client) {
        new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                 PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                 DataOutputStream dos = new DataOutputStream(client.getOutputStream())) {

                // Receive client inputs
                String cls = in.readLine();
                String courseCode = in.readLine();
                String courseName = in.readLine();
                String sem = in.readLine();

                String dbKey = cls + "-" + courseCode + "-" + sem;
                appendLog("Client request: " + dbKey);

                // Retrieve from DB
                // String qpContent = qpDatabase.getOrDefault(dbKey, "ERROR: QP not found");
                // if (qpContent.startsWith("ERROR")) {
                //     out.println("ERROR");
                //     out.println(qpContent);
                //     return;
                // }

                MongoDatabase db = MongoClients
                                    .create("mongodb+srv://dbUser:dbUserPassword@cluster0.igauqwz.mongodb.net/?appName=Cluster0")
                                    .getDatabase("ExamDB");
                MongoCollection<Document> collection = db.getCollection("questionpapers");
                // appendLog("DEBUG dbKey = [" + dbKey + "]");
                Document doc = collection.find(new Document("key", dbKey)).first();
                if (doc == null) {
                    out.println("ERROR");
                    out.println("QP not found");
                    return;
                }
                String qpContent = doc.getString("content");

                // Encrypt with Caesar Cipher (key=3 fixed for demo; STUDENT CUSTOMIZE shift)
                int shift = 3;
                String encryptedQP = caesarEncrypt(qpContent, shift);
                appendLog("QP encrypted with shift=" + shift);

                // Send metadata
                out.println("OK");
                out.println(encryptedQP.length());
                out.println(shift); // Send key to client

                // Send encrypted file content as bytes for binary safety
                dos.write(encryptedQP.getBytes("UTF-8"));
                appendLog("Encrypted QP sent to client");

            } catch (Exception ex) {
                appendLog("Client handling error: " + ex.getMessage());
            }
        }).start();
    }

    // STUDENT CUSTOMIZE: Caesar Cipher Encryption (shift >0 for encrypt)
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

    private void appendLog(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExamServerGUI::new);
    }
}
