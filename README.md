# 2D Boss Battle (Android)

Applicazione Android in Kotlin per un gioco 2D incentrato su una Boss Battle.

## Meccaniche iniziali

- **Controlli touch**: trascina il dito sullo schermo per spostare il giocatore lungo l'asse orizzontale e schivare i colpi del boss. Il giocatore spara automaticamente verso l'alto durante il combattimento.
- **Boss & Barra della Vita**:
  - Barra della vita del boss in alto con nome (`GUARDIANO D'OMBRA`), valore HP (`100 / 100 HP`) e animazione fluida del danno subito.
  - Il boss pattuglia l'arena e lancia proiettili mirati verso il giocatore.
  - Sotto il 50% di vita entra in **Fase 2**, aumentando velocità e sparando una raffica a 3 proiettili.
- **Stati di gioco**: schermata iniziale, combattimento, vittoria e sconfitta con riavvio rapido al tocco.

## Struttura del progetto

- `app/src/main/java/com/endermark/bossbattle/model/GameModels.kt`: modelli di stato (`Player`, `Boss`, `Projectile`, `HitParticle`, `GameState`).
- `app/src/main/java/com/endermark/bossbattle/engine/BossBattleEngine.kt`: logica di gioco, fisica 2D, collisioni e gestione HP.
- `app/src/main/java/com/endermark/bossbattle/ui/GameRenderer.kt`: rendering 2D su `Canvas`, inclusa la barra della vita del boss.
- `app/src/main/java/com/endermark/bossbattle/ui/GameSurfaceView.kt`: `SurfaceView` con game loop a 60 FPS e input touch.
- `app/src/main/java/com/endermark/bossbattle/ui/MainActivity.kt`: activity fullscreen in modalità portrait.

## Build e Test

```bash
./gradlew testDebugUnitTest assembleDebug
```