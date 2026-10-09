<div align="center">

[Русский](README.md) · [English](README.en.md) · [Español](README.es.md) · [Português](README.pt.md) · **Deutsch** · [Français](README.fr.md) · [Italiano](README.it.md) · [Türkçe](README.tr.md) · [Українська](README.uk.md) · [Polski](README.pl.md)

<br>

<img src="assets/readme/hero-de.svg" width="100%" alt="Budila — Pünktlich aufwachen. Leicht aufstehen.">

<a href="https://github.com/sailxx/Budila/releases/latest/download/Budila.apk"><img src="assets/readme/cta-de.svg" height="44" alt="APK herunterladen"></a>

[![Build](https://github.com/sailxx/Budila/actions/workflows/build.yml/badge.svg)](https://github.com/sailxx/Budila/actions/workflows/build.yml)

</div>

<br>

<img src="assets/readme/screens-de.svg" width="100%" alt="Budila-Bildschirme: Tags und Farben, eigenes Design, Weckruf, dunkles Design">

<br>

<img src="assets/readme/features-de.svg" width="100%" alt="Was Budila kann">

<details>
<summary>Mehr zu den Funktionen</summary>

### ⏰ Startbildschirm

Oben stehen groß die aktuelle Uhrzeit und das Datum — „Dienstag, 6. Oktober“. Darunter die Karte **„Nächster Wecker“**: Tag, Uhrzeit und wie viel Zeit noch bleibt. Wecker lassen sich in einer von 10 Farben einfärben und mit **Tags** markieren — eine Filterleiste („Alle · 5“, „Arbeit · 2“) zeigt nur die gewünschten.

### ✏️ Editor

Die Uhrzeit stellst du auf dem **Material-3-Zifferblatt** oder per Tastatur ein. Name, Wiederholung an Wochentagen, Tags (vorgegebene oder eigene), Kartenfarbe, Vibration und **sanftes Wecken**. Ein gelöschter Wecker lässt sich mit einem Tipp zurückholen.

### 🌿 Sanftes Wecken

Der Wecker beginnt fast lautlos, die Lautstärke steigt etwa eine Minute lang sanft an, die Vibration setzt nach 30 Sekunden ein. Der Modus wird **für jeden Wecker einzeln** ein- und ausgeschaltet; in den Einstellungen legst du fest, wie er bei neuen Weckern ist.

### 🔔 Weckruf

Der Weckbildschirm schaltet das Display selbst ein und öffnet sich **über dem Sperrbildschirm**. **„Ausschalten“** und **„Schlummern“** (5–20 Minuten) gibt es auf dem Bildschirm und in der Benachrichtigung. Reagiert niemand, verstummt der Wecker nach 5–30 Minuten von selbst.

### 🧮 Design „Instrument“

Budilas eigenes Design im Stil von [Okto](https://github.com/sailxx/Okto): monochromes Gehäuse, echte Tasten und vertiefte Displays wie bei einem Ingenieursrechner. Die Ziffern sind JetBrains Mono mit „unbeleuchteten“ Achten und einem Selbsttest beim Start, die Uhrzeit wird auf einer Tastatur eingetippt, und ein klingelnder Wecker leuchtet rot. 9 Designs — Klassisch (hell, dunkel, OLED), Papier, Minze, Sakura, Mitternacht, Ozean, Nord, Karmin, Bernstein. Standardmäßig aktiv; in den Einstellungen lässt sich Material 3 zurückholen.

### 🎨 Designs

8 Designs: **Material You** aus dem Hintergrundbild, Indigo, Ozean, Wald, Abendrot, Sakura, Graphit und **Eigenes** — wähle den Farbton auf dem Farbkreis, die Sättigung und den Stil (Ruhig, Lebhaft, Ausdrucksstark, Regenbogen…). Hell, dunkel oder wie im System, dazu ein rein schwarzer Hintergrund für AMOLED.

### 🌍 10 Sprachen

Русский, English, Українська, Español, Português, Deutsch, Français, Italiano, Türkçe, Polski. Ab Android 13 lässt sich die Sprache von Budila unabhängig vom System wählen.

### 🛡 Zuverlässigkeit

Wecker werden über den System-`AlarmClock` gestellt und klingeln pünktlich, auch wenn das Handy schläft. Nach einem Neustart oder einer Änderung von Uhrzeit oder Zeitzone stellt Budila sie neu.

</details>

<br>

<img src="assets/readme/quality-de.svg" width="100%" alt="Qualität in Zahlen">

<br>

<img src="assets/readme/design-de.svg" width="100%" alt="Design-Code: Material 3 und Material You">

## Installation

1. Lade [**Budila.apk**](https://github.com/sailxx/Budila/releases/latest/download/Budila.apk) aufs Handy.
2. Öffne die Datei und erlaube die Installation aus dieser Quelle.
3. Erlaube beim ersten Start Benachrichtigungen — ohne sie erscheint der Weckbildschirm nicht.

Benötigt Android 8.0 oder neuer. Kein Google Play und keine Konten nötig.

Budila gibt es auch im [Komi Store](https://github.com/komi-store/komi-store), einem App-Store auf Basis von GitHub-Releases: Suche nach „Budila“, und der Komi Store bietet Updates selbst an.

<details>
<summary>Aus dem Quellcode bauen</summary>

Du brauchst JDK 17+ und das Android SDK (Plattform 36).

```bash
git clone https://github.com/sailxx/Budila.git
cd Budila
./gradlew assembleRelease
```

Releases werden auf GitHub automatisch gebaut: Erhöhe `versionCode`/`versionName` in `app/build.gradle.kts` und pushe einen Tag (`git tag v2.5 && git push origin v2.5`) — Actions baut, signiert und veröffentlicht `Budila.apk`. Für einen lokal signierten Build kopiere `keystore.properties.example` nach `keystore.properties` und trage deinen Schlüssel ein; ohne ihn entsteht ein unsigniertes APK. Die Screenshots (auch auf Englisch, Deutsch und Spanisch) zeichnet `./gradlew testDebugUnitTest` neu (Robolectric, ohne Handy), sie landen in `screenshots/`. Die README-Bilder in allen Sprachen erzeugt `node tools/readme/build.mjs`, ihre Texte stehen in `tools/readme/strings.json`.

**Stack:** Kotlin · Jetpack Compose · Material 3 · [MaterialKolor](https://github.com/jordond/MaterialKolor) · AlarmManager · Foreground Service.

</details>

## Datenschutz und Sicherheit

Budila hat **keinen Internetzugriff**: Im Manifest gibt es keine `INTERNET`-Berechtigung. Wecker werden nur auf dem Handy gespeichert und landen nicht in Cloud-Backups. Keine Werbung, keine Analyse, keine Tracker. Details und wie du eine Schwachstelle meldest, stehen in [SECURITY.md](SECURITY.md).

<br>

<div align="center"><sub>© 2026 sailxx. Alle Rechte vorbehalten.</sub></div>
