import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import javax.swing.*;

public class ExamClientRSA extends JFrame {

        private JTextField classField, codeField, nameField, semField;
        private JTextArea outputArea;
        private JButton requestBtn;

        public ExamClientRSA() {
                setTitle("Exam Client - RSA Verification");
                setSize(500, 400);
                setDefaultCloseOperation(EXIT_ON_CLOSE);
                setLayout(new BorderLayout());

                JPanel panel = new JPanel(new GridLayout(5, 2));

                panel.add(new JLabel("Class:"));
                classField = new JTextField("10");
                panel.add(classField);

                panel.add(new JLabel("Course Code:"));
                codeField = new JTextField("CS101");
                panel.add(codeField);

                panel.add(new JLabel("Course Name:"));
                nameField = new JTextField("Intro");
                panel.add(nameField);

                panel.add(new JLabel("Semester:"));
                semField = new JTextField("1");
                panel.add(semField);

                requestBtn = new JButton("Request QP");
                requestBtn.addActionListener(e -> requestQP());
                panel.add(requestBtn);

                add(panel, BorderLayout.NORTH);

                outputArea = new JTextArea();
                add(new JScrollPane(outputArea), BorderLayout.CENTER);

                setVisible(true);
        }

        private void requestQP() {
                new Thread(() -> {
                        try (Socket socket = new Socket("localhost", 8080);
                                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                                        BufferedReader in = new BufferedReader(
                                                        new InputStreamReader(socket.getInputStream()));
                                        DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                                out.println(classField.getText());
                                out.println(codeField.getText());
                                out.println(nameField.getText());
                                out.println(semField.getText());

                                String status = in.readLine();
                                if ("ERROR".equals(status)) {
                                        outputArea.setText("Error: " + in.readLine());
                                        return;
                                }

                                int len = Integer.parseInt(in.readLine());
                                String signatureStr = in.readLine();
                                String pubKeyStr = in.readLine();

                                byte[] data = new byte[len];
                                dis.readFully(data);
                                String qp = new String(data, "UTF-8");

                                // 🔓 Convert public key
                                byte[] pubBytes = Base64.getDecoder().decode(pubKeyStr);
                                X509EncodedKeySpec spec = new X509EncodedKeySpec(pubBytes);
                                KeyFactory kf = KeyFactory.getInstance("RSA");
                                PublicKey publicKey = kf.generatePublic(spec);

                                // 🔐 Verify signature
                                byte[] signature = Base64.getDecoder().decode(signatureStr);
                                boolean verified = verifySignature(qp, signature, publicKey);

                                if (verified) {
                                        outputArea.setText("✅ VERIFIED\n\n" + qp);
                                } else {
                                        outputArea.setText("❌ TAMPERED DATA!");
                                }

                        } catch (Exception e) {
                                outputArea.setText("Error: " + e.getMessage());
                        }
                }).start();
        }

        // VERIFY METHOD
        private boolean verifySignature(String data, byte[] signature, PublicKey key) throws Exception {
                Signature verify = Signature.getInstance("SHA256withRSA");
                verify.initVerify(key);
                verify.update(data.getBytes());
                return verify.verify(signature);
        }

        public static void main(String[] args) {
                SwingUtilities.invokeLater(ExamClientRSA::new);
        }
}