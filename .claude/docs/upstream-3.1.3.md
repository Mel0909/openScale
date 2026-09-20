# Atualização para o openScale 3.1.3 (upstream)

Branch: **`feat/upstream-3.1.3`**, criada em 2026-09-20 a partir de
`oliexdev/openScale@master`. Compila nesta máquina — APK gerado e verificado.

## Por que isso foi feito

A balança da Mel ("Yoda0", protocolo okok) aparecia como **não suportada**. A
causa não era bug do redesign: o fork estava parado em 2020 e simplesmente não
tinha o driver. O upstream tem — `OkOkHandler.kt` reconhece `Yoda0` e `Yoda1`
por nome.

## O que mudou

O fork estava 1045 commits atrás. Não é a mesma base com correções: é uma
reescrita.

| | Fork (2.3.5) | Upstream (3.1.3) |
|---|---|---|
| Linguagem | Java | **Kotlin** (0 arquivos .java) |
| UI | XML + Fragments | **Jetpack Compose** (0 layouts XML) |
| minSdk | 21 (Android 5) | **31 (Android 12)** |
| Injeção de dependência | nenhuma | **Hilt** |
| Build | Groovy | **Kotlin DSL** + version catalog |
| Drivers de balança | 24 | **72** |
| Gradle | 9.1 | 9.4.1 |
| compileSdk | 35 | 37 |

## Consequência para o redesign

**As telas do `feat/redesign-material3` não se aplicam a esta base.** Elas são
layouts XML e Fragments Java; aqui não existe `res/layout` nem Fragment.

O que sobrevive e continua valendo:

- **`design/`** — protótipo e tokens. É a especificação, independe de tecnologia.
- **A paleta M3** — os 24 slots já estão traduzidos e verificados em contraste;
  em Compose viram um `ColorScheme`, o mapeamento é quase direto.
- **A escala tipográfica** — vira `Typography`.
- **As decisões de produto** registradas em `decisoes-implementacao.md` e as
  lacunas de `inventario-legado.md`.

O que precisaria ser refeito: as cinco telas, os sheets e os componentes, em
Compose.

**Atenção:** o 3.1.3 já vem com a própria UI, que é moderna e completa. Antes de
reimplementar o design por cima dela, vale olhar como ela está — pode ser que
só a paleta e a tipografia já entreguem boa parte do resultado desejado.

## O que perde ao adotar esta base

- **Aparelhos abaixo do Android 12** deixam de ser suportados (minSdk 21 → 31).
- O trabalho de UI das noites de 19–20/09, na forma em que está.

## O que ganha

- A balança okok funcionando, e mais 48 drivers.
- ~5 anos de correções, incluindo na camada Bluetooth.
- Base em manutenção ativa: fica fácil acompanhar o upstream daqui para frente.

## Branches

| Branch | O que é |
|---|---|
| `master` | fork original, 2.3.5, intocado |
| `chore/modernizar-build` | AGP 8.13 + Gradle 9.1 sobre a base antiga |
| `feat/redesign-material3` | o redesign em XML/Java sobre a base antiga |
| `feat/upstream-3.1.3` | **openScale 3.1.3 puro**, sem nenhuma alteração nossa |

Nada foi sobrescrito: todas continuam existindo e funcionando.

## Próximo passo (decisão pendente)

1. Instalar o APK desta branch e **confirmar que a balança conecta**. É o teste
   que motivou tudo.
2. Ver a UI do 3.1.3 e decidir: aplicar os tokens do design sobre ela, ou
   reimplementar as telas do protótipo em Compose.
3. Se a decisão for adotar esta base, a `master` do fork pode ser realinhada ao
   upstream — mas isso é passo separado, e as branches antigas devem ser
   preservadas até que você tenha certeza.

## Detalhe técnico

O remote `upstream` foi adicionado ao repositório:

```
git remote -v     # origin = seu fork, upstream = oliexdev/openScale
```

Para remover: `git remote remove upstream` (sem efeito sobre o código).
