# 2D Boss Battle (Android)

Applicazione Android in Kotlin per un gioco 2D incentrato su Boss Battle.

## Livelli disponibili

- **Menù dei livelli**: schermata iniziale di selezione livello (`LevelSelectActivity`).
- **Livello 1 — `GUARDIANO D'OMBRA` (Navicella)**:
  - Trascina il dito per spostarti orizzontalmente e schivare i colpi mentre spari automaticamente.
  - Sotto il 50% di HP entra in **Fase 2** con raffiche a 3 proiettili.
- **Livello 2 — `COLOSSO DI FERRO` (Platformer in mischia)**:
  - Controlli multi-touch dedicati: **◀ / ▶**, **SALTA** (con **doppio salto**) e **COLPO** in mischia.
  - Il boss alterna **onde d'urto a terra** e **cariche pesanti**, accelerando in **Fase 2**.
- **Livello 3 — `CHAN` (Hanoi • Spada e Scudo)**:
  - Ambientato ad **Hanoi** al tramonto con la **Torre della Tartaruga (Tháp Rùa)** e lanterne sospese.
  - La boss **Chan** lancia lanterne luminose e scatta rapidamente; il giocatore usa **spada e scudo** (`SCUDO`) per parare.
- **Livello 4 — `LOLLO ZURICH` (Zurigo • Attacchi verbali)**:
  - Ambientato a **Zurigo** tra Alpi e Grossmünster. **Lollo Zurich** (pelato, in giacca e cravatta) lancia fumetti con **lamentele in italiano** (`"CHE FREDDO!"`, `"TROPPO CARO!"`, `"SEMPRE PIOGGIA!"`, ...). Il giocatore torna **senza scudo**.
- **Livello 5 — `IVANONE` (Droni, Fotocamera e Sparizioni)**:
  - **Ivanone** (fisico scolpito) attacca lanciando **droni** e **flash fotografici**, e **sparisce facilmente** in una nuvola di fumo dopo pochi colpi o dopo uno scatto fotografico per riapparire dall'altro lato dell'arena.

## Build e Test

```bash
./gradlew testDebugUnitTest assembleDebug
```