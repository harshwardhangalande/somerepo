package ClientServer;
import java.io.*;
import java.net.*;
import java.util.Arrays;

class Server {
    public static void main(String args[]) throws Exception {
        ServerSocket ss = new ServerSocket(7777);
        Socket s = ss.accept();
        System.out.println("connected...");
        DataInputStream din = new DataInputStream(s.getInputStream());
        DataOutputStream dout = new DataOutputStream(s.getOutputStream());
        int n = din.readInt();
        int a[] = new int[n];
        System.out.println("Receiving Data...");
        for(int i = 0; i < n; i++) {
            a[i] = din.readInt();
        }
        System.out.println("Data Received. Original: " + Arrays.toString(a));
        System.out.println("Sorting Data...");
        Arrays.sort(a);
        for(int i = 0; i < n; i++) {
            dout.writeInt(a[i]);
        }
        System.out.println("Data Sent Successfully: " + Arrays.toString(a));
        s.close();
        ss.close();
    }
}



