# Sistem de Fișiere Distribuit 

## Compilare
Proiectul folosește structura standard. Pentru a compila clasele în folderul `target/classes`:
```bash
# Calea manuală:
mkdir -p target/classes
javac -d target/classes src/ro/ubm/sd/common/*.java src/ro/ubm/sd/node/*.java src/ro/ubm/sd/client/*.java
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

## Verificare protocol
```bash
javac -d target/classes src/ro/ubm/sd/common/*.java test/ro/ubm/sd/common/*.java
java -cp target/classes ro.ubm.sd.common.ProtocolCheck
```
