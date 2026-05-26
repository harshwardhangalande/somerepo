import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ExamClient {

    static String CLIENT_ID = "exam-app";
    static String CLIENT_SECRET ="HCSAcBhfFJ95BvaCCDygSniMRruNOCIY";
    static String TOKEN_URL =
            "http://localhost:8080/realms/OnlineExamRealm/protocol/openid-connect/token";

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter Username: ");
        String username = sc.nextLine();

        System.out.print("Enter Password: ");
        String password = sc.nextLine();

        String token = getAccessToken(username, password);

        if (token != null) {
            System.out.println("\nAuthentication Successful!");
            startExamSession(token);
        } else {
            System.out.println("\nAuthentication Failed!");
        }
    }

    public static String getAccessToken(String username, String password) {

        try {
            URL url = new URL(TOKEN_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type",
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

            int responseCode = conn.getResponseCode();
            System.out.println("Keycloak Response Code: " + responseCode);

            BufferedReader br;

            if (responseCode == 200) {
                br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
            } else {
                br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()));
            }

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            System.out.println("Keycloak Response: " + response.toString());

            if (responseCode == 200 &&
                    response.toString().contains("access_token")) {

                String token = response.toString()
                        .split("\"access_token\":\"")[1]
                        .split("\"")[0];

                return token;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void startExamSession(String token) {

        try {
        
//         InetAddress bindAddr = InetAddress.getByName("192.168.0.120");
  Socket socket = new Socket("localhost",5000);
            PrintWriter out =
                    new PrintWriter(socket.getOutputStream(), true);

            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));

            // Send token first
            out.println(token);

            // Then request question paper
            out.println("REQUEST_QP");

            String response;

            while ((response = in.readLine()) != null) {
                System.out.println(response);
                if (response.equals("END"))
                    break;
            }

            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
