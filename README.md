# 🚦 Semáforo de Valores Pro

App Android para motoristas de aplicativo (Uber, 99, inDrive) que exibe métricas de corrida em tempo real com classificação por cor e cálculo de custo de combustível.

## Funcionalidades

- **Overlay automático** sobre Uber, 99 e inDrive
- **Semáforo de cores**: Verde ✅ / Amarelo ⚠️ / Vermelho ❌
- **5 métricas por corrida**: R$/km, R$/hora, Lucro%, Avaliação, Lucro líquido
- **Cálculo de combustível** em tempo real
- **Histórico de corridas** aceitas/recusadas
- **Configurações** de limites por cor

## Stack

- Kotlin + Jetpack Compose
- Android Accessibility Service (leitura de tela)
- WindowManager (overlay flutuante)
- Room Database (histórico)
- DataStore (configurações)

## Build

```bash
git clone https://github.com/SEU_USUARIO/SemaforoValoresPro
cd SemaforoValoresPro
./gradlew assembleDebug
```

## Permissões necessárias

- `SYSTEM_ALERT_WINDOW` — overlay flutuante
- `BIND_ACCESSIBILITY_SERVICE` — leitura dos apps de corrida
- `FOREGROUND_SERVICE` — serviço em background

## Licença

MIT
