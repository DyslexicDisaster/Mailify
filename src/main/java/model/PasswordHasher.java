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


    /**
     * Hashes a password using PBKDF2 with HmacSHA256.
     *
     * @param password the password to hash
     * @return the hashed password as a Base64 encoded string
     */
    public static String hashPassword(String password) {
        try {
            // Generate a random salt
            byte[] salt = generateSalt();

            char[] chars = password.toCharArray();

            // Create the hash using the same algorithm and parameters
            PBEKeySpec spec = new PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH);

            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);

            byte[] hash = skf.generateSecret(spec).getEncoded();

            // Clear the password array for security
            java.util.Arrays.fill(chars, '\u0000');

            // Encode the salt and hash to Base64
            String saltString = Base64.getEncoder().encodeToString(salt);
            String hashString = Base64.getEncoder().encodeToString(hash);

            // Combine the salt and hash into a single string with the seperator
            return saltString + SEPARATOR + hashString;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();

            return String.valueOf(password.hashCode());
        }
    }


    /** Verifies a password against a stored hash.
     *
     * @param password    the password to verify
     * @param storedValue the stored hash (salt:hash)
     * @return true if the password matches the hash, false otherwise
     */
    public static boolean verifyPassword(String password, String storedValue) {
        try {
            // Split the stored value into salt and hash
            String[] parts = storedValue.split(SEPARATOR);
            if (parts.length != 2) {
                return false;
            }

            String saltString = parts[0];
            String storedHash = parts[1];
            // Decode the salt and hash
            byte[] salt = Base64.getDecoder().decode(saltString);

            char[] chars = password.toCharArray();

            // Create the hash using the same algorithm and parameters
            PBEKeySpec spec = new PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH);

            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);

            // Generate the hash
            byte[] hash = skf.generateSecret(spec).getEncoded();

            java.util.Arrays.fill(chars, '\u0000');

            // Encode the hash to Base64
            String hashString = Base64.getEncoder().encodeToString(hash);

            // Compare the generated hash with the stored hash
            return hashString.equals(storedHash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Generates a random salt.
     *
     * @return a byte array containing the salt
     */
    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }
}