import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.util.Base64;

public class DESUtil {

    private static final String SECRET_KEY = "12345678";

    private static SecretKey getKey() throws Exception {
        DESKeySpec keySpec = new DESKeySpec(SECRET_KEY.getBytes());
        SecretKeyFactory keyFactory =
                SecretKeyFactory.getInstance("DES");
        return keyFactory.generateSecret(keySpec);
    }

    public static String encrypt(String data) throws Exception {

        SecretKey key = getKey();
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encrypted = cipher.doFinal(data.getBytes());

        return Base64.getEncoder()
                .encodeToString(encrypted);
    }

    public static String decrypt(String encryptedData)
            throws Exception {

        SecretKey key = getKey();
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decoded =
                Base64.getDecoder().decode(encryptedData);

        return new String(cipher.doFinal(decoded));
    }
}
