# Sistem de Fișiere Distribuit 

## Compilare
Proiectul folosește structura standard. Pentru a compila clasele în folderul `target/classes`:
```bash
# Calea manuală:
mkdir -p target/classes
javac --release 17 -d target/classes src/ro/ubm/sd/common/*.java src/ro/ubm/sd/node/*.java src/ro/ubm/sd/client/*.java
```

## Pornire Procese

**1. Pornirea Coordonatorului (Nod 1)**
```bash
java -cp target/classes ro.ubm.sd.node.StorageNode --id n1 --port 7001 --dir data/n1 --coordinator
```

**2. Pornirea Nodurilor de Stocare (Nod 2, Nod 3)**
```bash
java -cp target/classes ro.ubm.sd.node.StorageNode --id n2 --port 7002 --dir data/n2
java -cp target/classes ro.ubm.sd.node.StorageNode --id n3 --port 7003 --dir data/n3
```

**3. Pornirea Clientului**
```bash
java -cp target/classes ro.ubm.sd.client.Client
```

Comenzi în client: `nodes`, `put <nod> <fișier>`, `get <nod> <nume> <destinație>`, `exit`. Numele fișierelor nu pot conține spații.

Grupul multicast este `239.255.0.k`, port 4446, unde `k` e numărul echipei (constanta `GROUP` din `Protocol.java`).

## Detectarea defectelor (Etapa 3)
`HELLO`-ul trimis la 2 s este și semnul de viață. Fiecare nod verifică la 500 ms de când nu a mai auzit de ceilalți:
fără `HELLO` de peste 3 s → `SUSPECT`, de peste 4 s → `CAZUT`, un `HELLO` nou → `ACTIV`. Un nod oprit forțat
este marcat `CAZUT` în max ~4,5 s. Fiecare schimbare de stare apare în log cu oră exactă:
```
10:43:55.890 [n1] nod n3@192.168.1.10:7003: CAZUT (ultimul HELLO acum 4485 ms)
```
Test: oprește un nod cu `Ctrl+C` (sau `taskkill /F /PID <pid>` / `kill -9 <pid>`) și urmărește logurile celorlalte.

## Verificare
```bash
javac --release 17 -d target/classes src/ro/ubm/sd/common/*.java test/ro/ubm/sd/common/*.java
java -cp target/classes ro.ubm.sd.common.ProtocolCheck
java -cp target/classes ro.ubm.sd.common.DiscoveryCheck
```
