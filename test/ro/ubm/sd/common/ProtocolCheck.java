package ro.ubm.sd.common;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProtocolCheck {
    public static void main(String[] args) throws Exception {
        Path target = Files.createTempFile("protocol", ".bin");
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream("abcdefXY".getBytes());
            String sha = Protocol.receiveBytes(stream, 6, target);
            check(Files.readString(target).equals("abcdef"), "citeste exact dimensiunea");
            check(stream.available() == 2, "nu consuma octetii mesajului urmator");
            check(sha.equals(Protocol.checksum(target)), "checksum identic cu cel din fisier");

            try {
                Protocol.receiveBytes(new ByteArrayInputStream(new byte[3]), 6, target);
                check(false, "fluxul trunchiat trebuie respins");
            } catch (EOFException expected) {
                System.out.println("OK: flux trunchiat respins");
            }
        } finally {
            Files.deleteIfExists(target);
        }
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError(name);
        }
        System.out.println("OK: " + name);
    }
}
