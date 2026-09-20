# 🚦 Semáforo de Valores Pro

App Android para motoristas de aplicativo (Uber, 99, inDrive) que mostra, sobre o app de corrida, se a oferta compensa: **verde / amarelo / vermelho**, com cálculo de combustível.

## Funcionalidades

- **Overlay automático** sobre Uber, 99 e inDrive
- **Semáforo**: Verde ✅ / Amarelo ⚠️ / Vermelho ❌ (vale a pior métrica)
- **5 métricas**: R$/km, R$/hora, lucro %, nota do passageiro, lucro líquido
- **Combustível** calculado a partir de preço do litro e consumo do carro
- **Histórico** de corridas aceitas/recusadas
- **Ajustes** de todos os limites, apps monitorados e valor mínimo

## Stack

Kotlin · Jetpack Compose · AccessibilityService · WindowManager (overlay) · Room · DataStore

## Build

O projeto **não inclui** `gradlew` / `gradle-wrapper.jar` (arquivo binário). O workflow do GitHub Actions
já instala o Gradle 8.9 sozinho, então basta dar push. Para builds locais:

```bash
gradle assembleDebug            # com Gradle 8.9 instalado
# ou gere o wrapper uma vez:
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

APK em `app/build/outputs/apk/debug/app-debug.apk`.

## Usar

1. Instale o APK.
2. Abra o app e conceda as 3 permissões (sobreposição, acessibilidade, notificações).
   - Em Android 13+ com APK instalado fora da Play Store: *Config. → Apps → Semáforo de Valores → ⋮ → Permitir configurações restritas* antes de ativar a acessibilidade.
3. Toque em **Iniciar monitoramento**.
4. Use **Testar overlay** para ver o card sem precisar de uma corrida real.

## Calibração (importante)

A leitura da tela está em `RideAccessibilityService.kt` (`extractUberOffer`). Ela usa heurísticas de texto (`km`, `min`, `R$`) e precisa ser ajustada à versão atual de cada app. Veja o `SETUP.md`.

## Licença

MIT
