# Changelog

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/).

## [1.1.7] — 2026-06-10

### Исправлено
- Settings: Shizuku shell с кавычками вокруг component (`Settings$…` больше не ломается в sh); shell раньше intent.
- Running: meminfo/displayPackages отсекают нативные процессы (`media.*`, `hidl.*`, `@` в имени).
- Тап по плашке на вкладке «Приложения» (название, иконка, память).

## [1.1.6] — 2026-06-10

### Исправлено
- Running: только установленные пакеты; фильтр native process (`media.*`, `android.hidl.*`).
- Settings: прямой запуск `com.android.settings` (+ Shizuku fallback), без Meta/VrShell relay.
- Тап по плашке на вкладке «Приложения» → Android app details.

## [1.1.5] — 2026-06-09

### Исправлено
- Фильтр «Пользовательские»: системные процессы из dumpsys больше не попадают как user (UID, префиксы, unknown entries).
- «Настройки Android»: запуск через системный `ACTION_SETTINGS` (VrShell relay на Quest), без неэкспортируемого `Settings`.

## [1.1.4] — 2026-06-09

### Добавлено
- Вкладка **Лог** — просмотр файла лога в реальном времени (фоновое обновление, без сброса прокрутки и выделения).

## [1.1.3] — 2026-06-09

### Исправлено
- Кнопка «Настройки Android» открывает главный экран Settings, а не режим разработчика.
- Список запущенных: `dumpsys activity processes`, summary meminfo, расширенный парсинг `ps`.
- Тап по плашке в «Запущенные» → страница приложения в Android Settings.
- Закрытие: `am force-stop` + `am kill` + `kill -9` по PID; отчёт о защищённых и неудачных.

## [1.1.2] — 2026-06-09

### Исправлено
- Зависание и тёмный экран при запуске: рекурсия в логере (`ShizukuShell` → `FileLogger` → shell → снова лог).
- Лог на Quest: запись в `/sdcard/Download/` через Shizuku без зацикливания.

### Изменено
- При каждом запуске из лаунчера лог очищается (старые записи не накапливаются).
- Shell-команды пишутся только в logcat, не в файл лога.
- Дублирование лога во внутреннее хранилище приложения (надёжная запись).

## [1.1.1] — 2026-06-09

### Исправлено
- Release APK подписан — установка через SideQuest/adb (v1.1.0 → `INSTALL_PARSE_FAILED_NO_CERTIFICATES`).

## [1.1.0] — 2026-06-09

### Добавлено
- Вкладки: Запущенные, Приложения, Настройки.
- Диск и RAM отдельно в карточках приложений.
- Фильтр running: ps ∪ meminfo (без recents-призраков).
- Локальное обновление toggles без full refresh.
- Protected apps: kill-protected и system-filter, видимость self + NMB.
- `CleanupForegroundService` — фоновая очистка по уведомлению.
- `orderedForKill` — self последним при kill-all.
- Кнопка **Настройки Android** (`com.android.settings`).
