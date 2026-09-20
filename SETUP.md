# Guia de Setup — Semáforo de Valores Pro

## Pré-requisitos

- Android Studio Ladybug (2024.2) ou mais novo
- JDK 17
- Android SDK 35
- Conta GitHub

## 1. Subir para o GitHub

```bash
cd SemaforoValoresPro
git init
git add .
git commit -m "feat: projeto completo"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/SemaforoValoresPro.git
git push -u origin main
```

O workflow `.github/workflows/build.yml` roda testes, gera o APK debug e release (sem assinar)
e publica ambos em **Actions → (execução) → Artifacts**.

## 2. Build local

```bash
gradle assembleDebug
# opcional: gerar o wrapper
gradle wrapper --gradle-version 8.9
```

No Android Studio: *File → Open* na pasta do projeto e aguarde o sync.

## 3. APK assinado no CI (opcional)

Em *Settings → Secrets and variables → Actions* crie:

| Secret | Descrição |
|--------|-----------|
| `KEYSTORE_BASE64` | `base64 -w 0 semaforovalores.jks` |
| `KEY_ALIAS` | Alias da chave |
| `KEY_PASSWORD` | Senha da chave |
| `STORE_PASSWORD` | Senha do keystore |

Gerar keystore:
```bash
keytool -genkeypair -v -keystore semaforovalores.jks -keyalg RSA -keysize 2048 -validity 10000 -alias semaforovalores
```
O APK assinado é gerado e anexado quando você cria uma **Release** no GitHub.

## 4. Instalar no celular

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 5. Calibrar a leitura dos apps

```bash
adb shell uiautomator dump /sdcard/window_dump.xml
adb pull /sdcard/window_dump.xml
```

Com a oferta na tela, abra o `window_dump.xml` e veja onde ficam distância, tempo e valor.
Ajuste `extractUberOffer()` (e os equivalentes de 99 e inDrive) em
`service/RideAccessibilityService.kt`. Dica: se o app mostra "5 min (2 km)" de busca e
"15 min (8 km)" da viagem, escolha o texto certo pela posição/ID em vez do primeiro `km`/`min`.

## Estrutura

```
app/src/main/java/com/semaforovalores/
├── SemaforoApp.kt
├── model/          TripOffer, UserSettings
├── util/           TripClassifier (lógica verde/amarelo/vermelho)
├── data/           AppDatabase, TripDao (Room), SettingsRepository (DataStore)
├── service/        RideAccessibilityService, OverlayService, OverlayLifecycleOwner
└── ui/
    ├── overlay/    TripOverlayCard
    ├── screens/    MainActivity, HomeScreen, HistoryScreen, SettingsScreen
    └── theme/      Theme.kt
```
