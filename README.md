# 2D Boss Battle (Android + Web — Kotlin Multiplatform)

Gioco 2D incentrato su **Boss Battle** in **Kotlin Multiplatform**:
- `:shared` — Motore di gioco (`BossBattleEngine`), modelli (`GameModels`) e catalogo livelli (`LevelCatalog`) condivisi al 100% tra Android e Web.
- `:app` — App nativa Android (`Canvas` + `AudioTrack` chiptune).
- `:web` — WebApp **Kotlin/JS + HTML5 Canvas + Web Audio API** giocabile da browser su iPhone, Android, Mac e PC, pronta per il deploy su **Cloudflare Pages**.

## Livelli disponibili (22 livelli)

1. **Livello 1 — `GUARDIANO D'OMBRA`**: Navicella 2D, schiva e distruggi il custode.
2. **Livello 2 — `COLOSSO DI FERRO`**: Platformer in mischia con salto, doppio salto e attacco di spada.
3. **Livello 3 — `CHAN` (Hanoi)**: Spada e scudo (`SCUDO`) contro la guerriera delle lanterne al tramonto su Hanoi.
4. **Livello 4 — `LOLLO ZURICH`**: Senza scudo a Zurigo, contro fumetti di lamentele in italiano.
5. **Livello 5 — `IVANONE`**: Fisico scolpito, droni, flash fotografici e sparizioni improvvise.
6. **Livello 6 — `LOUISE`**: Ragazza francese dagli occhi azzurri a Parigi: ti seduce con cuori, ma ti evita appena ti avvicini.
7. **Livello 7 — `MARCO DI GOOGLE`**: Porsche bianca, `"SONO PIENO DI MILIONI!"` e flash fotografici.
8. **Livello 8 — `CARAMEL` (Hanoi Spa)**: 3 fasi, armatura da spezzare, armi raccoglibili (`MARTELLO`, `KATANA`, `LANCIA`) e power-up.
9. **Livelli 9–13 — Modalità `ARCADE`**: Lollo Zurich, Ivanone, Louise, Marco di Google e Caramel in versione navicella stile Livello 1.
10. **Livello 14 • FINALE — `OMEGA ARCHITECT`**: 3 fasi e 3 scenari dinamici, piattaforme mobili, raggi laser telegrafati, armatura e power-up.
11. **Livello 15 • SPAZIO 360° — `DREADNOUGHT STELLARE`**: Volo libero 360° con pad virtuale + scagnozzi.
12. **Livello 16 • PUNTI DEBOLI — `CORAZZATA AEGIS`**: Volo libero 360°, invincibile finché non distruggi i 3 punti deboli (dietro e ai lati).
13. **Livello 17 • FORTEZZA ORBITALE — `BASTIONE ORBITALE`**: 8 torrette perimetrali che sbloccano la super-torretta boss in alto.
14. **Livello 18 • LOLLO GIGANTE — `LOLLO ZURICH GIGANTE`**: Enorme testa pelata; respingi TV e Divano per mandarlo in stato **INGOLFATO**.
15. **Livello 19 • LOLLO INFURIATO — `LOLLO GIGANTE INFURIATO`**: Ingolfato per metà tempo (`3.25s`), raggi laser ed esplosioni di rabbia.
16. **Livello 20 • TECH LEAD — `LOLLO TECH LEAD`**: Cravatta rossa, 3 fasi, programmatori kamikaze, scorregge e -50% danni in Fase 3.
17. **Livello 21 • BOARD OF DIRECTORS — `LOLLO ZURICH + IVANONE`**: Doppio boss simultaneo con due barre HP e modalità Super-CEO.
18. **Livello 22 • BOARD PLATFORM — `LOLLO ZURICH + IVANONE` (Platform + Pistola)**: Omino platform con pistola (`SPARA`), piattaforme mobili e 3 fasi.

## Build e Deploy (Android + Web / GitHub Pages)

- **Web App live su GitHub Pages**: https://endermark.github.io/2d_boss/ (aggiornata automaticamente ad ogni push su `main` tramite [`.github/workflows/web-deploy.yml`](.github/workflows/web-deploy.yml)).

```bash
# Test e APK Android
./gradlew :app:testDebugUnitTest :app:assembleDebug

# Bundle Web Kotlin/JS (output in web/build/dist/js/productionExecutable/)
./gradlew :web:jsBrowserDistribution
```