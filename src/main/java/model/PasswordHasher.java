package model;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class PasswordHasher {
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH = 16;


    private static final String SEPARATOR = ":";


    public static String hashPassword(String password) {
        try {
            byte[] salt = generateSalt();

            char[] chars = password.toCharArray();

            PBEKeySpec spec = new PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH);

            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);

            byte[] hash = skf.generateSecret(spec).getEncoded();

            java.util.Arrays.fill(chars, '\u0000');

            String saltString = Base64.getEncoder().encodeToString(salt);
            String hashString = Base64.getEncoder().encodeToString(hash);

            return saltString + SEPARATOR + hashString;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();

            return String.valueOf(password.hashCode());
        }
    }


    public static boolean verifyPassword(String password, String storedValue) {
        try {
            String[] parts = storedValue.split(SEPARATOR);
            if (parts.length != 2) {
                return false;
            }

            String saltString = parts[0];
            String storedHash = parts[1];

            byte[] salt = Base64.getDecoder().decode(saltString);

            char[] chars = password.toCharArray();

            PBEKeySpec spec = new PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH);

            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);

            byte[] hash = skf.generateSecret(spec).getEncoded();

            java.util.Arrays.fill(chars, '\u0000');

            String hashString = Base64.getEncoder().encodeToString(hash);

            return hashString.equals(storedHash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }
}