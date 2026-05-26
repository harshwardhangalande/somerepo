package ClientServer;
import java.io.*;
import java.net.*;
import java.util.Scanner;

class Client {
    public static void main(String args[]) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter size of array: ");
        int n = sc.nextInt();
        int a[] = new int[n];
        System.out.println("Enter " + n + " numbers:");
        for(int i = 0; i < n; i++) {
            a[i] = sc.nextInt();
        }
        Socket s = new Socket("localhost", 7777);
        DataInputStream din = new DataInputStream(s.getInputStream());
        DataOutputStream dout = new DataOutputStream(s.getOutputStream());
        dout.writeInt(n);
        for(int i = 0; i < n; i++) {
            dout.writeInt(a[i]);
        }
        System.out.println("Original array: ");
        for(int i = 0; i < n; i++) {
            System.out.print(a[i] + " ");
        }
        System.out.println("\nSorted array received from server:");
        for(int i = 0; i < n; i++) {
            a[i] = din.readInt();
            System.out.print(a[i] + " ");
        }
        System.out.println();
        s.close();
        sc.close();
    }
}
