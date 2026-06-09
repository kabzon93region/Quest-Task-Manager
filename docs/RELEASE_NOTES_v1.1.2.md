# Quest Task Manager v1.1.2

**Диспетчер задач для Meta Quest** — просмотр и закрытие приложений, управление правами фона, очистка по уведомлению. Работает автономно через [Shizuku](https://shizuku.rikka.app/).

## Установка

1. Включите **Developer Mode** на Quest.
2. Установите и запустите [Shizuku](https://shizuku.rikka.app/), выдайте разрешение QTaskMgr.
3. Скачайте `QTaskMgr-v1.1.2-release.apk` из Assets этого релиза.
4. Установите:
   ```powershell
   adb install -r QTaskMgr-v1.1.2-release.apk
   ```
   Или перетащите APK в SideQuest.
5. На шлеме: **Apps → Unknown Sources → Quest Task Manager**.

## Возможности

- **Запущенные** — процессы (ps + meminfo), закрытие вручную / по правилам / все
- **Приложения** — переключатели фона и фоновых данных (Android appops / netpolicy)
- **Настройки** — Shizuku, Android Settings, фоновая очистка по уведомлению, лог
- Диск и RAM отдельно в карточках приложений
- Защита системных Meta/Oculus процессов от случайного закрытия

## Требования

- Meta Quest 2 / 3 / Pro (Android 10+, minSdk 29)
- Shizuku с разрешением для `com.quest3.taskmanager`

## Что нового в v1.1.2

- **Исправлен запуск:** устранена рекурсия в логере, из‑за которой приложение зависало на тёмном экране (сотни shell-вызовов при старте).
- **Лог:** очищается при каждом запуске из лаунчера; shell-команды только в logcat.
- Путь лога по умолчанию: `/sdcard/Download/QTaskManager.log`

## Рекомендуется обновиться с

- **v1.1.0** — APK без подписи, не ставится через SideQuest.
- **v1.1.1** — подпись есть, но возможны зависания при запуске из‑за бага логера.

## Лицензия

MIT — см. [LICENSE](../LICENSE). Сторонние компоненты: [THIRD_PARTY.md](THIRD_PARTY.md).

**Разработчик:** [kabzon93region](https://github.com/kabzon93region)
