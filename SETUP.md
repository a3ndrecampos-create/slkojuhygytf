# Guia de Setup — Semáforo de Valores Pro

## Pré-requisitos

- Android Studio Hedgehog (2023.1.1) ou mais novo
- JDK 17
- Android SDK 35
- Conta GitHub

## 1. Criar repositório no GitHub

```bash
# Criar repo pelo CLI do GitHub
gh repo create SemaforoValoresPro --public --description "App para motoristas de app"

# Ou via interface: github.com/new
```

## 2. Subir o projeto

```bash
cd SemaforoValoresPro
git init
git add .
git commit -m "feat: estrutura inicial do projeto"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/SemaforoValoresPro.git
git push -u origin main
```

## 3. Configurar Secrets para CI/CD (APK assinado)

No GitHub: Settings → Secrets and variables → Actions

| Secret | Descrição |
|--------|-----------|
| `KEYSTORE_BASE64` | `base64 -w 0 seu_keystore.jks` |
| `KEY_ALIAS` | Alias da chave |
| `KEY_PASSWORD` | Senha da chave |
| `STORE_PASSWORD` | Senha do keystore |

### Gerar keystore:
```bash
keytool -genkeypair -v \
  -keystore semaforovalores.jks \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -alias semaforovalores
```

## 4. Build local

```bash
# Debug
./gradlew assembleDebug
# APK em: app/build/outputs/apk/debug/

# Release
./gradlew assembleRelease
```

## 5. Instalar no celular

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 6. Permissões necessárias (configurar após instalar)

1. **Overlay (janela sobreposta)**: Configurações → Apps → Semáforo de Valores → Exibir sobre outros apps
2. **Acessibilidade**: Configurações → Acessibilidade → Semáforo de Valores → Ativar

## 7. Ajustar extração de dados (importante!)

O arquivo `RideAccessibilityService.kt` precisa ser calibrado para cada versão dos apps:

```kotlin
// Método extractUberOffer() — inspecione o layout do Uber Driver com:
adb shell uiautomator dump && adb pull /sdcard/window_dump.xml
```

Use o Android Layout Inspector para mapear os IDs dos campos de distância, tempo e valor.

## Estrutura do projeto

```
app/src/main/java/com/semaforovalores/
├── model/
│   ├── TripOffer.kt        # Dados da corrida + cálculos
│   └── UserSettings.kt     # Configurações do usuário
├── service/
│   ├── RideAccessibilityService.kt  # Lê dados dos apps
│   └── OverlayService.kt            # Exibe o overlay
├── data/
│   ├── AppDatabase.kt      # Room DB
│   ├── TripDao.kt          # DAO histórico
│   └── SettingsRepository.kt        # DataStore
├── ui/
│   ├── overlay/
│   │   └── TripOverlay.kt  # Compose: card do overlay
│   ├── screens/
│   │   ├── MainActivity.kt
│   │   ├── HomeScreen.kt   # Dashboard principal
│   │   ├── HistoryScreen.kt
│   │   └── SettingsScreen.kt
│   └── theme/
│       └── Theme.kt
└── util/
    └── TripClassifier.kt   # Lógica verde/amarelo/vermelho
```
