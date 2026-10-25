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

Type the routine number straight after the name; it is removed before the prediction is written.

- **Normal reveal** (`Noa` or `Noa1`): Object 3 is forced; whoever picks the phone up gets Object 1, the other person Object 2.
- **Expert reveal** (`Noa2`): no force. The edge that rises first (seen from the magician's seat), plus an optional pause, picks one of six arrangements:

| Lift | Spectator's object | Magician's object | Remaining object |
|---|---|---|---|
| Front | obj1 | obj2 | obj3 |
| Front + pause | obj1 | obj3 | obj2 |
| Back | obj2 | obj1 | obj3 |
| Back + pause | obj3 | obj1 | obj2 |
| Right | obj3 | obj2 | obj1 |
| Left | obj2 | obj3 | obj1 |

The pause is made early in the lift: raise the edge slightly, hold still for the pause length, 0.2–1.0 s (a silent tap confirms it), then finish the pickup.

## Hebrew tester build

`hebrew-version/` holds a separate, fully Hebrew copy of the app for testers (its own package, app name and keyboard, so it installs next to the main app). It is not part of the main release.

## Project layout

- `src/com/freewill/predict/`
  - `PredictionKeyboard.java` — the keyboard (input method) that injects the prediction
  - `PickupDetector.java` — gravity + rotation-vector logic for arming and pickup detection
  - `MainActivity.java` — settings and rehearsal screen
  - `Prefs.java` — settings and prediction building
- `res/`, `assets/fonts/` — icon, hero images, Cinzel font
- `design/` — logo and icon source images
- `build.sh`, `tools/` — Gradle-free build: aapt2 → javac → d8 → align + APK Signature Scheme v2

## Building

`build.sh` expects a tools folder (`TOOLS=...`) containing `aapt2`, `android.jar` (API 29+), `d8.jar` with its dependency jars in `lib/`, plus Python 3 with `cryptography`. It writes `FreeWill.apk`.

Signing uses `signing-key.pem` in the project folder (created automatically on first build). **It is not committed.** Keep your own copy safe: updates only install over an existing install when signed with the same key.

Requires Android 8.0+ and a phone with a gyroscope.
