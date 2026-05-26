// ==========================
// ExamClientUI.java
// ==========================

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ExamClientUI extends JFrame {

    JTextField userField;
    JPasswordField passField;

    JTextArea area;

    JButton loginBtn;

    static String CLIENT_ID = "exam-app";

    static String CLIENT_SECRET =
            "gm1mYGhiiXg96LF11a44UP0W5dJVcETQ";

    static String TOKEN_URL =
            "http://localhost:8080/realms/onlineexamrealm/protocol/openid-connect/token";

    public ExamClientUI() {

        setTitle("Client - Online Exam System");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BorderLayout());

        JPanel top = new JPanel(new GridLayout(3, 2, 10, 10));

        top.setBorder(BorderFactory.createTitledBorder("Student Login"));

        top.add(new JLabel("Username:"));
        userField = new JTextField();
        top.add(userField);

        top.add(new JLabel("Password:"));
        passField = new JPasswordField();
        top.add(passField);

        loginBtn = new JButton("Request Secure QP");

        top.add(loginBtn);

        area = new JTextArea();
        area.setFont(new Font("Consolas", Font.PLAIN, 15));

        JScrollPane pane = new JScrollPane(area);

        add(top, BorderLayout.NORTH);
        add(pane, BorderLayout.CENTER);

        loginBtn.addActionListener(e -> authenticate());

        setVisible(true);
    }

    void authenticate() {

        new Thread(() -> {

            try {

                String username = userField.getText();

                String password =
                        new String(passField.getPassword());

                log("🔐 Authenticating...");

                String token =
                        getAccessToken(username, password);

                if (token != null) {

                    log("✅ Authentication Successful");

                    startExamSession(token);

                } else {

                    log("❌ Authentication Failed");
                }

            } catch (Exception e) {

                log("ERROR: " + e.getMessage());
            }

        }).start();
    }

    String getAccessToken(String username,
                          String password) {

        try {

            URL url = new URL(TOKEN_URL);

            HttpURLConnection conn =
                    (HttpURLConnection)
                            url.openConnection();

            conn.setRequestMethod("POST");

            conn.setDoOutput(true);

            conn.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded");

            String params =
                    "client_id=" + CLIENT_ID +
                    "&client_secret=" + CLIENT_SECRET +
                    "&grant_type=password" +
                    "&username=" + username +
                    "&password=" + password;

            OutputStream os = conn.getOutputStream();

            os.write(params.getBytes());

            os.flush();

            int responseCode =
                    conn.getResponseCode();

            BufferedReader br;

            if (responseCode == 200) {

                br = new BufferedReader(
                        new InputStreamReader(
                                conn.getInputStream()));

            } else {

                br = new BufferedReader(
                        new InputStreamReader(
                                conn.getErrorStream()));
            }

            StringBuilder response =
                    new StringBuilder();

            String line;

            while ((line = br.readLine()) != null) {

                response.append(line);
            }

            log("🌐 Keycloak Response Code: "
                    + responseCode);

            if (responseCode == 200 &&
                    response.toString()
                            .contains("access_token")) {

                return response.toString()
                        .split("\"access_token\":\"")[1]
                        .split("\"")[0];
            }

        } catch (Exception e) {

            log("ERROR: " + e.getMessage());
        }

        return null;
    }

    void startExamSession(String token) {

        try {

            Socket socket =
                    new Socket("localhost", 5000);

            PrintWriter out =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true);

            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()));

            out.println(token);

            out.println("REQUEST_QP");

            log("\n📘 Receiving Question Paper...\n");

            String response;

            while ((response = in.readLine()) != null) {

                if (response.equals("END"))
                    break;

                log(response);
            }

            socket.close();

        } catch (Exception e) {

            log("ERROR: " + e.getMessage());
        }
    }

    void log(String msg) {

        SwingUtilities.invokeLater(() -> {
            area.append(msg + "\n");
        });
    }

    public static void main(String[] args) {

        new ExamClientUI();
    }
}