package ro.ubm.sd.node;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ro.ubm.sd.common.Discovery;
import ro.ubm.sd.common.Protocol;

public class StorageNode {
    private final String id;
    private final Path dir;
    private final ExecutorService pool = Executors.newFixedThreadPool(16);
    private Discovery discovery;

    private StorageNode(String id, Path dir) {
        this.id = id;
        this.dir = dir;
    }

    public static void main(String[] args) throws IOException {
        String id = "unknown";
        int port = 0;
        String dir = "";
        boolean isCoordinator = false;

        // Parsare argumente de tipul: --id n1 --port 7001 --dir data/n1 --coordinator
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--id":
                    id = args[++i];
                    break;
                case "--port":
                    port = Integer.parseInt(args[++i]);
                    break;
                case "--dir":
                    dir = args[++i];
                    break;
                case "--coordinator":
                    isCoordinator = true;
                    break;
            }
        }

        // Verificăm dacă argumentele de bază au fost transmise
        if (id.equals("unknown") || port == 0 || dir.isEmpty()) {
            System.out.println("Eroare: Argumente incomplete. Rulați cu --id <ID> --port <PORT> --dir <DIR>");
            System.exit(1);
        }

        // Creare director dacă nu există
        File directory = new File(dir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String rol = isCoordinator ? "coordonator" : "nod de stocare";
        new StorageNode(id, Path.of(dir).toAbsolutePath().normalize()).run(port, dir, rol);
    }

    private void run(int port, String dirName, String rol) throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            log(String.format("pornit: port TCP %d, director %s, rol: %s", port, dirName, rol));
            discovery = new Discovery(id, m -> log("nod descoperit: " + m + " (cunosc acum " + discovery.members().size() + ")"));
            discovery.startListening();
            discovery.startAnnouncing(port);
            while (true) {
                Socket socket = server.accept();
                pool.submit(() -> handle(socket));
            }
        } catch (BindException e) {
            System.out.println("Eroare: portul " + port + " este deja utilizat");
            System.exit(1);
        }
    }

    // O conexiune transporta o singura cerere
    private void handle(Socket socket) {
        try (socket) {
            socket.setSoTimeout(30_000); // un client blocat nu tine ocupat un fir din pool la nesfarsit
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            String[] h = Protocol.readHeader(in);
            try {
                if (h[0].equals("PUT") && h.length == 4) {
                    handlePut(h, in, out);
                } else if (h[0].equals("GET") && h.length == 2) {
                    handleGet(h, out);
                } else {
                    Protocol.writeHeader(out, "ERROR antet invalid");
                }
            } catch (IllegalArgumentException e) { // dimensiune sau nume invalid primit de pe retea
                Protocol.writeHeader(out, "ERROR antet invalid");
            }
        } catch (IOException e) {
            log("conexiune esuata: " + e);
        }
    }

    // PUT <nume> <dimensiune> <sha256> + octeti
    private void handlePut(String[] h, DataInputStream in, DataOutputStream out) throws IOException {
        Path target = resolveInDir(h[1]);
        long size = Long.parseLong(h[2]);
        if (size < 0) {
            throw new IllegalArgumentException("dimensiune negativa");
        }
        // UUID: doua incarcari simultane cu acelasi nume nu scriu in acelasi .part
        Path tmp = dir.resolve(h[1] + "." + UUID.randomUUID() + ".part");
        try {
            String actual = Protocol.receiveBytes(in, size, tmp);
            if (!actual.equals(h[3])) {
                Protocol.writeHeader(out, "ERROR checksum diferit");
                return;
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log("stocat: " + h[1] + " (" + size + " octeti)");
            Protocol.writeHeader(out, "STORED " + actual);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // GET <nume>
    private void handleGet(String[] h, DataOutputStream out) throws IOException {
        Path file = resolveInDir(h[1]);
        if (!Files.isRegularFile(file)) {
            Protocol.writeHeader(out, "NOT_FOUND");
            return;
        }
        Protocol.writeHeader(out, "FILE " + Files.size(file) + " " + Protocol.checksum(file));
        Files.copy(file, out);
        out.flush();
    }

    // Numele vine de pe retea: "../x" sau o cale absoluta nu au voie sa iasa din directorul nodului
    private Path resolveInDir(String name) {
        Path path = dir.resolve(name).normalize();
        if (!dir.equals(path.getParent())) {
            throw new IllegalArgumentException("nume invalid: " + name);
        }
        return path;
    }

    private void log(String message) {
        System.out.println(LocalTime.now().truncatedTo(ChronoUnit.MILLIS) + " [" + id + "] " + message);
    }
}
