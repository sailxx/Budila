<div align="center">

<img src="assets/readme/hero.svg" width="100%" alt="Budila — Просыпайся вовремя. Вставай легко.">

<a href="https://github.com/sailxx/Budila/releases/latest/download/Budila.apk"><img src="assets/readme/cta.svg" height="44" alt="Скачать APK"></a>

[![Build](https://github.com/sailxx/Budila/actions/workflows/build.yml/badge.svg)](https://github.com/sailxx/Budila/actions/workflows/build.yml)

</div>

<br>

<img src="assets/readme/screens.svg" width="100%" alt="Экраны Budila: список, редактор, звонок, тёмная тема">

<br>

<img src="assets/readme/features.svg" width="100%" alt="Что умеет Budila">

<details>
<summary><b>Подробнее о возможностях</b></summary>

### ⏰ Список будильников

Сверху карточка **«Следующий будильник»**: день, время и сколько осталось («через 7 ч 15 мин»). Ниже все будильники: крупное время, название, дни повтора и переключатель. Выключенные приглушаются, чтобы активные было видно сразу.

### ✏️ Редактор

Время задаётся на **циферблате Material 3** или вводом с клавиатуры. Есть название («Работа», «Пробежка»), повтор по дням недели и вибрация. После сохранения Budila подскажет, через сколько прозвенит будильник. Удалённый будильник можно вернуть одной кнопкой.

### 🔔 Звонок

Экран звонка сам включает дисплей и открывается **поверх экрана блокировки**. Мелодия — системный звук будильника, громкость нарастает плавно. Кнопки **«Выключить»** и **«Отложить на 5 мин»** есть и на экране, и в уведомлении. Если никто не ответит, звонок сам затихнет через 10 минут.

### 🛡 Надёжность

Будильники ставятся через системный `AlarmClock`, поэтому звонят точно, даже когда телефон в режиме сна. После перезагрузки, смены времени или часового пояса Budila сама расставит их заново.

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

<details>
<summary><b>Собрать из исходников</b></summary>

Нужны JDK 17+ и Android SDK (платформа 36).

```bash
git clone https://github.com/sailxx/Budila.git
cd Budila
./gradlew assembleRelease
```

Для подписанного APK скопируйте `keystore.properties.example` в `keystore.properties` и укажите свой ключ; без него соберётся неподписанный APK. Скриншоты экранов перерисовываются командой `./gradlew testDebugUnitTest` (Robolectric, без телефона) и появляются в `screenshots/`.

**Стек:** Kotlin · Jetpack Compose · Material 3 · AlarmManager · Foreground Service.

</details>

## Приватность и безопасность

У Budila **нет доступа к интернету**: в манифесте нет разрешения `INTERNET`. Будильники хранятся только на телефоне и не попадают в облачные бэкапы. Нет рекламы, аналитики и трекеров. Подробности и как сообщить об уязвимости — в [SECURITY.md](SECURITY.md).

<br>

<div align="center"><sub>© 2026 sailxx. Все права защищены.</sub></div>
