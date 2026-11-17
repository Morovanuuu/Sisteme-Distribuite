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
    public enum State { ACTIV, SUSPECT, CAZUT }

    public record Member(String id, InetAddress address, int port, long lastSeen, State state) {
        @Override
        public String toString() {
            return id + "@" + address.getHostAddress() + ":" + port;
        }
    }

    static final long HELLO_MS = 2000;
    // SUSPECT dupa un HELLO ratat, CAZUT dupa doua; cu verificare la 500 ms, un nod oprit e marcat in max ~4.5 s (cerinta: 5 s)
    static final long SUSPECT_MS = 3000;
    static final long DEAD_MS = 4000;
    static final long CHECK_MS = 500;

    private final String selfId;
    private final Consumer<Member> onChange;
    private final Map<String, Member> members = new ConcurrentHashMap<>();
    private final InetSocketAddress group;
    private final MulticastSocket socket;

    // onChange: apelat cand un nod apare sau isi schimba starea
    public Discovery(String selfId, Consumer<Member> onChange) throws IOException {
        this.selfId = selfId;
        this.onChange = onChange;
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
        }, 0, HELLO_MS, TimeUnit.MILLISECONDS);
    }

    // Detectarea defectelor: HELLO-ul periodic e si semnul de viata
    public void startMonitoring() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::check, CHECK_MS, CHECK_MS, TimeUnit.MILLISECONDS);
    }

    static State stateFor(long silentMs) {
        return silentMs > DEAD_MS ? State.CAZUT : silentMs > SUSPECT_MS ? State.SUSPECT : State.ACTIV;
    }

    private synchronized void check() {
        long now = System.currentTimeMillis();
        for (Member m : members.values()) {
            State s = stateFor(now - m.lastSeen());
            if (s != m.state()) {
                update(new Member(m.id(), m.address(), m.port(), m.lastSeen(), s));
            }
        }
    }

    // synchronized: ascultatorul si verificatorul nu au voie sa-si suprascrie unul altuia starea
    private synchronized void update(Member m) {
        Member old = members.put(m.id(), m);
        if (old == null || old.state() != m.state()) {
            onChange.accept(m);
        }
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
                update(new Member(p[1], packet.getAddress(), Integer.parseInt(p[2]), System.currentTimeMillis(), State.ACTIV));
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
