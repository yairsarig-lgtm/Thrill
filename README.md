# Thrill

A prediction keyboard for Android, built for a Free Will–style mentalism routine with a single, unambiguous prediction.

## How it works

1. Before the show, enter your name, the three objects and the prediction text in the Thrill app.
2. Open any app with a text field, switch to the **Thrill** keyboard and type the spectator's name.
3. Lay the phone face down with its **bottom pointing at you**. A short buzz means it's armed.
4. When the phone is picked up, the keyboard detects who picked it up (whoever the screen ends up facing), replaces the name with the full prediction — that person's name first — and switches back to your normal keyboard.

The prophecy text (shared by both reveals) uses these tags:

- `{spectator}` – the name typed on the keyboard
- `{magician}` – the magician's name
- `{spectator's object}` – the object the spectator ends up holding
- `{magician's object}` – the object the magician ends up holding
- `{remaining object}` – the object left over (always Object 3 in the normal reveal)

Old `{first}`/`{obj1}`-style text is converted automatically.

## Routines

The home page lists every routine; each has its own page with its own players, prophecy,
reveal diagram, vanish settings and rehearsal. Type the routine number straight after the
name (it is removed before the prediction is written). No number, or a number with no
routine behind it, runs routine I.

| # | Routine | Outcomes | Pickups |
|---|---|---|---|
| I | The Normal Reveal (`Noa`, `Noa1`) | 2 | who picks the phone up |
| II | The Expert Reveal (`Noa2`) | 6 | front, front‖, back, back‖, right, left |
| III | The Eight (`Noa3`) | 8 | every edge, with and without a pause; prophecy or mental force |
| IV | Die Roll (`Noa4`) | 6 | 1–2 front, 3–4 back, 5 left, 6 right |
| V | ESP Symbols (`Noa5`) | 5 | by number of lines, clockwise: front, right, back, left, front‖ |
| VI | Coin Flips (`Noa6`) | 2 / 4 / 8 | 1st flip H = front/back, T = side; 2nd H = front/left, T = back/right; 3rd T = pause |
| VII+ | Build It Yourself (`Noa7`, `Noa8`, …) | 2–8 | you assign each outcome to a pickup |

‖ = lift slightly, hold still until the silent tap (0.2–1.0 s), then finish the pickup.
Pickups are named as seen from the magician's seat, phone face down with its bottom toward the magician.

Settings are stored per routine (`r{n}_*` keys in `Prefs`); settings from v0.15 and earlier
carry over automatically. Deleting a custom routine asks whether to keep the numbers or
close the gaps.

## Hebrew tester build

`hebrew-version/` holds a separate, fully Hebrew copy of the app for testers (its own package, app name and keyboard, so it installs next to the main app). It is not part of the main release.

## Project layout

- `src/com/freewill/predict/`
  - `PredictionKeyboard.java` — the keyboard (input method) that injects the prediction
  - `PickupDetector.java` — gravity + rotation-vector logic for arming and pickup detection
  - `HomeActivity.java` — home: keyboard status, routine deck, rehearsal, credits
  - `RoutineActivity.java` — one routine's page (and the Build It Yourself editor)
  - `Routines.java` — routine catalogue and prediction building
  - `RehearsalCard.java`, `BaseActivity.java`, `SymbolView.java` — shared UI
  - `Prefs.java` — per-routine settings storage
- `res/`, `assets/fonts/` — icon, hero images, Cinzel font
- `design/` — logo and icon source images
- `build.sh`, `tools/` — Gradle-free build: aapt2 → javac → d8 → align + APK Signature Scheme v2

## Building

`build.sh [dir] [name]` builds a sideload APK and, with keystore.properties + the upload key present, a signed Play bundle (.aab). Tools: aapt2, SDK android-36.jar, d8 + lib jars, bundletool, Python 3 with cryptography. Output: Thrill.apk and Thrill.aab.

Signing uses `signing-key.pem` in the project folder (created automatically on first build). **It is not committed.** Keep your own copy safe: updates only install over an existing install when signed with the same key.

Requires Android 8.0+ and a phone with a gyroscope.
