package ro.ubm.sd.client;

import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String coordinatorAddress = "";

        // Parsare argument: --coordinator localhost:7001
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--coordinator")) {
                coordinatorAddress = args[++i];
            }
        }

        if (coordinatorAddress.isEmpty()) {
            System.out.println("Eroare: Lipseste argumentul --coordinator <IP:PORT>");
            System.exit(1);
        }

        // Afisajul exact cerut in Pasul 6
        System.out.printf("[client] coordonator configurat: %s; comenzi: put, get, ls, rm, nodes, exit%n", coordinatorAddress);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) break;
            
            String command = scanner.nextLine().trim();
            if (command.equalsIgnoreCase("exit")) {
                break;
            } else if (!command.isEmpty()) {
                System.out.println("Comandă interceptată: " + command + " (va fi implementată în Etapa 2)");
            }
        }
        scanner.close();
    }
}
