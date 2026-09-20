# Arquitetura da base 3.1.3

Mapa de como o app é organizado e onde mexer. Escrito para quem vai trabalhar no
visual sem quebrar o domínio.

## Visão geral

```
UI (Compose)
   ↓ observa
SharedViewModel            ← estado compartilhado entre telas
   ↓ chama
core/facade/*Facade        ← a fronteira: é tudo que a UI precisa conhecer
   ↓ orquestra
core/usecase/*             ← regras de negócio, uma classe por operação
   ↓ usa
core/service/*             ← BLE, avaliação, tendência, cálculo derivado
   ↓ persiste
core/database (Room)
```

A regra prática: **a UI fala com o ViewModel e com as facades. Nunca com Room,
nunca com um handler de balança.**

Injeção de dependência por **Hilt** — as facades chegam aos ViewModels por
construtor, sem singleton manual.

## `core/` — domínio

### `facade/` — a porta de entrada

| Facade | Responsabilidade |
|---|---|
| `MeasurementFacade` | consultar, criar, editar e apagar medições; agregação; avaliação; insights |
| `UserFacade` | perfis |
| `BluetoothFacade` | escanear, conectar, receber medições |
| `SettingsFacade` | preferências |
| `DataManagementFacade` | import/export, backup |

São a fronteira estável. Se uma tela nova precisa de dado, é aqui que ela pede.

### `usecase/` — regras de negócio

Uma classe por área: CRUD, filtro, suavização, transformação, agregação,
avaliação, insights, import/export, backup, demo.

Mexer aqui é mexer em **comportamento**, não em visual.

### `service/` — serviços de apoio

- `BleScanner` / `BleConnector` — camada Bluetooth de baixo nível
- `MeasurementEvaluator` — classifica valores em faixas (baixo/normal/alto)
- `TrendCalculator` — linha de tendência
- `DerivedValuesCalculator` — métricas calculadas a partir de outras
- `MeasurementEnricher` — completa uma medição com o que dá para derivar

### `bluetooth/` — ⚠️ não editar

`ScaleFactory` mais **72 handlers** em `scales/`, um por protocolo, e `libs/`
com os algoritmos de composição corporal de cada fabricante.

Erros aqui **não geram crash** — a balança simplesmente para de funcionar, e só
se descobre com hardware real. Ver
[balancas-bluetooth.md](balancas-bluetooth.md).

### `database/`, `data/`, `model/`

Room, entidades e modelos de domínio. O tipo central é `MeasurementWithValues`:
uma medição com seus valores, que é o que as telas recebem.

### `worker/`

Trabalho agendado: backup automático e lembretes de pesagem.

## `ui/` — tudo em Compose

### `theme/` — onde o design vive

| Arquivo | Origem |
|---|---|
| `DesignColor.kt` | **nosso** — a paleta do design |
| `DesignType.kt` | **nosso** — a tipografia do design |
| `Color.kt`, `Type.kt` | do upstream — mantidos intocados para não conflitar |
| `Theme.kt` | do upstream, com nossas linhas — escolhe o esquema |

### `screen/` — uma pasta por tela

`overview`, `graph`, `table`, `statistics`, `insights`, `settings`, mais
`components/` (compartilhados) e `dialog/`.

`components/` é o lugar de maior alavancagem para trabalho visual: mexer num
cartão ali propaga para várias telas.

### `navigation/`

- `Routes.kt` — as rotas
- `AppNavHost.kt` — o grafo
- `AppNavigation.kt` — scaffold, barra inferior, top bar

Para trocar uma tela por uma versão redesenhada, o caminho limpo é criar o
Composable novo e **apontar a rota para ele** — sem editar o original.

### `shared/SharedViewModel.kt`

Estado que atravessa telas: usuário selecionado, título e ações da top bar,
modo de seleção, eventos de snackbar, e os *flows* de dados
(`screenFlow`, `drillDownFlow`).

A maioria das telas observa este ViewModel. Entender `screenFlow` é o atalho
para entender como os dados chegam à tela.

## Onde mexer, por tipo de tarefa

| Quero… | Vou em |
|---|---|
| mudar cor ou fonte | `ui/theme/Design*.kt` |
| mudar a forma de uma tela | `ui/screen/<tela>/` |
| mudar algo que aparece em várias telas | `ui/screen/components/` |
| mudar navegação ou barra inferior | `ui/navigation/` |
| buscar um dado que a tela não tem | a facade correspondente |
| mudar o cálculo de uma métrica | `core/usecase/` ou `core/service/` — **não é visual** |
| suporte a uma balança | `core/bluetooth/` — **ler o doc antes** |

## Acompanhar o upstream

```bash
git fetch upstream
git log master..upstream/master --oneline
```

Para o merge continuar barato: nossas mudanças ficam em arquivos com prefixo
`Design`, e telas redesenhadas ganham arquivo próprio em vez de editar o
original. O único ponto de contato inevitável é `Theme.kt`.
