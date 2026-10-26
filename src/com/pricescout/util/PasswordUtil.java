package com.pricescout.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Salted PBKDF2-HMAC-SHA256 password hashing (built into Java, no extra jars).
 * Stored format: pbkdf2_sha256$iterations$saltBase64$hashBase64
 */
public final class PasswordUtil {

    private static final String PREFIX = "pbkdf2_sha256";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() { }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        Base64.Encoder enc = Base64.getEncoder();
        return PREFIX + "$" + ITERATIONS + "$" + enc.encodeToString(salt) + "$" + enc.encodeToString(hash);
    }

    /** Returns false for anything not in our format (e.g. placeholder seed hashes). */
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) return false;
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            Base64.Decoder dec = Base64.getDecoder();
            byte[] salt = dec.decode(parts[2]);
            byte[] expected = dec.decode(parts[3]);
            byte[] actual = derive(password.toCharArray(), salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations, int bits) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bits);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    /** Helper for making seed/admin accounts: java com.pricescout.util.PasswordUtil <password> */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: java com.pricescout.util.PasswordUtil <password>");
            System.exit(1);
        }
        System.out.println(hash(args[0]));
    }
}
