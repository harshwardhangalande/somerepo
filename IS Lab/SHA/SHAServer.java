import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.spec.DHParameterSpec;
import java.util.Arrays;

public class SHAServer {

    public static void main(String[] args) throws Exception {

        ServerSocket serverSocket = new ServerSocket(5000);
        Socket socket = serverSocket.accept();

        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        // Generate server key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
        kpg.initialize(2048);
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

        // Create DES key
        SecretKey desKey = new SecretKeySpec(sharedSecret, 0, 8, "DES");

        // Receive encrypted message
        byte[] encryptedMsg = (byte[]) in.readObject();

        // Receive hash
        byte[] receivedHash = (byte[]) in.readObject();

        // Decrypt message
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, desKey);
        byte[] decryptedMsg = cipher.doFinal(encryptedMsg);

        String message = new String(decryptedMsg);
        System.out.println("Decrypted Message: " + message);

        // Compute SHA-256 again
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] computedHash = md.digest(message.getBytes());

        // Verify integrity
        if (Arrays.equals(receivedHash, computedHash)) {
            System.out.println("Integrity Verified. Message not modified.");
        } else {
            System.out.println("Message Integrity Failed!");
        }

        socket.close();
        serverSocket.close();
    }
}