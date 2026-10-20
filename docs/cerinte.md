# Proiect Sisteme Distribuite – Etapa 1

## 1. Cerințe funcționale: Operațiile

| Operație | Comanda | Pașii prin sistem | Rezultat | Erori tratate |
| :--- | :--- | :--- | :--- | :--- |
| **Încărcare** | `put raport.pdf` | 1. clientul cere coordonatorului două noduri active, distincte; 2. trimite fișierul primului nod; 3. primul nod îl copiază pe al doilea; 4. după ambele confirmări, coordonatorul înregistrează metadatele | ID-ul fișierului și cele două noduri | mai puțin de 2 noduri active: refuz, nimic stocat · fișier local inexistent · o copie eșuată: se alege alt nod |
| **Descărcare** | `get raport.pdf ./out/` | 1. clientul cere coordonatorului locațiile copiilor; 2. descarcă de la primul nod; 3. verifică checksum-ul | fișierul salvat local, cu checksum verificat | fișier necunoscut · primul nod nu răspunde sau checksum greșit: încearcă a doua copie · nicio copie accesibilă: eroare explicită |
| **Listare** | `ls` | 1. clientul cere coordonatorului lista tuturor fișierelor; 2. coordonatorul trimite metadatele (nume, dimensiune, noduri). | afișarea listei fișierelor pe ecran | niciun fișier stocat: afișează listă goală · coordonatorul nu răspunde: timeout. |
| **Ștergere** | `rm raport.pdf` | 1. clientul cere coordonatorului ștergerea; 2. coordonatorul trimite comanda de ștergere fizică către nodurile care dețin copiile; 3. coordonatorul șterge metadatele. | confirmarea ștergerii | fișier necunoscut: eroare. *Dacă un nod e căzut:* coordonatorul șterge metadatele și ignoră nodul căzut (când nodul va reporni, fișierul va deveni "orfelin", curățat eventual în etapele viitoare). |
| **Stare sistem** | `nodes` | 1. clientul cere coordonatorului lista nodurilor din rețea și starea lor. | listă cu nodurile, IP:port, starea (ACTIV/CAZUT) și marcajul de coordonator | coordonatorul este căzut: eroare de conexiune. |
| **Adăugare nod** | N/A (invizibil ptr. utilizator) | 1. nodul nou pornește; 2. trimite mesaj de descoperire (ex: multicast sau direct la coordonator); 3. coordonatorul îl adaugă în tabela de noduri. | nodul nou devine disponibil pentru viitoarele `put`. | port deja utilizat: eroare la pornire. |

**Decizii suplimentare de design:**
*   **Identificare fișiere:** Utilizatorul interacționează cu fișierele prin **nume**, dar intern sistemul folosește **UUID**. 
*   **Coliziune nume:** La încărcarea unui nume existent, sistemul va refuza operația cu eroarea „Fișierul există deja”, obligând utilizatorul să șteargă varianta veche sau să o redenumească local.
*   **Ipoteze de lucru:** Sistemul va rula într-o rețea locală sigură, erorile sunt de tip *crash-stop* (nu bizantine - nodurile nu mint), și se presupune că cel mult un nod de stocare pică la un moment dat.

## 2. Modelul de date (În memoria Coordonatorului)

**Metadatele unui fișier:**
| Câmp | Tip Java | Exemplu | Stabilit de |
| :--- | :--- | :--- | :--- |
| identificator | UUID | 3f2a9c1e-5b7d4e8a-9c21-7d4e5f6a8b90 | coordonator, la încărcare |
| nume | String | raport.pdf | utilizator |
| dimensiune | long (octeți) | 482133 | client |
| checksum | String (SHA-256, hex) | 9f86d081... | client, verificat de fiecare nod |
| locațiile copiilor | List\<String> | [n2, n3] | coordonator, după confirmări |

**Lista nodurilor:**
| Câmp | Tip Java | Exemplu |
| :--- | :--- | :--- |
| ID nod | String | n2 |
| adresă și port | String, int | 192.168.37.102, 7002 |
| stare | enum | ACTIV, SUSPECT, CAZUT |
| ultimul semn de viață | long (ms) | System.currentTimeMillis() |

*Notă de arhitectură:* Pentru a preveni pierderea metadatelor (Etapa 6), la schimbarea coordonatorului, fiecare nod va trimite un mesaj cu fișierele pe care le deține fizic, permițând reconstrucția tabelei.

## 3. Cerințe nefuncționale și obiective măsurabile

| Cerință | Obiectiv măsurabil | Cum se măsoară | Etapa |
| :--- | :--- | :--- | :--- |
| Integritate | 0 fișiere cu checksum diferit din 100 descărcări | script care încarcă și descarcă 100 de fișiere | 2 |
| Descoperire | un nod nou apare în lista celorlalți în max 3s | diferența de timp din loguri | 2 |
| Detectare defecte | un nod oprit forțat (`kill -9`) este marcat CĂZUT în max 5s | loguri cu timestamp pe fiecare nod | 3 |
| Scalabilitate | debitul de încărcare crește cu 3 noduri față de 1 nod | cronometrare script 10 fișiere de 100 MB | 4 |
| Coordonare | noul coordonator este ales în max 10s după o cădere | loguri Bully algorithm | 5 |
| Replicare | fiecare fișier are exact 2 copii pe noduri distincte | comanda `ls` și verificare manuală directoare `data/` | 6 |
| Disponibilitate | după oprirea oricărui nod, 100% din fișiere rămân descărcabile | script de verificare automată prin `get` | 7 |

## 4. Scenariul Demonstrației (Test de acceptanță)

| Pas | Acțiunea operatorului | Ce trebuie să se vadă | Funcționează din |
| :--- | :--- | :--- | :--- |
| 1 | pornește nodurile n1, n2, n3 în terminale | fiecare nod îi descoperă pe ceilalți; n1 este coordonator | 2 |
| 2 | `put raport.pdf` din client | ID-ul fișierului și două noduri distincte care îl dețin | 2 |
| 3 | `get raport.pdf` | fișierul descărcat, cu checksum identic | 2 |
| 4 | pornește nodul n4, fără a opri ceva | n4 apare în `nodes`; viitoarele încărcări îl folosesc | 4 |
| 5 | `kill -9` pe un nod ce deține raport.pdf | nodul este marcat căzut; `get raport.pdf` folosește a 2-a copie | 3 |
| 6 | `kill -9` pe coordonator | nodurile rămase aleg un nou coordonator | 5 |
| 7 | `ls` și `get raport.pdf` după realegere | metadatele sunt reconstruite; fișierul rămâne accesibil | 6 |

## 5. Schița Protocolului

| Mesaj | De la → către | Conținut | Răspuns |
| :--- | :--- | :--- | :--- |
| **ALLOCATE** | client → coordonator | nume, dimensiune | `NODES` (ID fișier + 2 locații) sau `ERROR` |
| **PUT** | client → nod | ID, nume, dim., checksum, nod_copie + octeți | `STORED` sau `ERROR` |
| **REPLICATE**| nod → nod | la fel ca PUT, fără nodul următor | `STORED` sau `ERROR` |
| **COMMIT** | nod → coordonator | ID fișier, nodurile care îl dețin | `OK` |
| **LOCATE** | client → coordonator | nume sau ID | `LOCATIONS` (metadate) sau `NOT_FOUND` |
| **GET** | client → nod | ID fișier | antet(dim) + octeți, sau `NOT_FOUND` |
| **LIST** | client → coordonator | cerere lista | array cu metadatele tuturor fișierelor |
| **REMOVE** | client → coordonator | nume fisier | `OK` sau `NOT_FOUND` |
| **DELETE_FILE**| coordonator → nod | ID fisier pentru stergere fizica | `DELETED` |
| **GET_NODES**| client → coordonator | cerere lista noduri | array cu statusul nodurilor |
