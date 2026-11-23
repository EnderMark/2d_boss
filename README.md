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
- **Controlli Audio**: barra volume e toggle separati per **Musica** ed **Effetti (SFX)** disponibili sia nel menù livelli sia in battaglia.

## Build e Test

```bash
./gradlew testDebugUnitTest assembleDebug
```