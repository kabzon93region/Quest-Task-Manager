# Quest Task Manager v1.1.0

**Диспетчер задач для Meta Quest 3** — автономное приложение на базе Shizuku.

## Установка

1. Включите **Developer Mode** на Quest.
2. Установите и запустите [Shizuku](https://shizuku.rikka.app/), выдайте разрешение QTaskMgr.
3. Установите APK из этого релиза (Unknown Sources).
4. Опционально: кнопка **Настройки Android** на вкладке «Настройки» — для ADB, специальных возможностей, параметров разработчика.

## Возможности

- **Запущенные** — список процессов (ps + meminfo), закрытие вручную / по правилам / все
- **Приложения** — переключатели фона и фоновых данных (Android appops / netpolicy)
- **Настройки** — Shizuku, Android Settings, фоновая очистка по уведомлению, лог
- Диск и RAM отдельно в карточках приложений
- Защита системных Meta/Oculus процессов от случайного закрытия

## Требования

- Meta Quest 2 / 3 / Pro (Android 10+, minSdk 29)
- Shizuku с разрешением для `com.quest3.taskmanager`

## Не требуется

- No More Background
- Quest Task Kuller (QTKiller)

## Лицензия

MIT — см. [LICENSE](../LICENSE). Сторонние компоненты: [THIRD_PARTY.md](THIRD_PARTY.md).
