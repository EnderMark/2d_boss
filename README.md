# 2D Boss Battle (Android)

Applicazione Android in Kotlin per un gioco 2D incentrato su Boss Battle.

## Livelli disponibili

- **Menù dei livelli**: schermata iniziale di selezione livello (`LevelSelectActivity`).
- **Livello 1 — `GUARDIANO D'OMBRA` (Navicella)**:
  - Trascina il dito per spostarti orizzontalmente e schivare i colpi mentre spari automaticamente.
  - Sotto il 50% di HP entra in **Fase 2** con raffiche a 3 proiettili.
- **Livello 2 — `COLOSSO DI FERRO` (Platformer in mischia)**:
  - Controlli multi-touch dedicati: **◀ / ▶** per muoversi, **SALTA** (supporta **doppio salto**), **COLPO** per attaccare in mischia (nessun proiettile per il giocatore).
  - Il boss alterna **onde d'urto a terra** e **cariche pesanti**, accelerando in **Fase 2**.
- **Livello 3 — `CHAN` (Hanoi • Spada e Scudo)**:
  - Ambientato ad **Hanoi** al tramonto con la silhouette della **Torre della Tartaruga (Tháp Rùa)** sul lago Hoàn Kiếm e lanterne sospese.
  - La boss **Chan** indossa un tradizionale **Áo dài** e **Nón lá**, alternando lanci di lanterne infuocate e scatti rapidi.
  - Il giocatore impugna **spada e scudo**: tieni premuto **SCUDO** per parare le lanterne e fermare gli scatti di Chan senza subire danni, oppure usa salto/doppio salto e contrattacca con **COLPO**.
- **Barra della vita del Boss**: sempre visibile in alto con nome, badge `FASE 2`, contatore HP e scia animata del danno.

## Build e Test

```bash
./gradlew testDebugUnitTest assembleDebug
```