# openScale — guia do repositório

App Android **em Kotlin com Jetpack Compose** para registrar peso e métricas
corporais, com leitura automática via balanças Bluetooth LE. Licença GPLv3.

Fork de [oliexdev/openScale](https://github.com/oliexdev/openScale), alinhado à
versão **3.1.3** do upstream, com um **design visual próprio** aplicado.

## Estado atual do trabalho

O objetivo é aplicar o design de [`design/`](design/) — paleta rosa, tipografia
Bricolage + Instrument Sans — sobre a base do upstream.

**Já feito:** paleta e tipografia. Todo o app usa as cores e fontes do design,
porque no Compose tudo lê do `MaterialTheme`.

**Em aberto:** a estrutura das telas. O protótipo desenha a tela inicial como
peso-herói + sparkline + grade de métricas; a `OverviewScreen` do upstream tem
outra forma, com agregação por período e drill-down. Ver
[.claude/docs/design-compose.md](.claude/docs/design-compose.md).

| Documento | Para quê |
|---|---|
| [.claude/docs/design-compose.md](.claude/docs/design-compose.md) | O design: o que já foi aplicado, o que falta, e como aplicar o resto |
| [.claude/docs/arquitetura.md](.claude/docs/arquitetura.md) | Como a base 3.1.3 é organizada e onde mexer |
| [.claude/docs/balancas-bluetooth.md](.claude/docs/balancas-bluetooth.md) | **Ler antes de tocar em pesagem.** Falha em silêncio. |
| [.claude/docs/build.md](.claude/docs/build.md) | Build, toolchain e como rodar |
| [.claude/docs/historico-fork.md](.claude/docs/historico-fork.md) | De onde viemos: o fork antigo e o redesign em XML |

## Layout do repositório

| Pasta | O que é |
|---|---|
| `design/` | **Fonte de verdade do visual.** Protótipo navegável + tokens. Abrir no navegador. |
| `android_app/` | O projeto Gradle. Praticamente todo o trabalho acontece aqui. |
| `arduino_mcu/` | Firmware da balança open hardware. Não relacionado ao app. |
| `docs/` | Imagens e documentação de protocolos do upstream. |
| `fastlane/` | Metadados de loja (F-Droid/Play). |

## Dentro de `android_app/app/src/main/java/com/health/openscale/`

### `core/` — domínio

Organizado em camadas. **Não é a parte a mexer para trabalho visual.**

- `facade/` — a fronteira que a UI usa: `MeasurementFacade`, `UserFacade`,
  `BluetoothFacade`, `SettingsFacade`, `DataManagementFacade`.
- `usecase/` — regras de negócio por operação (CRUD, agregação, avaliação,
  import/export, backup).
- `service/` — serviços de apoio: `BleScanner`, `BleConnector`,
  `MeasurementEvaluator`, `TrendCalculator`, `DerivedValuesCalculator`.
- `bluetooth/` — `ScaleFactory` + **72 handlers**, um por protocolo, em
  `scales/`. ⚠️ **Nunca editar** — ver o doc das balanças.
- `database/`, `data/`, `model/` — Room, entidades e modelos.
- `worker/` — trabalho agendado (backup automático, lembretes).

### `ui/` — tudo em Compose

- `theme/` — **onde o design vive.**
  - `DesignColor.kt` / `DesignType.kt` — **nossos**: a paleta e a tipografia.
  - `Color.kt` / `Type.kt` — do upstream, mantidos para não conflitar em merge.
  - `Theme.kt` — o `OpenScaleTheme`, que escolhe o esquema.
- `screen/` — uma pasta por tela: `overview`, `graph`, `table`, `statistics`,
  `insights`, `settings`, mais `components/` e `dialog/`.
- `navigation/` — `AppNavHost`, `AppNavigation`, `Routes`.
- `shared/` — `SharedViewModel`, que a maioria das telas observa.
- `widget/` — widget de tela inicial.

## Build

Kotlin, Compose, **minSdk 31** (Android 12), targetSdk/compileSdk 37,
AGP 8.x + Gradle 9.4.1, Hilt para injeção, Room para persistência.

O build **funciona** — use o JDK embutido do Android Studio. Detalhes e
comandos em [.claude/docs/build.md](.claude/docs/build.md).

## Convenções de código

- Kotlin idiomático, seguindo o estilo do upstream.
- Cabeçalho de licença GPL em todo arquivo novo.
- Cor **sempre** de `MaterialTheme.colorScheme`, nunca literal.
- Tipografia **sempre** de `MaterialTheme.typography`.
- Strings em `res/values/strings.xml`; traduções são do **Weblate** e não se
  editam à mão.
- Arquivos novos do design levam o prefixo `Design` para ficar claro o que é
  nosso e o que veio do upstream.

## Acompanhar o upstream

O remote `upstream` aponta para `oliexdev/openScale`:

```bash
git fetch upstream
git log master..upstream/master --oneline   # o que há de novo
```

Manter nossas mudanças em arquivos próprios (`Design*.kt`) reduz conflito ao
puxar atualizações. `Theme.kt` é o ponto de contato inevitável — ali o conflito
é esperado e pequeno.

## Convenções de git — obrigatórias

### Branches: uma por tarefa (Conventional Branch)

**Sempre criar uma branch nova antes de começar.** Nunca trabalhar direto na
`master`. Uma branch = uma tarefa.

```
<tipo>/<descrição-em-kebab-case>
```

Tipos: `feat/` · `fix/` · `chore/` · `docs/` · `refactor/` · `test/` · `release/`

```
feat/tela-hoje
fix/contraste-chip-variacao
docs/fundacao-compose
chore/atualizar-upstream
```

Sem acento, sem maiúscula, sem `_`. Descrição curta e específica.

### Commits: Conventional Commits, sempre atômicos

```
<tipo>(<escopo opcional>): <descrição no imperativo>

[corpo opcional explicando o porquê]
```

Tipos: `feat` · `fix` · `docs` · `style` · `refactor` · `perf` · `test` ·
`build` · `ci` · `chore` · `revert`

Escopos úteis: `design`, `tema`, `overview`, `graph`, `table`, `settings`,
`bluetooth`, `db`, `build`.

```
feat(design): aplicar paleta e tipografia do design no 3.1.3
fix(overview): corrigir contraste do chip de variacao no tema escuro
chore(build): atualizar AGP
```

**Atômico significa:** um commit faz **uma coisa só** e deixa o repositório num
estado coerente. Se a mensagem precisa de "e" para descrever o que foi feito,
são dois commits.

Regras de escrita:
- Descrição no **imperativo**: "adicionar", não "adicionado".
- Minúscula inicial, sem ponto final, até ~72 caracteres.
- O corpo explica **por que**, não o quê (o diff já mostra o quê).

### Commits que não devem existir

- Misturar mudança visual com mudança em `core/`.
- Tocar em traduções do Weblate.
- Commit "wip" ou "ajustes" — se não dá para nomear, não dá para commitar.

### Fluxo

```bash
git switch -c feat/tela-hoje          # 1. branch nova, a partir da master
# ... trabalho, em commits atômicos ...
git push -u origin feat/tela-hoje     # 2. publica
# 3. abre PR
```

Commitar e fazer push só quando a pessoa pedir.
