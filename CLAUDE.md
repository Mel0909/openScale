# openScale — guia do repositório

App Android **nativo em Java** (sem Kotlin no código do app, sem Compose, sem ViewBinding)
para registrar peso e métricas corporais, com leitura automática via balanças Bluetooth LE.
Licença GPLv3. Upstream: https://github.com/oliexdev/openScale

## Estado atual do trabalho

**A UI está sendo refeita do zero com um design novo.** Não é um retoque do visual
antigo: a interface existente será substituída, incluindo a estrutura de navegação.

A lógica de domínio (`core/`: cálculos corporais, protocolos Bluetooth, banco de
dados) **permanece intacta** — a fronteira é `OpenScale.getInstance()`.

Três documentos guiam o trabalho:

| Documento | Para quê |
|---|---|
| [.claude/docs/design-novo.md](.claude/docs/design-novo.md) | O design a construir: tokens, paleta, telas, pré-requisitos técnicos |
| [.claude/docs/inventario-legado.md](.claude/docs/inventario-legado.md) | O que o app antigo faz, para nada se perder na reconstrução |
| [.claude/docs/balancas-bluetooth.md](.claude/docs/balancas-bluetooth.md) | **Ler antes de qualquer coisa que toque em pesagem.** Falha em silêncio. |
| [.claude/docs/build.md](.claude/docs/build.md) | Build e como validar sem conseguir compilar |

A fonte de verdade do design é a pasta [`design/`](design/) na raiz
(`Tokens.dc.html` e `App.dc.html` — abrir no navegador).

## Layout do repositório

| Pasta | O que é |
|---|---|
| `design/` | **Design novo.** Protótipo navegável + tokens. Fonte de verdade do visual. |
| `android_app/` | O projeto Gradle do app. É aqui que praticamente todo o trabalho acontece. |
| `arduino_mcu/` | Firmware da "balança caseira" open hardware (C++/Arduino). Não relacionado ao app. |
| `docs/` | Imagens: screenshots, ícones-fonte, fotos de balanças, docs de protocolo. |
| `fastlane/` | Metadados de loja (F-Droid/Play): descrições, changelogs, screenshots por idioma. |
| `.github/` | Templates de issue. |
| `.travis.yml` | CI legada (Travis). Compila debug/release e roda testes. |

## Dentro de `android_app/app/src/main/`

### `java/com/health/openscale/core/` — domínio, **não mexer no trabalho de UI**

- `OpenScale.java` — **singleton central**. Fachada para banco, usuários, medições,
  import/export CSV, conexão Bluetooth. Toda a UI passa por `OpenScale.getInstance()`.
  Expõe `getScaleMeasurementsLiveData()`, que é o que os fragments observam.
- `database/` — Room. `AppDatabase` (version 5, migrações 1→5), DAOs, e um
  `ScaleDatabaseProvider` (ContentProvider que expõe os dados a outros apps sob
  permissão `dangerous`).
- `datatypes/` — `ScaleMeasurement` (uma pesagem: peso, gordura, água, músculo,
  circunferências, calipers, comentário…) e `ScaleUser` (perfil: nome, nascimento,
  sexo, altura, meta, unidade de medida).
- `bluetooth/` — 24 drivers, um por fabricante/protocolo, todos herdando de
  `BluetoothCommunication`. `BluetoothFactory` mapeia nome do device → driver.
  `lib/` guarda os algoritmos proprietários de composição corporal de cada marca.
  ⚠️ **Nunca editar.** Erros aqui não dão crash — a balança só para de funcionar,
  e só se descobre com hardware real. Ver
  [.claude/docs/balancas-bluetooth.md](.claude/docs/balancas-bluetooth.md).
- `bodymetric/` — fórmulas estimativas quando a balança não fornece o dado
  (gordura: Deurenberg/Eddy/Gallagher; LBM: Boer/Hume; água: Behnke/Hume-Weyers…).
- `evaluation/` — `EvaluationSheet` classifica um valor em LOW / NORMAL / HIGH conforme
  idade/sexo. É o que alimenta as cores dos indicadores e o gauge na UI.
- `alarm/` — lembretes de pesagem e backup automático agendado.
- `utils/` — `Converters` (kg/lb/st, cm/in), `CsvHelper`, `PolynomialFitter`
  (usado na linha de tendência/previsão do gráfico).

### `java/com/health/openscale/gui/` — **toda a UI**

- `MainActivity.java` (~860 linhas) — única Activity de verdade. Monta Toolbar,
  DrawerLayout (menu lateral), BottomNavigationView, e o NavHostFragment.
  Também aplica tema (`app_theme` = Light/Dark), idioma, e o status do Bluetooth
  no menu de ação.
- `overview/OverviewFragment.java` — tela inicial: seletor de usuário, gráfico,
  e a tabela com a última medição.
- `graph/GraphFragment.java` — gráfico anual com BarChart (meses) + LineChart.
- `table/TableFragment.java` — RecyclerView com todas as medições.
- `statistic/StatisticsFragment.java` — meta, diferenças, "fatias" do corpo.
- `measurement/` — o coração da UI. Ver seção abaixo.
- `preferences/` — telas de configuração (PreferenceFragmentCompat + `res/xml/*.xml`).
- `slides/` — onboarding (biblioteca AppIntro), primeira execução.
- `widget/` — widget de tela inicial.
- `utils/ColorUtil.java` — paleta global hardcoded e `getTintColor()` (claro/escuro).

### O pacote `measurement/` — a parte que mais muda

`MeasurementView` é uma `TableLayout` abstrata que **constrói a si mesma em código Java,
sem XML**, com paddings em pixels brutos. Há uma subclasse por métrica
(`WeightMeasurementView`, `FatMeasurementView`, …), cada uma com cor hardcoded
em `getColor()`.

É o componente mais visível do app antigo e não sobrevive ao redesign na forma atual.
Mas **a lógica dentro dele precisa migrar**: `FloatMeasurementView` concentra conversão
de unidade, delta vs. medição anterior, avaliação LOW/NORMAL/HIGH e estimativa
automática por fórmula. Ler antes de reescrever.

### `res/` — o que permanece e o que sai

**Permanece:**
- `values/strings.xml` — fonte de verdade dos textos (inglês).
- `values-XX/` — ~30 traduções do **Weblate**. **Nunca editar à mão.**
  Chaves removidas na reconstrução ficam órfãs; chaves novas entram só em `values/`.
- `drawable/ic_*.xml` — ~70 ícones vetoriais. Reaproveitáveis, mas o design novo
  usa ícones de traço mais fino (ver SVGs inline no protótipo).
- `xml/*_preferences.xml` — inventário útil do que é configurável hoje.

**Sai / é reescrito:** `layout/`, `values/styles.xml`, `values/colors.xml`,
`navigation/mobile_navigation.xml`, `menu/`.

## Build

`versionCode 54` / `versionName 2.3.5`, minSdk 21, targetSdk 29, AGP 4.1, Gradle 6.5.

Três build types além do debug — `release`, `light` (sem link de doação) e `pro` —
que só diferem em ícone, applicationId suffix e keystore.

**Atenção ao ambiente local:** o Gradle 6.5 / AGP 4.1 deste projeto exige **JDK 8–11**.
A máquina tem JDK 25 e nenhum Android SDK detectado, então `./gradlew` não roda aqui
sem configuração adicional. Mudanças de UI precisam ser revisadas por leitura de código
ou compiladas no Android Studio. Ver [.claude/docs/build.md](.claude/docs/build.md).

## Convenções de código

- Java 8, estilo do upstream: 4 espaços, chaves na mesma linha.
- Cabeçalho de licença GPL em todo arquivo novo `.java`.
- Logging via **Timber**, não `android.util.Log`.
- Strings sempre em `res/values/strings.xml`, nunca hardcoded na UI.
- Preferências lidas com `PreferenceManager.getDefaultSharedPreferences(context)`.

## Convenções de git — obrigatórias

### Branches: uma por tarefa (Conventional Branch)

**Sempre criar uma branch nova antes de começar.** Nunca trabalhar direto na `master`.
Uma branch = uma tarefa. Terminou a tarefa, abre PR, fecha a branch.

```
<tipo>/<descrição-em-kebab-case>
```

Tipos: `feat/` · `fix/` · `chore/` · `docs/` · `refactor/` · `test/` · `release/`

```
feat/tela-hoje
feat/tokens-material3
fix/contraste-chip-variacao
refactor/migrar-merge-para-core
docs/inventario-legado
chore/subir-lib-material
```

Sem acento, sem maiúscula, sem `_`. Descrição curta e específica —
`feat/tela-hoje`, não `feat/nova-tela`.

### Commits: Conventional Commits, sempre atômicos

```
<tipo>(<escopo opcional>): <descrição no imperativo>

[corpo opcional explicando o porquê]

[rodapé opcional: BREAKING CHANGE, refs]
```

Tipos: `feat` · `fix` · `docs` · `style` · `refactor` · `perf` · `test` ·
`build` · `ci` · `chore` · `revert`

Escopos úteis neste projeto: `tokens`, `tema`, `hoje`, `historico`, `medicoes`,
`perfil`, `ajustes`, `grafico`, `bluetooth`, `db`, `build`.

```
feat(tokens): adicionar paleta rosa M3 em colors.xml
feat(tokens): adicionar variante escura em values-night
feat(hoje): criar layout da tela inicial com peso herói
fix(grafico): corrigir cor do eixo no tema escuro
refactor(bluetooth): mover regra de merge de MainActivity para OpenScale
chore(build): subir com.google.android.material para 1.5.0
docs: documentar regras da camada bluetooth
```

**Atômico significa:** um commit faz **uma coisa só** e deixa o repositório num
estado coerente. Se a mensagem precisa de "e" para descrever o que foi feito,
são dois commits.

- ❌ `feat: nova tela Hoje e ajustes de cor e correção do gráfico`
- ✅ três commits separados

Regras de escrita:
- Descrição no **imperativo**: "adicionar", não "adicionado" nem "adiciona".
- Minúscula inicial, sem ponto final.
- Até ~72 caracteres na primeira linha.
- O corpo explica **por que**, não o que (o diff já mostra o que).
- `BREAKING CHANGE:` no rodapé quando quebrar compatibilidade.

### Commits que não devem existir

- Misturar mudança de UI com mudança em `core/` — separar sempre.
- Tocar em `res/values-*/` (Weblate). Só `values/strings.xml`.
- Commit "wip" ou "ajustes" — se não dá para nomear, não dá para commitar.

### Fluxo

```bash
git switch -c feat/tela-hoje          # 1. branch nova, a partir da master
# ... trabalho, em commits atômicos ...
git push -u origin feat/tela-hoje     # 2. publica
# 3. abre PR
```

Commitar e fazer push só quando a pessoa pedir.
