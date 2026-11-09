package ro.ubm.sd.common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class Discovery implements AutoCloseable {
    public record Member(String id, InetAddress address, int port, long lastSeen) {
        @Override
        public String toString() {
            return id + "@" + address.getHostAddress() + ":" + port;
        }
    }

    private final String selfId;
    private final Consumer<Member> onNewMember;
    private final Map<String, Member> members = new ConcurrentHashMap<>();
    private final InetSocketAddress group;
    private final MulticastSocket socket;

    public Discovery(String selfId, Consumer<Member> onNewMember) throws IOException {
        this.selfId = selfId;
        this.onNewMember = onNewMember;
        group = new InetSocketAddress(InetAddress.getByName(Protocol.GROUP), Protocol.GROUP_PORT);
        socket = new MulticastSocket(Protocol.GROUP_PORT);
        socket.joinGroup(group, null);
    }

    public Map<String, Member> members() {
        return members;
    }

    public void startListening() {
        Thread listener = new Thread(this::listen, "discovery-listen");
        listener.setDaemon(true);
        listener.start();
    }

    public void startAnnouncing(int tcpPort) {
        byte[] hello = ("HELLO;" + selfId + ";" + tcpPort).getBytes(StandardCharsets.UTF_8);
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                socket.send(new DatagramPacket(hello, hello.length, group));
            } catch (IOException e) {
                System.err.println("[" + selfId + "] anunt esuat: " + e.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void listen() {
        byte[] buffer = new byte[1024];
        while (!socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String[] p = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).split(";");
                if (p.length != 3 || !p[0].equals("HELLO") || p[1].equals(selfId)) {
                    continue;
                }
                // Adresa se ia din datagrama: getLocalHost() poate intoarce 127.0.1.1 pe Linux
                Member m = new Member(p[1], packet.getAddress(), Integer.parseInt(p[2]), System.currentTimeMillis());
                if (members.put(m.id(), m) == null) {
                    onNewMember.accept(m);
                }
            } catch (NumberFormatException e) {
                // Anunt malformat de pe retea: il ignoram, urmatorul vine peste 2 secunde
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.err.println("[" + selfId + "] ascultare esuata: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void close() {
        socket.close();
    }
}
