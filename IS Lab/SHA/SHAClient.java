import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.spec.DHParameterSpec;
import java.security.MessageDigest;

public class SHAClient {

    public static void main(String[] args) throws Exception {

        Socket socket = new Socket("localhost", 5000);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        // Receive server public key
        byte[] serverPubKeyEnc = (byte[]) in.readObject();
        KeyFactory kf = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(serverPubKeyEnc);
        PublicKey serverPubKey = kf.generatePublic(x509Spec);

        // Generate client key pair using same parameters
        DHParameterSpec dhParamSpec = ((javax.crypto.interfaces.DHPublicKey) serverPubKey).getParams();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
        kpg.initialize(dhParamSpec);
        KeyPair kp = kpg.generateKeyPair();

        // Send client public key
        out.writeObject(kp.getPublic().getEncoded());
        out.flush();

        // Generate shared secret
        KeyAgreement ka = KeyAgreement.getInstance("DH");
        ka.init(kp.getPrivate());
        ka.doPhase(serverPubKey, true);
        byte[] sharedSecret = ka.generateSecret();

        // Create DES key
        SecretKey desKey = new SecretKeySpec(sharedSecret, 0, 8, "DES");

        // Message
        String message = "Hello Secure World";

        // Encrypt message using DES
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, desKey);
        byte[] encryptedMsg = cipher.doFinal(message.getBytes());

        // Generate SHA-256 hash of original message
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(message.getBytes());

        // Send encrypted message
        out.writeObject(encryptedMsg);

        // Send hash
        out.writeObject(hash);

        out.flush();
        socket.close();
    }
}