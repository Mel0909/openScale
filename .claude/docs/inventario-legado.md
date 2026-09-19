# Inventário do app antigo

**Propósito deste documento:** a UI vai ser refeita do zero com o Claude Design
(ver `design/`). Este arquivo **não** é um guia de como mexer no visual antigo —
é a lista do que o app faz hoje, para que nada se perca na reconstrução.

Use como checklist: cada item aqui precisa ter destino no design novo — reimplementado,
repensado ou **deliberadamente** cortado. O que não pode acontecer é sumir por descuido.

Legenda de cobertura pelo design novo (`design/App.dc.html`):
**✅ coberto** · **🟡 parcial** · **❌ ausente** · **⚙️ lógica pura** (sem UI, não muda)

---

## 1. Telas atuais → telas novas

O app antigo tem 4 abas. O design novo também tem 4, mas **não são as mesmas**:

| Antigo (bottom nav) | Novo (bottom nav) | Observação |
|---|---|---|
| Overview | **Hoje** | vira tela de peso-herói + métricas em grade |
| Graph | **Histórico** | gráfico com chips de métrica + períodos |
| Table | **Medições** | lista agrupada por mês, com sheet de detalhe |
| Statistics | — | **dissolvida**: meta e estatísticas precisam de destino |
| (drawer) Preferences | **Perfil** + Ajustes | perfil vira aba; ajustes fica em tela separada |

**Decisão pendente #1:** a tela Statistics não existe no design novo. Ela tem hoje
meta de peso, dias restantes, diferença desde a última medição e "fatias" do corpo.
Parte disso cabe em Hoje ou Histórico — mas precisa de uma decisão explícita.

**Decisão pendente #2:** o **drawer lateral** desaparece no design novo (que usa só
bottom nav + header). Os itens que só existiam no drawer precisam de novo lar:
doação, ajuda/wiki, e o acesso a Ajustes (este já resolvido pelo ícone no header de Hoje).

---

## 2. Métricas — o coração do app

São **25 tipos de medição**, cada uma podendo ser ligada/desligada e reordenada.

| Métrica | Origem | No design novo |
|---|---|---|
| Peso | balança | ✅ herói em Hoje |
| IMC | calculado | ✅ |
| Água | balança/estimada | ✅ |
| Músculo | balança | ✅ |
| Massa magra (LBM) | estimada | ✅ toggle |
| Gordura | balança/estimada | ✅ |
| Massa óssea | balança | ✅ toggle |
| Gordura visceral | balança | 🟡 só na lista de toggles genérica |
| Cintura | manual | ✅ campo manual |
| Cintura-altura (WHtR) | calculado | ❌ |
| Quadril | manual | ✅ campo manual |
| Cintura-quadril (WHR) | calculado | ❌ |
| Peito, Coxa, Bíceps, Pescoço | manual | 🟡 só Pescoço aparece |
| Caliper 1/2/3 + Gordura por caliper | manual | ❌ |
| TMB (BMR) | calculado | ✅ |
| TDEE | calculado | ❌ |
| Calorias | calculado | ❌ |
| Comentário | manual | ✅ "anotação" no detalhe |
| Data / Hora | — | ✅ |
| Usuário | — | ✅ seletor |

**Decisão pendente #3:** o design novo mostra ~9 métricas; o app tem 25. As faltantes
(calipers, TDEE, calorias, WHR, WHtR, circunferências) são de nicho, mas **são usadas**
— tirar é uma escolha de produto legítima, desde que consciente. O padrão do app antigo
é vir com poucas ligadas e o resto disponível em Ajustes → Medições.

### Comportamentos por métrica que precisam sobreviver

- **Ligar/desligar** cada métrica (preference por chave).
- **Reordenar** livremente (preference `measurementOrder`, drag & drop).
- **Estimativa automática** quando a balança não fornece: escolha de fórmula por
  métrica (gordura: Gallagher / Gallagher asiático / Deurenberg I e II / Eddy;
  LBM: Boer / Hume / peso-menos-gordura; água: Behnke / Hume-Weyers / Lee-Song-Kim /
  Delwaide-Crenier). ✅ o design cobre isso em Ajustes → fórmulas.
- **Avaliação LOW / NORMAL / HIGH** por idade e sexo → hoje é a barra colorida e o
  gauge expansível. 🟡 no design novo existe "faixa de referência" no gráfico,
  mas não o equivalente por linha.
- **Delta vs. medição anterior** ✅ (chip de variação).
- **Conversão de unidade**: kg / lb / st e cm / in, por usuário. ⚙️ lógica,
  mas a UI precisa respeitar.
- **Eixo esquerdo ou direito** no gráfico, por métrica. ❌ não aparece no design.

---

## 3. Funcionalidades por área

### Usuários ✅
Múltiplos perfis, cada um com nome, nascimento, sexo, altura, unidades, meta de peso
e data-alvo. Troca rápida por seletor. Design novo cobre bem (sheet de usuários + Perfil).

**Atenção:** o app antigo também tem **assistente de usuário na balança**
(`smartUserAssign`) — atribui automaticamente a medição ao perfil mais provável.
❌ não aparece no design novo, mas é ajuste, não tela.

### Bluetooth 🟡
- Escaneamento e escolha da balança (lista de dispositivos encontrados, com ícone
  indicando suportado/não suportado).
- **24 drivers** de fabricantes diferentes.
- Status de conexão vive hoje **no ícone da toolbar** (buscando / conectado /
  perdido / desabilitado / não suportado).
- Preferences: `btEnable`, `mergeWithLastMeasurement`, `smartUserAssign`,
  `ignoreOutOfRange`.
- Tela de **debug Bluetooth** para reportar balanças novas (usada pela comunidade
  para adicionar suporte).

O design novo tem o sheet "Pesar agora / Aguardando a balança…" ✅, que é a parte
principal. Mas **falta**: onde mora o status persistente de conexão, a tela de
escaneamento/pareamento, e o debug. São telas de Ajustes — não bloqueiam o começo.

### Dados: importar/exportar 🟡→❌
- Export/import **CSV** (a coluna de cada métrica).
- Export/import de **backup do banco** (arquivo .db).
- **Backup automático** agendado, com opção de sobrescrever e diretório configurável.
- **ContentProvider** que expõe os dados a outros apps sob permissão `dangerous`. ⚙️

❌ nada disso aparece no design novo. É tela de Ajustes, mas é funcionalidade
importante (é um dos pontos que o README vende: "respeita sua privacidade e deixa
você decidir o que fazer com seus dados").

### Lembretes ❌
Notificação para pesar, com dias da semana, horário e texto customizável.
Não aparece no design novo. Tela de Ajustes.

### Widget de tela inicial ❌
Widget redimensionável mostrando a última medição, configurável por métrica e usuário.
Vive fora do app (`WidgetProvider` + `widget.xml`). **Tem visual próprio** que também
ficaria datado se o app todo mudar — vale decidir se entra no escopo.

### Onboarding 🟡
7 slides na primeira execução (boas-vindas, open source, privacidade, métricas,
usuário, bluetooth, apoie). Usa a lib AppIntro. Design novo não trata.

### Gráfico — opções ✅/🟡
Preferences: legenda, rótulos de valor, pontos, **linha de meta**, **linha de tendência**
(regressão polinomial), previsão. Range: dia / semana / mês / ano.
Design novo tem períodos (semana/mês/ano ✅) e faixa de referência, mas não expõe
meta/tendência como toggles — e **linha de meta é funcionalidade querida** (o app
inteiro tem conceito de "meta de peso" com data-alvo).

### Outros
- **Idioma**: ~30 traduções via Weblate, com seletor no app. ✅ manter (não editar
  `values-*/` à mão).
- **Tema**: claro / escuro / seguir sistema. ✅ o design novo nasce com os dois.
- **Confirmação ao excluir** (preference). 🟡
- **Diálogo de feedback** no 15º lançamento. Provavelmente cortar.
- **Tela de crash** (CustomActivityOnCrash). ⚙️
- **Debug log**. ⚙️

---

## 4. O que é intocável

`core/` inteiro. Em particular:

- **`OpenScale.getInstance()`** — a fachada que toda a UI nova vai consumir.
  Principais entradas: `getScaleMeasurementsLiveData()` (LiveData que as telas
  observam), `getScaleUserList()`, `getSelectedScaleUser()`, `addScaleMeasurement()`,
  `updateScaleMeasurement()`, `deleteScaleMeasurement()`, `connectToBluetoothDevice()`.
- **`ScaleMeasurement`** e **`ScaleUser`** — os dois modelos de dados.
- **`EvaluationSheet`** — devolve LOW/NORMAL/HIGH + limites, por idade e sexo.
  É o que alimenta "faixa de referência" no design novo.
- **`Converters`** — kg/lb/st, cm/in.
- Os 24 drivers Bluetooth e as fórmulas de `bodymetric/`.

A UI nova troca **o que vem depois** desses pontos. A fronteira é limpa: nenhuma
tela precisa falar com o banco diretamente.

---

## 5. Restrições técnicas herdadas

Estas não são opinião de design — são limites reais do projeto hoje:

- **Java, minSdk 21, AGP 4.1, Gradle 6.5.** Sem Compose (exigiria AGP 7+,
  Kotlin e uma migração grande). O design novo é implementável em XML + Material
  Components, mas não em Compose sem antes modernizar o build.
- **Tema herda de `Theme.AppCompat.DayNight`**, não de Material Components — os
  slots M3 do design (`primaryContainer`, `surfaceVariant`…) **não existem** nesse
  parent. Trocar o parent para `Theme.Material3.*` é pré-requisito, e exige subir
  a lib `com.google.android.material` (hoje em `1.3.0-alpha04`, antiga demais para M3).
- **Sem ViewBinding.** Habilitar é barato e vale a pena numa reconstrução.
- **Fontes**: o design pede Bricolage Grotesque + Instrument Sans. Ambas no Google
  Fonts e empacotáveis via `res/font/`. Pesa no APK — medir.
- **MPAndroidChart 3.1.0** é a lib de gráfico. O design já especifica cores
  pensando nela (`gridColor`, `axisLineColor`, `LimitLine`) ✅.
- **Sem `res/values-night/`** hoje. O design novo tem paleta escura completa,
  então esse diretório passa a existir.
- **Build local não roda** (JDK 25 vs. requisito 8–11, sem Android SDK).
  Ver `.claude/docs/build.md`.
- **Testes Espresso** em `androidTest/.../gui/` dependem de ids (`saveButton`,
  `float_input`, `action_add_measurement`). Vão quebrar na reconstrução —
  esperado, mas atualizar junto.

---

## 6. Oportunidades que o app antigo já tinha e vale preservar

Coisas boas do legado que seria fácil perder sem querer:

1. **Reordenar métricas por drag & drop** — controle raro e bem-feito.
2. **Escolha de fórmula de estimativa** — o design novo manteve ✅, é um diferencial
   real frente a apps proprietários.
3. **Expandir uma métrica para ver a faixa saudável** (o gauge) — conceito bom,
   mesmo com execução datada. Vale repensar, não descartar.
4. **Funciona sem conta, sem nuvem, dados exportáveis** — é a proposta do app.
   Import/export não é acessório.
5. **Entrada manual rápida**, com botões +/- e "próximo" encadeando as métricas
   numa sequência só. Fluxo eficiente para quem mede várias coisas.
6. **Mesclar com a última medição** (`mergeWithLastMeasurement`) — pesou na balança
   e depois mediu a cintura à mão, vira um registro só.
