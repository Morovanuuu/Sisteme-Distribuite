package ro.ubm.sd.node;

import java.io.File;

public class StorageNode {
    public static void main(String[] args) {
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

        // Afisajul exact cerut în Pasul 6
        String rol = isCoordinator ? "coordonator" : "nod de stocare";
        System.out.printf("[%s] pornit: port %d, director %s, rol: %s%n", id, port, dir, rol);
        
        // Bucla infinita pentru a mentine procesul in viata
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
