# CoreMe Android

Android-мессенджер на Kotlin + Jetpack Compose.

## Setup

1. Склонируй репозиторий.
2. Скопируй `local.properties.example` в `local.properties`:
   - Windows (PowerShell): `Copy-Item local.properties.example local.properties`
   - Unix: `cp local.properties.example local.properties`
3. Открой `local.properties` и подставь URL твоего бэкенда в `debug.base.url`.
   - Эмулятор: `http://10.0.2.2:3001`
   - Физическое устройство в Wi-Fi: `http://<IP-твоего-ПК>:3001` (например `http://192.168.0.200:3001`)
4. Открой проект в Android Studio и запусти.

## Build

- Debug: `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`)
- Release: `./gradlew assembleRelease` (требует настроенного keystore — см. ниже)

## Release signing

Перед сборкой релиза создай keystore и пропиши пути в `local.properties`:

```
release.store.file=../keystore/coreme-release.jks
release.store.password=твой_пароль
release.key.alias=coreme
release.key.password=твой_пароль_ключа
```

Сгенерировать keystore:

```
keytool -genkey -v -keystore coreme-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias coreme
```

## Backend (Go API)

Go REST API находится в папке `backend/`. Стек: Gin, PostgreSQL (pgx), JWT, Sentry, OpenTelemetry.

### Запуск

```bash
cd backend
cp .env.example .env     # заполнить DB_* и JWT_SECRET
go run ./cmd/api         # по умолчанию порт 3001
```

### Тесты

```bash
cd backend
go test ./...
```

Тесты с БД пропускаются автоматически, если PostgreSQL недоступен.
