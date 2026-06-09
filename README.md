# Quest Task Manager (QTaskMgr)

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Диспетчер задач для Meta Quest** — просмотр и закрытие приложений, управление правами фона, очистка по уведомлению. Работает **автономно** через [Shizuku](https://shizuku.rikka.app/).

| | |
|---|---|
| **Разработчик** | [kabzon93region](https://github.com/kabzon93region) |
| **Package** | `com.quest3.taskmanager` |
| **Версия** | 1.1.0 |

## Возможности

| Вкладка | Описание |
|---------|----------|
| **Запущенные** | Процессы (ps + meminfo), RAM/диск, фильтр user/system, закрытие (все / выбранные / по правилам / тап) |
| **Приложения** | Список установленных apps, переключатели «Фон» и «Данные» (Android `appops` / `netpolicy`) |
| **Настройки** | Shizuku, **Настройки Android** (`com.android.settings`), уведомление очистки, лог |

## Требования

- Meta Quest 2 / 3 / Pro, **Developer Mode**
- [Shizuku](https://shizuku.rikka.app/) с разрешением для QTaskMgr

**Не требуется:** No More Background, QTKiller — QTaskMgr v1.1 включает свою очистку по правилам через уведомление.

## Установка

1. Скачайте APK из [Releases](https://github.com/kabzon93region/quest-task-manager/releases) или соберите сами.
2. Установите через SideQuest / `adb install -r`.
3. На Quest: **Apps → Unknown Sources → Quest Task Manager**.
4. Запустите Shizuku, выдайте разрешение QTaskMgr.

```powershell
adb install -r dist\QTaskMgr-v1.1.0-release.apk
```

## Быстрый старт

1. **Shizuku** — pairing или USB, в Shizuku → Manage apps → разрешить QTaskMgr.
2. **Приложения** — выключите «Фон» у apps, которые не должны работать в фоне.
3. **Настройки** — включите «Фоновая очистка (уведомление)».
4. Перед игрой — тап по уведомлению QTaskMgr → закроются apps с запрещённым фоном.

Лог по умолчанию: `/sdcard/Download/QTaskManager.log`

## Сборка

```powershell
.\scripts\generate-launcher-icons.ps1   # при смене иконки
.\scripts\build-apk.ps1 -Release        # release APK → dist\
.\scripts\build-apk.ps1                 # debug APK
```

Требуется: JDK 17, Android SDK, Gradle 8.2+ (или локальный из `quest-app-guard\.gradle-local`).

## Структура проекта

```
quest-task-manager/
├── LICENSE
├── NOTICE
├── docs/
│   ├── THIRD_PARTY.md      # лицензии зависимостей
│   ├── GITHUB_PUBLISH.md   # публикация на GitHub
│   └── RELEASE_NOTES_v1.1.0.md
├── scripts/
├── src/quest-app/          # Android Gradle project
└── dist/                   # собранные APK (в .gitignore)
```

## Лицензия и third-party

- **QTaskMgr** — [MIT](LICENSE) © kabzon93region
- Зависимости (Shizuku, AndroidX, …) — [docs/THIRD_PARTY.md](docs/THIRD_PARTY.md)
- [NOTICE](NOTICE) — краткая атрибуция для дистрибутивов

Управление фоновыми правами использует те же публичные API Android, что и [No More Background](https://github.com/adil192/no_more_background); код NMB **не включён** и не является частью этого проекта.

## Связанные проекты

| Проект | Связь |
|--------|-------|
| [Quest Task Kuller (QTKiller)](https://github.com/kabzon93region/quest-task-kuller) | Опционально; отдельный package, режим AGGRESSIVE при повторном запуске |
| [No More Background](https://github.com/adil192/no_more_background) | Аналогичная идея; QTaskMgr — независимая реализация |

## Disclaimer

Не аффилирован с Meta Platforms, Inc. или Google LLC. Используйте на свой риск. Закрытие системных процессов Quest ограничено whitelist'ом в приложении.
