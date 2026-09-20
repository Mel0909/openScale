# `ui/design/` — o design próprio

Esta pasta é **o design deste fork**, implementado a partir de
[`design/`](../../../../../../../../../design/) na raiz do repositório.

## A regra

> **`design/` na raiz é a fonte de verdade.** O que não está desenhado lá não
> se inventa aqui.

Se o protótipo não cobre um caso, vale a regra declarada nele:

> *"o rosa nunca preenche a tela — ele aparece no número, na linha do gráfico,
> no botão e na variação"*

E há uma segunda referência, para os casos que o protótipo não desenha: a
branch **`feat/redesign-material3`**, que implementou este mesmo design em
Java/XML sobre a base antiga (2.3.5). Ela já resolveu lacunas como estado
vazio, teto de pontos do sparkline e curva suavizada — vale consultar antes de
decidir sozinho:

```bash
git show feat/redesign-material3:android_app/app/src/main/res/layout/rd_fragment_today.xml
git show feat/redesign-material3:android_app/app/src/main/java/com/health/openscale/gui/redesign/TodayFragment.java
```

Os arquivos de lá têm prefixo `rd_`.

## Por que uma pasta separada

`ui/screen/` é o código do **upstream** (`oliexdev/openScale`). Ele continua lá,
intocado, e continua mesclando limpo quando puxamos atualizações.

`ui/design/` é **nosso**. Nenhum *componente visual* do upstream é
reaproveitado aqui. A separação é física, não uma convenção de nome: dá para
verificar com

```bash
grep -rn "import com.health.openscale.ui.screen" ui/design/
```

O resultado esperado são **só os dois ViewModels** que a casca precisa
instanciar (`SettingsViewModel`, `BluetoothViewModel`). Eles são estado e
dados, não visual — a mesma fronteira que o `SharedViewModel`. Qualquer
import de um `Composable` de `ui/screen/` é um vazamento e deve sair.

As duas pastas se encontram só em dois lugares:

| Ponto de contato | O que é |
|---|---|
| `ui/navigation/AppNavHost.kt` | a rota escolhe qual tela desenhar |
| `ui/shared/SharedViewModel.kt` | de onde os dados vêm, para as duas |
| `ui/theme/` | a paleta e a tipografia, já aplicadas |

## O que tem aqui

```
design/
├── DesignApp.kt             a casca: barra inferior de 4 abas + botão Pesar
├── DesignTokens.kt          medidas lidas do protótipo (raio, espaço, tamanho)
├── components/              um componente por elemento que o protótipo desenha
│   ├── DesignComponents.kt    superfície, rótulo micro, herói, chip, cartão de métrica…
│   └── DesignSparklineCard.kt cartão "Últimos 30 dias"
└── screen/                  uma tela por tela do protótipo
    └── HojeScreen.kt          tela Hoje
```

### A casca é metade do trabalho

O que faz o app *parecer* o design não é só o conteúdo das telas — é a
moldura. O upstream e o design divergem aqui:

| | upstream (`ui/navigation/AppNavigation.kt`) | design (`DesignApp.kt`) |
|---|---|---|
| navegação | menu lateral (drawer), 6 destinos | barra inferior, 4 abas |
| título | `TopAppBar` comum a todas | cada tela traz o seu |
| ação principal | ícones na barra superior | botão "Pesar" flutuante |

Quem escolhe a casca é a `MainActivity`. As quatro abas são as do protótipo:
Hoje · Histórico · Medições · Perfil.

## Escopo atual

As telas aqui têm **exatamente** o que o protótipo desenha — nada além.

Funcionalidades do upstream que o protótipo não previu (agregação por período,
drill-down, avaliação por faixa, linhas expansíveis, metas) **não estão aqui**.
Elas seguem na `ui/screen/overview/OverviewScreen.kt`, que continua no
repositório. A decisão foi entregar primeiro o design exato e só depois decidir,
olhando, o que vale trazer de volta e em que forma.

## Ao acrescentar uma tela

1. Ler o `sc-if` correspondente no `design/App.dc.html`.
2. Criar `ui/design/screen/<Nome>Screen.kt`.
3. Apontar a rota em `AppNavHost.kt` — **não** editar a tela do upstream.
4. Medidas novas vão para `DesignTokens.kt`, com o valor do protótipo no
   comentário.
5. Cor sempre de `MaterialTheme.colorScheme`; tipografia de
   `MaterialTheme.typography`, ou de `DesignTokens.Type` quando o protótipo
   usar um tamanho que a escala não cobre.
