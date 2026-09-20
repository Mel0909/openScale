# O design sobre a base 3.1.3

Fonte de verdade: a pasta [`design/`](../../design/) na raiz.

| Arquivo | O que é |
|---|---|
| `Tokens.dc.html` | Fundação: paleta M3 (claro + escuro), escala tipográfica, cores de gráfico. **Etapa 1 de 4.** |
| `App.dc.html` | Protótipo navegável das telas. Abrir no navegador; tem alternador claro/escuro. |
| `android-frame.jsx` | Moldura de device. Scaffold, não é conteúdo. |
| `support.js` | Runtime do formato `.dc.html`. Não editar. |

## O conceito

Rosa profundo como cor de **dado e de ação**, sobre fundos quentes levemente
rosados. A regra declarada no design: *"o rosa nunca preenche a tela — ele
aparece no número, na linha do gráfico, no botão e na variação"*.

Duas famílias tipográficas:
- **Bricolage Grotesque** (800/700) — só números grandes e títulos curtos.
- **Instrument Sans** (400/500/600) — todo o resto.

Sem sombra em nenhum tema: no claro a hierarquia vem de `surface` mais claro que
`background`; no escuro, de borda de 1px. Decisão explícita para evitar blur e
elevação custosos.

Todos os pares têm **contraste WCAG AA verificado** na fonte.

---

## ✅ O que já está aplicado

### Paleta — `ui/theme/DesignColor.kt`

Os 24 slots do Material 3, claro e escuro, com a razão de contraste anotada em
cada um. Mais `DesignChartColors`, para os gráficos.

Ligada em `Theme.kt` através de `designLightScheme` / `designDarkScheme`.

**O dynamic color foi desligado de propósito.** Ele deixaria o Android
sobrescrever a paleta com as cores do papel de parede, anulando o design. Os
esquemas de **alto contraste** do upstream continuam funcionando, porque são
acessibilidade, não estética.

### Tipografia — `ui/theme/DesignType.kt`

A escala do design mapeada nos slots M3:

| Token do design | Slot M3 | Spec |
|---|---|---|
| Número herói | `displayLarge` | Bricolage 800 · 64sp · -0.035em |
| Número secundário | `headlineMedium` | Bricolage 700 · 34sp · -0.02em |
| Título de tela | `headlineSmall` | Bricolage 700 · 26sp |
| Título de seção | `titleMedium` | Instrument 600 · 18sp |
| Corpo | `bodyLarge` | Instrument 400 · 15sp |
| Rótulo | `labelLarge` | Instrument 500 · 13sp |
| Rótulo micro | `labelSmall` | Instrument 600 · 11sp · +0.09em |

Os slots que o design não define herdam o padrão do Material **com a família
trocada**, para nada ficar em Roboto por descuido.

### Fontes — `res/font/`

Empacotadas no APK (592 KB), baixadas do repositório oficial do Google Fonts.
Ambas OFL.

**Não usam Downloadable Fonts** de propósito: elas dependem do Google Play
Services e cairiam no fallback no F-Droid, que é um canal importante deste app.

São fontes **variáveis**, então um arquivo por família cobre toda a escala de
pesos — declarar `FontWeight` em `Font()` basta.

### Efeito prático

Todo o app usa a paleta e as fontes: telas, diálogos, navegação, widget. No
Compose tudo lê do `MaterialTheme`, então não há como uma tela escapar — bem
diferente do código antigo, onde a cor vinha de quatro lugares.

---

## 🔲 O que falta: a estrutura das telas

A paleta mudou; a **forma** das telas ainda é a do upstream.

### A diferença concreta

O protótipo desenha a tela inicial assim:

```
cabeçalho (chip de usuário + ajustes)
peso herói 84sp + chip de variação
cartão de sparkline, clicável
grade 2 colunas de cartões de métrica
```

A `OverviewScreen` do upstream (1290 linhas) tem outra forma, com funcionalidades
que o app de 2020 não tinha e o protótipo não previu:

- agregação por período (dia/semana/mês/ano)
- drill-down: tocar num período abre as medições dele
- splitter arrastável entre gráfico e lista
- avaliação por faixa, com banner de erro
- linhas de medição expansíveis

**Reescrever no formato do protótipo significa decidir o que fazer com essas
funcionalidades** — preservá-las dentro do novo layout, ou cortá-las.

### Telas do protótipo × telas da base

| Protótipo | Base 3.1.3 | Situação |
|---|---|---|
| Hoje | `overview/OverviewScreen.kt` | forma diferente |
| Histórico | `graph/` | forma diferente |
| Medições | `table/` | forma diferente |
| Perfil | dentro de `settings/` | forma diferente |
| Ajustes | `settings/` | forma diferente |
| — | `insights/` | **não existe no protótipo** |
| — | `statistics/` | **não existe no protótipo** |

### Ordem sugerida, quando for atacar

1. **Overview** — é a tela que mais define a percepção do app, e a que mais
   difere. Decidir antes o que fazer com agregação e drill-down.
2. **Componentes compartilhados** (`ui/screen/components/`) — cartão, chip de
   variação, linha de métrica. Mexer aqui propaga para várias telas de uma vez.
3. Demais telas, uma por vez.

Cada uma em sua branch, conforme as convenções do CLAUDE.md.

---

## Como estender sem quebrar o merge com o upstream

O upstream é ativo. Para conseguir puxar atualizações:

- **Código do design em arquivos próprios**, com prefixo `Design`.
  Já são `DesignColor.kt` e `DesignType.kt`.
- **Não editar `Color.kt` e `Type.kt`** do upstream. Estão intocados de
  propósito, mesmo sem uso — são o que o upstream vai atualizar.
- `Theme.kt` é o ponto de contato inevitável. O conflito ali é esperado e
  pequeno: são as linhas que escolhem o esquema e a tipografia.
- Ao reescrever uma tela, preferir **um arquivo novo** (`DesignOverviewScreen.kt`)
  e trocar a rota, em vez de editar a original. Assim o upstream continua
  mesclando limpo e dá para comparar as duas.

---

## Pendências de decisão

Herdadas do trabalho anterior, ainda válidas:

1. **Meta de peso** — no fork antigo vivia na tela Statistics. A base nova tem
   `statistics/` e agora também metas com data de início. Ver como o protótipo
   se encaixa.
2. **Paleta azul alternativa** — o protótipo tem um seletor rosa/azul, mas o
   `Tokens.dc.html` só documenta a rosa, com contraste verificado. A azul
   precisaria ser verificada antes de existir.
3. **"Últimos 30 dias"** — o rótulo do sparkline. Trinta dias de calendário ou
   as últimas trinta medições? São coisas diferentes para quem pesa uma vez por
   semana.
4. **`insights/` e `statistics/`** — telas que o protótipo não previu porque
   não existiam. Ficam como estão, ganham tratamento do design, ou saem?
