# Changelog

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/).

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
