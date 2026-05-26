import java.io.*;
import java.net.*;
import java.util.Base64;

public class ExamServer {

    public static void main(String[] args) throws Exception {
//    InetAddress bindAddr = InetAddress.getByName("192.168.0.120");
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Exam Server Started...");

        while (true) {

            Socket socket = serverSocket.accept();
            System.out.println("Client Connected");

            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));

            PrintWriter out =
                    new PrintWriter(socket.getOutputStream(), true);

            String token = in.readLine();

            System.out.println("Token Received: " + token);

            if (validateToken(token)) {

                String request = in.readLine();

                if ("REQUEST_QP".equals(request)) {

                    out.println("Question Paper:");
                    out.println("1. Define OAuth 2.0.");
                    out.println("2. Explain JWT Structure.");
                    out.println("END");
                }

            } else {

                out.println("Authentication Failed.");
                out.println("END");
            }

            socket.close();
        }
    }

    public static boolean validateToken(String token) {

        try {
            String[] parts = token.split("\\.");

            String payload = new String(
                    Base64.getUrlDecoder().decode(parts[1]));

            System.out.println("Decoded Payload: " + payload);

            // Check expiration
            long exp = Long.parseLong(
                    payload.split("\"exp\":")[1].split(",")[0]);

            long currentTime =
                    System.currentTimeMillis() / 1000;

            if (currentTime > exp) {
                System.out.println("Token Expired!");
                return false;
            }

            // Check Student role
            if (payload.contains("\"roles\":[\"student\"") ||
                payload.contains("student")) {

                System.out.println("Role Verified: student");
                return true;
            }

            System.out.println("Role Not Found!");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
