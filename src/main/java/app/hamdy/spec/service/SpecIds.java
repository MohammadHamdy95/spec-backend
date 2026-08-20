package app.hamdy.spec.service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Ids and edit tokens.
 *
 * <p>Ids are 10-char base62 like paste's (~8 x 10^17), unguessable enough
 * that an unlisted spec is not practically enumerable.</p>
 *
 * <p>Edit tokens are longer — 32 chars — because an id merely reveals a
 * document while a token rewrites it for everyone holding the link. Only the
 * SHA-256 is ever stored, so the token is shown once at creation and a
 * database read cannot recover it.</p>
 */
public final class SpecIds {

    static final int ID_LENGTH = 10;
    static final int TOKEN_LENGTH = 32;

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private SpecIds() {
    }

    public static String generateId() {
        return random(ID_LENGTH);
    }

    public static String generateEditToken() {
        return random(TOKEN_LENGTH);
    }

    /** SHA-256, hex encoded. What we persist in place of the token itself. */
    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Constant-time comparison. A timing-sensitive equals() on a secret is a
     * small leak, but a free one to avoid.
     */
    public static boolean tokenMatches(String presented, String storedHash) {
        if (presented == null || storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hashToken(presented).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }

    private static String random(int length) {
        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(out);
    }
}
