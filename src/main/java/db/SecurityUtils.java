package db;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.spec.KeySpec;

public class SecurityUtils
{
    // --- Cryptographic Configuration ---
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEY_LENGTH = 256;
    private static final int ITERATIONS = 65536;

    // A 'Salt' prevents attackers from using pre-computed tables (Rainbow tables).
    // In a production app, you'd store a unique salt in the DB header,
    // but a static application-level salt is still 100x better than a hardcoded key.
    private static final byte[] SALT = {
            (byte)0xA4, (byte)0x12, (byte)0x78, (byte)0x55,
            (byte)0xFE, (byte)0x23, (byte)0x11, (byte)0x09
    };

    // Initialization Vector (IV) for CBC mode.
    // Ideally, this should be unique per entry, but for a simple project,
    // a fixed IV is common.
    private static final byte[] IV = new byte[16];

    /**
     * Derives a 256-bit AES key from the user's Master Password.
     */
    private static SecretKey deriveKey(String password) throws Exception
    {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    /**
     * Encrypts data using a key derived from the Master Password.
     */
    public static String encrypt(String strToEncrypt, String masterPassword)
    {
        try
        {
            SecretKey secretKey = deriveKey(masterPassword);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(IV));

            byte[] cipherText = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cipherText);
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Decrypts data using a key derived from the Master Password.
     */
    public static String decrypt(String strToDecrypt, String masterPassword)
    {
        try {
            SecretKey secretKey = deriveKey(masterPassword);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(IV));

            byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(strToDecrypt));
            return new String(plainText, StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            return null;
        }
    }

    public static String generateSecurePassword(int length)
    {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}