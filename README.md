<div align="center">

<img src="assets/readme/hero.svg" width="100%" alt="Budila — Просыпайся вовремя. Вставай легко.">

<a href="https://github.com/sailxx/Budila/releases/latest/download/Budila.apk"><img src="assets/readme/cta.svg" height="44" alt="Скачать APK"></a>

[![Build](https://github.com/sailxx/Budila/actions/workflows/build.yml/badge.svg)](https://github.com/sailxx/Budila/actions/workflows/build.yml)

</div>

<br>

<img src="assets/readme/screens.svg" width="100%" alt="Экраны Budila: теги и цвета, своя тема, звонок, тёмная тема">

<br>

<img src="assets/readme/features.svg" width="100%" alt="Что умеет Budila">

<details>
<summary><b>Подробнее о возможностях</b></summary>

### ⏰ Главный экран

Вверху крупно текущее время и дата — «Вторник, 6 октября». Ниже карточка **«Следующий будильник»**: день, время и сколько осталось. Будильники можно окрасить в любой из 10 цветов и пометить **тегами** — ряд фильтров («Все · 5», «Работа · 2») показывает только нужные.

### ✏️ Редактор

Время — на **циферблате Material 3** или с клавиатуры. Название, повтор по дням недели, теги (готовые или свои), цвет карточки, вибрация и **спокойное пробуждение**. Удалённый будильник можно вернуть одной кнопкой.

### 🌿 Спокойное пробуждение

Звонок начинается почти беззвучно, громкость плавно растёт около минуты, вибрация включается через 30 секунд. Режим включается и выключается **у каждого будильника отдельно**; в настройках задаётся, каким он будет у новых.

### 🔔 Звонок

Экран звонка сам включает дисплей и открывается **поверх экрана блокировки**. **«Выключить»** и **«Отложить»** (на 5–20 минут) — на экране и в уведомлении. Если никто не ответит, звонок затихнет сам через 5–30 минут.

### 🎨 Темы

8 тем: **Material You** из обоев, Индиго, Океан, Лес, Закат, Сакура, Графит и **своя** — выберите тон на цветовом круге, насыщенность и гамму (Спокойная, Яркая, Выразительная, Радуга…). Светлая, тёмная или как в системе, плюс чистый чёрный фон для AMOLED.

### 🌍 10 языков

Русский, English, Українська, Español, Português, Deutsch, Français, Italiano, Türkçe, Polski. На Android 13+ язык Budila можно выбрать отдельно от системы.

### 🛡 Надёжность

Будильники ставятся через системный `AlarmClock` и звонят точно, даже когда телефон спит. После перезагрузки, смены времени или часового пояса Budila расставит их заново.

</details>

<br>

<img src="assets/readme/quality.svg" width="100%" alt="Качество в цифрах">

<br>

<img src="assets/readme/design.svg" width="100%" alt="Дизайн-код: Material 3 и Material You">

## Установка

1. Скачайте [**Budila.apk**](https://github.com/sailxx/Budila/releases/latest/download/Budila.apk) на телефон.
2. Откройте файл и разрешите установку из этого источника.
3. При первом запуске разрешите уведомления — без них не появится экран звонка.

Нужен Android 8.0 или новее. Google Play и аккаунты не нужны.

Budila есть и в [Komi Store](https://github.com/komi-store/komi-store) — магазине приложений из GitHub-релизов: найдите «Budila» в поиске, и Komi Store будет сам предлагать обновления.

<details>
<summary><b>Собрать из исходников</b></summary>

Нужны JDK 17+ и Android SDK (платформа 36).

```bash
git clone https://github.com/sailxx/Budila.git
cd Budila
./gradlew assembleRelease
```

Для подписанного APK скопируйте `keystore.properties.example` в `keystore.properties` и укажите свой ключ; без него соберётся неподписанный APK. Скриншоты экранов (в том числе на английском, немецком и испанском) перерисовываются командой `./gradlew testDebugUnitTest` (Robolectric, без телефона) и появляются в `screenshots/`.

**Стек:** Kotlin · Jetpack Compose · Material 3 · [MaterialKolor](https://github.com/jordond/MaterialKolor) · AlarmManager · Foreground Service.

</details>

## Приватность и безопасность

У Budila **нет доступа к интернету**: в манифесте нет разрешения `INTERNET`. Будильники хранятся только на телефоне и не попадают в облачные бэкапы. Нет рекламы, аналитики и трекеров. Подробности и как сообщить об уязвимости — в [SECURITY.md](SECURITY.md).

<br>

<div align="center"><sub>© 2026 sailxx. Все права защищены.</sub></div>
