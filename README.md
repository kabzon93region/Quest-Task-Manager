# Quest Task Manager (QTaskMgr)

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Диспетчер задач для Meta Quest** — просмотр и закрытие приложений, управление правами фона, очистка по уведомлению. Работает **автономно** через [Shizuku](https://shizuku.rikka.app/).

| | |
|---|---|
| **Разработчик** | [kabzon93region](https://github.com/kabzon93region) |
| **Package** | `com.quest3.taskmanager` |
| **Версия** | 1.4.1 ([схема версий](docs/VERSIONING.md)) |
| **GitHub** | [quest-task-manager](https://github.com/kabzon93region/quest-task-manager) |

## Возможности

| Вкладка | Описание |
|---------|----------|
| **Запущенные** | Процессы (ps + meminfo), RAM устройства и на карточке, фильтры Все / Пользовательские / Системные / **Демоны**, **поиск**, закрытие (все / выбранные / по правилам) |
| **Приложения** | Список установленных apps, те же фильтры и поиск, переключатели «Фон» и «Данные» (`appops` / `netpolicy`) |
| **Настройки** | Версия, GitHub, **DonationAlerts**, Shizuku, **Настройки Android**, уведомление очистки, лог |
| **Лог** | Просмотр файла лога в реальном времени |

## Требования

- Meta Quest 2 / 3 / Pro, **Developer Mode**
- [Shizuku](https://shizuku.rikka.app/) с разрешением для QTaskMgr

**Не требуется:** No More Background, QTKiller — QTaskMgr включает свою очистку по правилам через уведомление.

## Android Settings на Quest

Кнопка **«Настройки Android»** и тап по приложению в списке открывают пакет `com.android.settings` (как на телефоне: Wi‑Fi, приложения, спец. возможности, режим разработчика).

**На Quest этого приложения нет «из коробки»** — в системном меню Meta его не видно. Пакет `com.android.settings` обычно **попадает на шлем вместе с другой утилитой**, например при установке через SideQuest или Quest Games Optimizer. Если вы когда‑то ставили QGO, Hidden Settings или похожий APK — Android Settings, скорее всего, уже есть.

Проверка: вкладка **Настройки** → **Настройки Android**. Если пакет не найден, QTaskMgr покажет диалог с ссылками (тот же текст, что ниже).

### Где взять Android Settings

| Вариант | Описание | Ссылка |
|---------|----------|--------|
| **Quest Games Optimizer (QGO)** | Часто ставят через SideQuest; в QGO есть кнопка «Open Settings» / встроенный доступ к Android Settings | [anagan79.itch.io/quest-games-optimizer](https://anagan79.itch.io/quest-games-optimizer) |
| **XR Native Android Settings** | Отдельная обёртка под VR | [anagan79.itch.io/xr-native-android-settings](https://anagan79.itch.io/xr-native-android-settings) |
| **Quest Hidden Settings** | Популярная обёртка от threethan | [GitHub Releases](https://github.com/threethan/QuestHiddenSettings/releases) |

Установка — через SideQuest, `adb install`, Mobile VR Station или встроенный установщик в выбранной утилите. После появления `com.android.settings` кнопка в QTaskMgr начнёт работать без переустановки самого QTaskMgr.

## Установка

1. Скачайте APK из [Releases](https://github.com/kabzon93region/quest-task-manager/releases) или соберите сами.
2. Установите через SideQuest / `adb install -r`.
3. На Quest: **Apps → Unknown Sources → Quest Task Manager**.
4. Запустите Shizuku, выдайте разрешение QTaskMgr.

```powershell
adb install -r dist\QTaskMgr-v1.4.1-release.apk
```

## Быстрый старт

1. **Shizuku** — pairing или USB, в Shizuku → Manage apps → разрешить QTaskMgr.
2. **Приложения** — выключите «Фон» у apps, которые не должны работать в фоне.
3. **Настройки** — включите «Фоновая очистка (уведомление)».
4. Перед игрой — тап по уведомлению QTaskMgr → закроются apps с запрещённым фоном.

Лог по умолчанию: `/sdcard/Download/QTaskManager.log` (очищается при каждом запуске из лаунчера)

## Сборка

```powershell
.\scripts\generate-launcher-icons.ps1   # при смене иконки
.\scripts\build-apk.ps1 -Release        # release APK → dist\
.\scripts\build-apk.ps1                 # debug APK
```

Требуется: JDK 17, Android SDK, Gradle 8.2+ (или локальный из `quest-task-killer\.gradle-local`).

## Структура проекта

```
quest-task-manager/
├── LICENSE
├── NOTICE
├── CHANGELOG.md
├── docs/
│   ├── README.md
│   ├── THIRD_PARTY.md
│   ├── GITHUB_PUBLISH.md
│   ├── VERSIONING.md
│   ├── RELEASE_NOTES_v1.4.1.md   # актуальный для GitHub Releases
│   ├── RELEASE_NOTES_v1.4.0.md   # накопительный v1.2.14 → v1.4.0
│   └── RELEASE_NOTES_v*.md
├── scripts/
├── src/quest-app/          # Android Gradle project
└── dist/                   # собранные APK (в .gitignore)
```

## Лицензия и third-party

- **QTaskMgr** — [MIT](LICENSE) © kabzon93region
- Зависимости (Shizuku, AndroidX, …) — [docs/THIRD_PARTY.md](docs/THIRD_PARTY.md)
- [NOTICE](NOTICE) — краткая атрибуция для дистрибутивов

Управление фоновыми правами использует те же публичные API Android, что и [No More Background](https://github.com/adil192/no_more_background); код NMB **не включён** и не является частью этого проекта.

## Поддержать проект

Разовый донат картой РФ, СБП, ЮMoney, VK Pay:

**[DonationAlerts → kabzon93region](https://www.donationalerts.com/r/kabzon93region)**

Та же ссылка — во вкладке **Настройки** в приложении.

> Если ваш ник на DonationAlerts другой — замените `kabzon93region` в `strings.xml` (`settings_donationalerts_url`) и в этом README.

## Связанные проекты

| Проект | Связь |
|--------|-------|
| [Quest Task Killer (QTKiller)](https://github.com/kabzon93region/quest-task-killer) | Опционально; отдельный package, режим AGGRESSIVE при повторном запуске |
| [No More Background](https://github.com/adil192/no_more_background) | Аналогичная идея; QTaskMgr — независимая реализация |

## Disclaimer

Не аффилирован с Meta Platforms, Inc. или Google LLC. Используйте на свой риск — закрытие системных процессов и демонов может повлиять на стабильность шлема.
