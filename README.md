# 2D Boss Battle (Android)

Applicazione Android in Kotlin per un gioco 2D incentrato su Boss Battle.

## Livelli disponibili

- **Menù dei livelli**: schermata iniziale di selezione livello (`LevelSelectActivity`).
- **Livello 1 — `GUARDIANO D'OMBRA` (Navicella)**:
  - Trascina il dito per spostarti orizzontalmente e schivare i colpi mentre spari automaticamente.
  - Sotto il 50% di HP entra in **Fase 2** con raffiche a 3 proiettili.
- **Livello 2 — `COLOSSO DI FERRO` (Platformer in mischia)**:
  - Controlli multi-touch dedicati: **◀ / ▶** per muoversi, **SALTA** (supporta **doppio salto**), **COLPO** per attaccare in mischia (nessun proiettile per il giocatore).
  - Il boss alterna **onde d'urto a terra** (da saltare) e **cariche pesanti** lungo l'arena, accelerando in **Fase 2**.
- **Barra della vita del Boss**: sempre visibile in alto con nome, badge `FASE 2`, contatore HP e scia animata del danno.

## Struttura del progetto

- `app/src/main/java/com/endermark/bossbattle/model/LevelConfig.kt`: definizione dei livelli (`BossLevel`, `BattleMode`) e catalogo (`LevelCatalog`).
- `app/src/main/java/com/endermark/bossbattle/model/GameModels.kt`: modelli di stato (`Player`, `Boss`, `Projectile`, `HitParticle`, `GameState`).
- `app/src/main/java/com/endermark/bossbattle/engine/BossBattleEngine.kt`: logica di gioco, fisica 2D, doppio salto, combattimento in mischia e IA dei boss.
- `app/src/main/java/com/endermark/bossbattle/ui/LevelSelectActivity.kt`: menù di selezione livelli (schermata iniziale).
- `app/src/main/java/com/endermark/bossbattle/ui/GameRenderer.kt`: rendering 2D su `Canvas`, barra della vita del boss e controlli touch.
- `app/src/main/java/com/endermark/bossbattle/ui/GameSurfaceView.kt`: `SurfaceView` con game loop a 60 FPS e input multi-touch.
- `app/src/main/java/com/endermark/bossbattle/ui/MainActivity.kt`: activity fullscreen della battaglia.

## Build e Test

```bash
./gradlew testDebugUnitTest assembleDebug
```