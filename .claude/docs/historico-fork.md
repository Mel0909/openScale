# De onde viemos

Contexto do que aconteceu antes da base atual. Serve para entender decisões que
parecem estranhas fora de contexto, e para achar trabalho antigo se for preciso.

## A linha do tempo

**Até 19/09/2026** — o fork estava na versão **2.3.5**, um snapshot do openScale
de 2020: Java, layouts XML, `minSdk 21`, 24 drivers de balança.

**19–20/09** — um redesign completo da UI foi construído sobre essa base: tokens
Material 3, cinco telas novas em XML, bottom nav sem drawer. Junto veio uma
migração de build (AGP 4.1 → 8.13, Gradle 6.5 → 9.1), porque o Android Studio
atual recusa AGP abaixo de 7.2.

**20/09** — a balança da Mel ("Yoda0", protocolo okok) aparecia como **não
suportada**. A investigação mostrou que não era bug do redesign: o fork
simplesmente não tinha o driver, porque estava parado em 2020. O upstream tinha.

A decisão foi **adotar o upstream 3.1.3** e reaplicar o design sobre ele.

## O que o upstream tinha virado

Não era a mesma base com correções — era uma reescrita.

| | Fork (2.3.5) | Upstream (3.1.3) |
|---|---|---|
| Linguagem | Java | Kotlin |
| UI | XML + Fragments | Jetpack Compose |
| minSdk | 21 (Android 5) | 31 (Android 12) |
| Drivers | 24 | 72 |
| DI | nenhuma | Hilt |
| Build | Groovy | Kotlin DSL |

1045 commits de diferença.

## O que se perdeu e o que sobreviveu

**Perdeu-se:** as cinco telas em XML e os fragments Java do redesign. Não havia
como adaptá-los — a base nova não tem `res/layout` nem Fragment.

Também se perdeu o suporte a aparelhos abaixo do **Android 12**.

**Sobreviveu — e era a parte que mais custou a pensar:**

- `design/` — o protótipo e os tokens, que são a especificação e independem de
  tecnologia;
- a paleta M3 com os contrastes verificados, que virou `DesignColor.kt`;
- a escala tipográfica, que virou `DesignType.kt`;
- as decisões de produto e o levantamento de funcionalidades.

## Onde está o trabalho antigo

| Referência | O que guarda |
|---|---|
| tag `fork-2.3.5-java` | a master no ponto anterior à adoção do upstream |
| branch `feat/redesign-material3` | o redesign completo em XML/Java |
| branch `chore/modernizar-build` | a migração AGP 4.1 → 8.13 sobre a base antiga |
| branch `feat/upstream-3.1.3` | o upstream puro, sem nenhuma alteração nossa |

Nada foi apagado. Para consultar:

```bash
git show fork-2.3.5-java:CLAUDE.md
git log feat/redesign-material3 --oneline
```

## Lições que continuam valendo

**A camada de balanças falha em silêncio.** Foi o que levou horas para
diagnosticar: a balança "não suportada" não gerava erro nenhum. Ver
[balancas-bluetooth.md](balancas-bluetooth.md).

**Compilar não é rodar.** O redesign antigo compilou e só então revelou um crash
no `onCreate`. Validação estática pega referência quebrada, não comportamento.

**Um fork parado acumula dívida invisível.** O app funcionava; o que faltava só
apareceu quando uma balança nova entrou em cena. Daí a orientação de acompanhar
o upstream — ver a seção correspondente no CLAUDE.md.
