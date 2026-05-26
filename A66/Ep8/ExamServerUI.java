// ==========================
// ExamServerUI.java
// ==========================

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Base64;

public class ExamServerUI extends JFrame {

    JTextArea area;
    JButton startBtn;

    public ExamServerUI() {

        setTitle("Server - Online Exam System");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        area = new JTextArea();
        area.setFont(new Font("Consolas", Font.PLAIN, 15));
        area.setEditable(false);

        JScrollPane pane = new JScrollPane(area);

        startBtn = new JButton("Start Server (Port 5000)");

        add(pane, BorderLayout.CENTER);
        add(startBtn, BorderLayout.SOUTH);

        startBtn.addActionListener(e -> startServer());

        setVisible(true);
    }

    void startServer() {

        new Thread(() -> {

            try {

                ServerSocket serverSocket =
                        new ServerSocket(5000);

                log("🚀 Exam Server Started on Port 5000");

                while (true) {

                    Socket socket = serverSocket.accept();

                    log("\n📡 Client Connected");

                    BufferedReader in =
                            new BufferedReader(
                                    new InputStreamReader(
                                            socket.getInputStream()));

                    PrintWriter out =
                            new PrintWriter(
                                    socket.getOutputStream(),
                                    true);

                    String token = in.readLine();

                    log("🔑 Token Received");

                    if (validateToken(token)) {

                        String request = in.readLine();

                        log("📥 Request: " + request);

                        if ("REQUEST_QP".equals(request)) {

                            out.println("===== QUESTION PAPER =====");
                            out.println("1. Define OAuth 2.0");
                            out.println("2. Explain JWT Structure");
                            out.println("3. Explain Access Token");
                            out.println("==========================");
                            out.println("END");

                            log("✅ Question Paper Sent");
                        }

                    } else {

                        out.println("❌ Authentication Failed");
                        out.println("END");

                        log("❌ Invalid Token");
                    }

                    socket.close();
                }

            } catch (Exception ex) {
                log("ERROR: " + ex.getMessage());
            }

        }).start();
    }

    boolean validateToken(String token) {

        try {

            String[] parts = token.split("\\.");

            String payload = new String(
                    Base64.getUrlDecoder()
                            .decode(parts[1]));

            log("📄 Decoded Payload:");
            log(payload);

            long exp = Long.parseLong(
                    payload.split("\"exp\":")[1]
                            .split(",")[0]);

            long current =
                    System.currentTimeMillis() / 1000;

            if (current > exp) {

                log("⌛ Token Expired");
                return false;
            }

            if (payload.contains("student")) {

                log("🎓 Role Verified: student");
                return true;
            }

        } catch (Exception e) {
            log("ERROR: " + e.getMessage());
        }

        return false;
    }

    void log(String msg) {

        SwingUtilities.invokeLater(() -> {
            area.append(msg + "\n");
        });
    }

    public static void main(String[] args) {

        new ExamServerUI();
    }
}