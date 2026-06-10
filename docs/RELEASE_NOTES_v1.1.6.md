# Quest Task Manager v1.1.6

## Что нового в v1.1.6

- **Запущенные / Пользовательские:** только установленные пакеты; убраны `media.*`, `android.hidl.*` и прочие имена процессов.
- **Настройки Android:** только `com.android.settings` (без generic intent — Meta/VrShell больше не перехватывает).
- **Тап по плашке** работает и на вкладке «Приложения»; детали app через `InstalledAppDetails`.
