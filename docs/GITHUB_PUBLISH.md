# Публикация на GitHub (private → public)

Пошаговая инструкция для репозитория **Quest Task Manager (QTaskMgr)**.

**Разработчик:** [kabzon93region](https://github.com/kabzon93region)  
**Рекомендуемое имя репозитория:** `quest-task-manager`

---

## Часть 1. Подготовка на ПК

### 1.1. Проверить `.gitignore`

Не должны попасть в Git:

- `**/build/`, `.gradle/`, `local.properties`
- `*.log`, `logs/`
- `dist/*.apk`
- `*.jks`, `keystore.properties`, `signing.properties`

### 1.2. Собрать release APK

```powershell
cd B:\quest3\PC\quest-task-manager
.\scripts\build-apk.ps1 -Release
```

Результат: `dist\QTaskMgr-v1.1.2-release.apk`

Release подписывается debug-ключом (для SideQuest/adb). Без подписи Android выдаёт `INSTALL_PARSE_FAILED_NO_CERTIFICATES`.

### 1.3. Инициализировать Git (если ещё не сделано)

```powershell
cd B:\quest3\PC\quest-task-manager
git init
git add .
git status
```

Проверьте `git status` — не должно быть `build/`, `*.log`, `dist/*.apk`.

```powershell
git commit -m "Initial release: Quest Task Manager (QTaskMgr) v1.1.0"
```

---

## Часть 2. Создание private-репозитория на GitHub

1. [GitHub](https://github.com) → **+** → **New repository**
2. Поля:

| Поле | Значение |
|------|----------|
| **Owner** | `kabzon93region` |
| **Repository name** | `quest-task-manager` |
| **Description** | `Task manager for Meta Quest 3 via Shizuku — kill apps, background policies, notification cleanup.` |
| **Visibility** | **Private** |
| **Add a README** | ❌ (уже в проекте) |
| **Add .gitignore** | ❌ |
| **Choose a license** | ❌ (LICENSE уже в проекте) |

3. **Create repository**

### Залить код

```powershell
cd B:\quest3\PC\quest-task-manager
git branch -M main
git remote add origin https://github.com/kabzon93region/quest-task-manager.git
git push -u origin main
```

---

## Часть 3. GitHub Release

### v1.1.2 (текущий — исправление запуска)

1. Репозиторий → **Releases** → **Draft a new release**
2. **Tag:** `v1.1.2` (create new tag on `main`)
3. **Title:** `Quest Task Manager v1.1.2`
4. **Description** — скопировать из [RELEASE_NOTES_v1.1.2.md](RELEASE_NOTES_v1.1.2.md)
5. Прикрепить: `dist\QTaskMgr-v1.1.2-release.apk`
6. **Set as the latest release** → **Publish release**

### Предыдущие релизы

| Версия | Примечание |
|--------|------------|
| v1.1.1 | Подпись APK; возможны зависания при запуске — обновиться до v1.1.2 |
| v1.1.0 | Без подписи — `INSTALL_PARSE_FAILED_NO_CERTIFICATES` |

---

## Часть 3.1. Последующие релизы (шпаргалка)

1. Обновить `versionCode` / `versionName` в `src/quest-app/app/build.gradle.kts` и `scripts/build-apk.ps1`.
2. Добавить запись в `CHANGELOG.md` и `docs/RELEASE_NOTES_vX.Y.Z.md`.
3. `.\scripts\build-apk.ps1 -Release`
4. Commit + push в `main`.
5. GitHub → Releases → тег `vX.Y.Z` → описание из `RELEASE_NOTES` → asset `dist\QTaskMgr-vX.Y.Z-release.apk`.

### GitHub CLI

```powershell
gh release create v1.1.2 dist\QTaskMgr-v1.1.2-release.apk --title "Quest Task Manager v1.1.2" --notes-file docs\RELEASE_NOTES_v1.1.2.md
```

---

## Часть 4. Переход в Public (когда готовы)

1. **Settings** → **General** → **Danger Zone** → **Change repository visibility** → **Public**
2. Убедиться, что в README и LICENSE указан MIT и ссылка на `docs/THIRD_PARTY.md`

---

## Подпись release APK (опционально, для production)

По умолчанию `assembleRelease` подписывается debug-ключом (удобно для sideload).

Для постоянного release-ключа:

```powershell
keytool -genkey -v -keystore release.jks -alias qtaskmgr -keyalg RSA -keysize 2048 -validity 10000
```

Создайте `src/quest-app/keystore.properties` (в `.gitignore`):

```properties
storeFile=../../release.jks
storePassword=***
keyAlias=qtaskmgr
keyPassword=***
```

И раскомментируйте блок `signingConfigs` в `app/build.gradle.kts` (см. комментарии в файле).

---

## Чеклист перед публикацией

- [ ] `LICENSE`, `NOTICE`, `docs/THIRD_PARTY.md` на месте
- [ ] README актуален (версия, установка, Shizuku)
- [ ] Release APK собран и протестирован на Quest
- [ ] В репозитории нет логов, `build/`, keystore
- [ ] Private repo создан, push выполнен
