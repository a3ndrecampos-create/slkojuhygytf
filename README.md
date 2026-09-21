# 🛒 Lista Inteligente

App Android para lista de compras inteligente com **escaneamento de etiquetas** de supermercado via câmera.

## Funcionalidades

- 📷 **Escanear etiquetas** — aponte a câmera para a etiqueta e o app lê automaticamente todos os preços (varejo, atacado, clube, app)
- 💰 **Escolha o melhor preço** — selecione entre as opções disponíveis e informe a quantidade
- 🧮 **Totalizador em tempo real** — veja o total da lista atualizado a cada item, sem surpresas no caixa
- 📊 **Controle de orçamento** — defina um limite e acompanhe quanto falta
- ✅ **Marcar como pego** — risca o item da lista enquanto você faz compras
- 📁 **Múltiplas listas** — organize por supermercado ou ocasião

## Formatos de etiqueta suportados

| Rede | Formatos |
|------|----------|
| Max Atacadista | Varejo / Atacado / Crediffato / Clube Max App |
| Assaí | Varejo / Atacado |
| Atacadão | Varejo / Atacado |
| Supermercados em geral | Preço unitário / kg |

## Stack

- **Kotlin** + Jetpack Compose
- **ML Kit Text Recognition** — OCR das etiquetas
- **CameraX** — preview e análise de frames
- **Room** — banco local das listas
- **Hilt** — injeção de dependências
- **DataStore** — preferências

## Build

```bash
git clone https://github.com/SEU_USUARIO/ListaInteligente
cd ListaInteligente
./gradlew assembleDebug
# APK em: app/build/outputs/apk/debug/
```

## Permissões

- `CAMERA` — escanear etiquetas de preço

## Licença

MIT
