package travelagency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Password hashing. Passwords are never stored; only SHA-256(salt + password),
 * with a different random salt for every account.
 *
 * @author Hafsa Khurram, Zainab Asif, Syeda Humaima
 */
public final class Security {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Security() {
    }

    public static String newSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return hex(bytes);
    }

    public static String hash(String salt, char[] password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] pw = new String(password).getBytes(StandardCharsets.UTF_8);
            byte[] result = digest.digest(pw);
            Arrays.fill(pw, (byte) 0);
            return hex(result);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
