# Sistem de Fișiere Distribuit 

## Compilare
Proiectul folosește structura standard. Pentru a compila clasele în folderul `target/classes`:
```bash
# Calea manuală:
mkdir -p target/classes
javac -d target/classes src/ro/ubm/sd/node/StorageNode.java src/ro/ubm/sd/client/Client.java
# Sau dacă folosești Maven:
mvn -q package
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
java -cp target/classes ro.ubm.sd.client.Client --coordinator localhost:7001
```
