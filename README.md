# 2D Boss Battle (Android)

Applicazione Android in Kotlin per un gioco 2D incentrato su una Boss Battle.

## Funzionalità

- **Menù dei livelli**: schermata iniziale di selezione livello con il **Livello 1** (`GUARDIANO D'OMBRA`), strutturata tramite `LevelCatalog` per aggiungere facilmente nuovi boss e livelli in futuro.
- **Controlli touch**: trascina il dito sullo schermo per spostare il giocatore lungo l'asse orizzontale e schivare i colpi del boss. Il giocatore spara automaticamente verso l'alto durante il combattimento.
- **Boss & Barra della Vita**:
  - Barra della vita del boss in alto con nome, valore HP (`100 / 100 HP`) e animazione fluida del danno subito.
  - Il boss pattuglia l'arena e lancia proiettili mirati verso il giocatore.
  - Sotto il 50% di vita entra in **Fase 2**, aumentando velocità e sparando una raffica a 3 proiettili.
- **Stati di gioco**: schermata pronta, combattimento, vittoria e sconfitta con riavvio rapido o ritorno al **Menù Livelli**.

## Struttura del progetto

- `app/src/main/java/com/endermark/bossbattle/model/LevelConfig.kt`: definizione dei livelli (`BossLevel`) e catalogo (`LevelCatalog`).
- `app/src/main/java/com/endermark/bossbattle/model/GameModels.kt`: modelli di stato (`Player`, `Boss`, `Projectile`, `HitParticle`, `GameState`).
- `app/src/main/java/com/endermark/bossbattle/engine/BossBattleEngine.kt`: logica di gioco, fisica 2D, collisioni e gestione HP.
- `app/src/main/java/com/endermark/bossbattle/ui/LevelSelectActivity.kt`: menù di selezione livelli (schermata iniziale).
- `app/src/main/java/com/endermark/bossbattle/ui/GameRenderer.kt`: rendering 2D su `Canvas`, barra della vita del boss e overlay.
- `app/src/main/java/com/endermark/bossbattle/ui/GameSurfaceView.kt`: `SurfaceView` con game loop a 60 FPS e input touch.
- `app/src/main/java/com/endermark/bossbattle/ui/MainActivity.kt`: activity fullscreen della battaglia.

## Build e Test

```bash
./gradlew testDebugUnitTest assembleDebug
```