# 2D Boss Battle (Android)

Applicazione Android in Kotlin per un gioco 2D incentrato su Boss Battle, con **musica di sottofondo chiptune** ed **effetti sonori** sintetizzati (`SoundManager`).

## Livelli disponibili

- **Menù dei livelli**: schermata iniziale di selezione livello (`LevelSelectActivity`).
- **Livello 1 — `GUARDIANO D'OMBRA`**: Navicella 2D, schiva e distruggi il custode.
- **Livello 2 — `COLOSSO DI FERRO`**: Platformer in mischia con salto, doppio salto e attacco di spada.
- **Livello 3 — `CHAN` (Hanoi)**: Spada e scudo (`SCUDO`) contro la guerriera delle lanterne al tramonto su Hanoi.
- **Livello 4 — `LOLLO ZURICH`**: Senza scudo a Zurigo, contro fumetti di lamentele in italiano.
- **Livello 5 — `IVANONE`**: Fisico scolpito, droni, flash fotografici e sparizioni improvvise.
- **Livello 6 — `ALESSIO DELLE TASSE`**: Moduli fiscali di Zurigo, superiorità morale e mossa speciale con lo **strano dente**.
- **Livello 7 — `LOUISE`**: Ragazza francese dagli occhi azzurri a Parigi: ti seduce con cuori e frasi francesi, ma ti evita appena ti avvicini.
- **Livello 8 — `DAISY`**: Ragazza calabrese di **UBS**, urla e si lamenta perché non la inviti mai alle hike.
- **Livello 9 — `ANDREA DATTERO`**: Magrissimo con felpa **MOSCHINO**, tenta il grappling ravvicinato, lancia ciotole da **200g di riso e pollo** e si vanta di essere un 10x software engineer (tra crash e bug).
- **Livello 10 — `MARCO DI GOOGLE`**: Sfreccia in **Porsche bianca** davanti al Google Campus, cerca di fare dating ripetendo `"SONO PIENO DI MILIONI!"` e scatta foto con flash fotografici dal finestrino.
- **Livello 11 — `CARAMEL` (Hanoi Spa)**: Boss fight avanzata a **3 fasi** contro la massaggiatrice di Hanoi, con **armatura da spezzare** (stordimento ad ogni rottura), **armi raccoglibili** (`MARTELLO` spacca-armatura, `KATANA` rapida, `LANCIA` con onda d'energia) e **power-up** (`+1 VITA`, `VELOCITÀ`, `FURIA x2`).
- **Livelli 12–19 — Modalità `ARCADE` (stile Livello 1 / Arkanoid)**: ripropongono tutti i boss da **Lollo Zurich** fino a **Caramel** in versione navicella dall'alto verso il basso, ciascuno con i propri attacchi a tema (lamentele, droni/flash/sparizioni, tasse & strano dente, cuori & schivate, urla UBS, riso & pollo, Porsche & milioni, pietre calde a 3 fasi con armatura).
- **Livello 20 • FINALE — `OMEGA ARCHITECT`**: Gran finale a **3 fasi con 3 scenari dinamici** (*Cyber Citadel* → *Vulcano Cosmico* → *Singolarità Omega*), **piattaforme mobili** su cui saltare, **raggi laser telegrafati** orizzontali e verticali con effetti speciali, **armatura da spezzare** (100 / 70 / 60) con stordimento, **armi raccoglibili** e **power-up**.
- **Livello 21 • SPAZIO 360° — `DREADNOUGHT STELLARE`**: Volo libero su tutto lo schermo con **pad analogico virtuale** (movimento 360° + rotazione) e pulsante **FUOCO**, contro una grande astronave boss che spara salve al plasma e genera **piccole navicelle scagnozze** abbattibili (spawn bilanciato -50%).
- **Livello 22 • PUNTI DEBOLI — `CORAZZATA AEGIS`**: Volo libero 360° senza scagnozzi. La corazzata è **invincibile** finché non distruggi i suoi **3 punti deboli** (1 dietro e 2 ai lati), dopodiché perde lo scudo e può essere danneggiata.
- **Livello 23 • FORTEZZA ORBITALE — `BASTIONE ORBITALE`**: Volo libero 360° circondato da **8 torrette difensive** disposte tutt'attorno all'arena che sparano verso il giocatore. Una volta distrutte tutte le torrette, si sblocca la **super-torretta boss in alto** con la vera barra della vita e potenti salve al plasma.
- **Livello 24 • LOLLO GIGANTE — `LOLLO ZURICH GIGANTE`**: Enorme testa pelata in volo libero 360° che attacca lanciando **computer, divani e televisioni**. Lollo è invincibile finché non respingi contro di lui sia una **TV** sia un **Divano**: entra allora in stato **INGOLFATO** e diventa vulnerabile ai colpi!
- **Livello 25 • LOLLO INFURIATO — `LOLLO GIGANTE INFURIATO`**: Versione infuocata e più difficile di Lollo Gigante con fiamme sulla pelata, vapore dalle orecchie, **raggi laser telegrafati** (e a croce sotto il 50% HP), stato **INGOLFATO dimezzato** (`3.25s`) ed **esplosione radiale di rabbia** appena si riprende!
- **Livello 26 • TECH LEAD — `LOLLO TECH LEAD`**: Lollo in camicia e **cravatta rossa** in **3 fasi**, spawna **programmatori kamikaze** che ti inseguono per esploderti addosso; in **Fase 3** spara **nuvole di scorregge tossiche** e subisce **solo la metà del danno**!
- **Controlli Audio**: barra volume e toggle separati per **Musica** ed **Effetti (SFX)** disponibili sia nel menù livelli sia in battaglia.

## Build e Test

```bash
./gradlew testDebugUnitTest assembleDebug
```