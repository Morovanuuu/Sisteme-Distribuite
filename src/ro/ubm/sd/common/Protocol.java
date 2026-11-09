package ro.ubm.sd.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Protocol {
    // Echipa k foloseste 239.255.0.k
    public static final String GROUP = "239.255.0.1";
    public static final int GROUP_PORT = 4446;

    private Protocol() {
    }

    public static void writeHeader(DataOutputStream out, String header) throws IOException {
        out.writeUTF(header);
        out.flush();
    }

    // shortcut: campurile sunt separate prin spatiu, deci numele de fisier nu pot contine spatii; upgrade: antet JSON
    public static String[] readHeader(DataInputStream in) throws IOException {
        return in.readUTF().split(" ");
    }

    public static String checksum(Path file) throws IOException {
        MessageDigest sha = sha256();
        byte[] buffer = new byte[64 * 1024];
        try (InputStream in = Files.newInputStream(file)) {
            int n;
            while ((n = in.read(buffer)) >= 0) {
                sha.update(buffer, 0, n);
            }
        }
        return HexFormat.of().formatHex(sha.digest());
    }

    public static String receiveBytes(InputStream in, long size, Path target) throws IOException {
        MessageDigest sha = sha256();
        byte[] buffer = new byte[64 * 1024];
        long remaining = size;
        try (OutputStream out = Files.newOutputStream(target)) {
            while (remaining > 0) {
                int n = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (n < 0) {
                    throw new EOFException("conexiune inchisa dupa " + (size - remaining) + " din " + size + " octeti");
                }
                out.write(buffer, 0, n);
                sha.update(buffer, 0, n);
                remaining -= n;
            }
        }
        return HexFormat.of().formatHex(sha.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
