import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

// Changed class name to match your filename Server1.java
public class Server1 extends JFrame {

    private ServerSocket serverSocket;
    private Map<String, String> qpDatabase;
    private JTextArea logArea;

    public Server1() { // Constructor must match class name
        qpDatabase = new HashMap<>();
        qpDatabase.put("10-CS101-1", "Question 1: What is Java? (5 marks)\n" + 
                                     "Question 2: Explain OOP concepts. (10 marks)");
        qpDatabase.put("10-CS301-3", "Question 1: Explain Socket Programming. (8 marks)\n" +
                                     "Question 2: Implement Transposition Cipher. (12 marks)");

        setTitle("IS Lab Exam Server");
        setSize(650, 420);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JButton startBtn = new JButton("Start Server (Port 8080)");
        startBtn.addActionListener(e -> {
            startBtn.setEnabled(false);
            startServer();
        });
        add(startBtn, BorderLayout.SOUTH);

        setVisible(true);
    }

    // ... (rest of startServer, handleClient, and transpositionEncrypt methods stay the same)

    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(8080);
                appendLog("Server started on port 8080...");
                while (!serverSocket.isClosed()) {
                    Socket client = serverSocket.accept();
                    handleClient(client);
                }
            } catch (Exception ex) {
                appendLog("Server Error: " + ex.getMessage());
            }
        }).start();
    }

    private void handleClient(Socket client) {
        new Thread(() -> {
            try (Socket s = client; 
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                 PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                 DataOutputStream dos = new DataOutputStream(s.getOutputStream())) {

                String cls = in.readLine();
                String code = in.readLine();
                in.readLine(); // skip name
                String sem = in.readLine();

                String dbKey = cls + "-" + code + "-" + sem;
                String qp = qpDatabase.get(dbKey);

                if (qp != null) {
                    int key = 4;
                    String encrypted = transpositionEncrypt(qp, key);
                    byte[] data = encrypted.getBytes("UTF-8");
                    out.println("OK");
                    out.println(data.length);
                    out.println(key);
                    dos.write(data);
                } else {
                    out.println("ERROR");
                }
            } catch (Exception ex) { appendLog("Error: " + ex.getMessage()); }
        }).start();
    }

    private String transpositionEncrypt(String text, int key) {
        text = text.replaceAll("\\s+", "_");
        int rows = (int) Math.ceil((double) text.length() / key);
        char[][] matrix = new char[rows][key];
        int idx = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < key; c++) {
                matrix[r][c] = (idx < text.length()) ? text.charAt(idx++) : 'X';
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < key; c++) {
            for (int r = 0; r < rows; r++) sb.append(matrix[r][c]);
        }
        return sb.toString();
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }

    public static void main(String[] args) {
        // Main method must call the new class name
        SwingUtilities.invokeLater(Server1::new); 
    }
}
