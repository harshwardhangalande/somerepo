    import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;

public class DHServer {

    public static void main(String[] args) throws Exception {

        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Server started. Waiting for client...");
        Socket socket = serverSocket.accept();
        System.out.println("Client connected.");

        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

        // Generate Diffie-Hellman key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();

        // Send server public key
        out.writeObject(kp.getPublic().getEncoded());
        out.flush();

        // Receive client public key
        byte[] clientPubKeyEnc = (byte[]) in.readObject();
        KeyFactory kf = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(clientPubKeyEnc);
        PublicKey clientPubKey = kf.generatePublic(x509Spec);

        // Generate shared secret
        KeyAgreement ka = KeyAgreement.getInstance("DH");
        ka.init(kp.getPrivate());
        ka.doPhase(clientPubKey, true);
        byte[] sharedSecret = ka.generateSecret();

        // Create DES key from shared secret
        SecretKey desKey = new SecretKeySpec(sharedSecret, 0, 8, "DES");

        // Receive encrypted message
        byte[] encryptedMsg = (byte[]) in.readObject();

        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, desKey);
        byte[] decrypted = cipher.doFinal(encryptedMsg);

        System.out.println("Decrypted message from client: " + new String(decrypted));

        socket.close();
        serverSocket.close();
    }
}
