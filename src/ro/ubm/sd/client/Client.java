package ro.ubm.sd.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Scanner;

import ro.ubm.sd.common.Discovery;
import ro.ubm.sd.common.Discovery.Member;
import ro.ubm.sd.common.Protocol;

public class Client {
    private static final String USAGE = "comenzi: nodes | put <nod> <fisier> | get <nod> <nume> <destinatie> | exit";

    public static void main(String[] args) {
        System.out.println("[client] " + USAGE);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) break;

            String command = scanner.nextLine().trim();
            if (command.equalsIgnoreCase("exit")) {
                break;
            } else if (!command.isEmpty()) {
                try {
                    execute(command.split("\\s+"));
                } catch (IOException | InterruptedException | IllegalArgumentException e) {
                    System.out.println("Eroare: " + e);
                }
            }
        }
        scanner.close();
    }

    private static void execute(String[] c) throws IOException, InterruptedException {
        if (c[0].equals("nodes") && c.length == 1) {
            discover().values().forEach(System.out::println);
        } else if (c[0].equals("put") && c.length == 3) {
            put(find(c[1]), Path.of(c[2]));
        } else if (c[0].equals("get") && c.length == 4) {
            get(find(c[1]), c[2], Path.of(c[3]));
        } else {
            System.out.println(USAGE);
        }
    }

    // 3 secunde: mai mult decat intervalul de 2 secunde dintre anunturi
    private static Map<String, Member> discover() throws IOException, InterruptedException {
        try (Discovery discovery = new Discovery(null, m -> { })) {
            discovery.startListening();
            Thread.sleep(3000);
            return discovery.members();
        }
    }

    private static Member find(String nodeId) throws IOException, InterruptedException {
        Member member = discover().get(nodeId);
        if (member == null) {
            throw new IOException("nodul " + nodeId + " nu a fost descoperit");
        }
        return member;
    }

    private static Socket connect(Member node) throws IOException {
        Socket socket = new Socket(node.address(), node.port());
        socket.setSoTimeout(30_000);
        return socket;
    }

    private static void put(Member node, Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            System.out.println("Eroare: fisier local inexistent: " + file);
            return;
        }
        try (Socket socket = connect(node)) {
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            String sha = Protocol.checksum(file);
            Protocol.writeHeader(out, "PUT " + file.getFileName() + " " + Files.size(file) + " " + sha);
            Files.copy(file, out);
            out.flush();
            System.out.println(String.join(" ", Protocol.readHeader(in)));
        }
    }

    private static void get(Member node, String name, Path destination) throws IOException {
        try (Socket socket = connect(node)) {
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            Protocol.writeHeader(out, "GET " + name);
            String[] h = Protocol.readHeader(in);
            if (!h[0].equals("FILE") || h.length != 3) {
                System.out.println(String.join(" ", h));
                return;
            }
            Files.createDirectories(destination);
            Path target = destination.resolve(name);
            Path tmp = destination.resolve(name + ".part");
            try {
                String actual = Protocol.receiveBytes(in, Long.parseLong(h[1]), tmp);
                if (!actual.equals(h[2])) {
                    System.out.println("Eroare: checksum diferit, fisierul nu a fost salvat");
                    return;
                }
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                System.out.println("descarcat " + target + ", checksum OK");
            } finally {
                Files.deleteIfExists(tmp);
            }
        }
    }
}
