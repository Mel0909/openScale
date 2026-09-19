# Design novo — do protótipo para o Android

Fonte da verdade: a pasta [`design/`](../../design/) na raiz.

| Arquivo | O que é |
|---|---|
| `Tokens.dc.html` | Fundação: paleta M3 completa (claro + escuro), escala tipográfica, cores de gráfico. **Etapa 1 de 4.** |
| `App.dc.html` | Protótipo navegável das telas. Abre no navegador e é clicável. |
| `android-frame.jsx` | Moldura de device Material 3 (scaffold, não é conteúdo). |
| `github.md` | Log de sincronização com o repo `Mel0909/openScale`. |
| `support.js` | Runtime do formato `.dc.html`. Não editar. |

Para ver: abrir `design/App.dc.html` no navegador. Tem alternador claro/escuro.

---

## O conceito

Rosa profundo como cor de **dado e de ação**, sobre fundos quentes levemente rosados.
A regra central declarada no design: *"o rosa nunca preenche a tela — ele aparece no
número, na linha do gráfico, no botão e na variação"*.

Duas famílias tipográficas:
- **Bricolage Grotesque** (800/700) — só números grandes e títulos curtos. É o que
  cria o contraste de peso.
- **Instrument Sans** (400/500/600) — todo o resto.

Sem sombra em nenhum tema. Hierarquia vem de `surface` mais claro que `background`
no tema claro, e de borda de 1px no escuro. Decisão explícita para evitar blur e
elevação custosos no Android.

Todos os pares de cor vêm com **contraste WCAG AA verificado** no `Tokens.dc.html`.

---

## Paleta → `res/values/colors.xml`

Os slots já estão nomeados exatamente como o Material 3 espera. Tema claro:

```
primary            #A81F52     background       #FBF3F1
onPrimary          #FFFFFF     onBackground     #2A2224
primaryContainer   #FFD9E3     surface          #FFF8F6
onPrimaryContainer #3F0019     onSurface        #2A2224
secondary          #7A5A62     surfaceVariant   #F4E4E6
onSecondary        #FFFFFF     onSurfaceVariant #5D4B50
secondaryContainer #FBE1E7     outline          #8A7176
onSecondaryContainer #2F1620   outlineVariant   #E6D2D6
tertiary           #8A5A2B     error            #B3261E
onTertiary         #FFFFFF     onError          #FFFFFF
tertiaryContainer  #FFDDB8     errorContainer   #F9DEDC
onTertiaryContainer #2E1700    onErrorContainer #410E0B
```

Tema escuro (→ `res/values-night/colors.xml`, diretório que **ainda não existe**):

```
primary            #FFB1C6     background       #141011
onPrimary          #5E0A2B     onBackground     #F0E0E3
primaryContainer   #7E1F45     surface          #191113
onPrimaryContainer #FFD9E3     onSurface        #F0E0E3
secondary          #E3BDC7     surfaceVariant   #3A2C30
onSecondary        #432A33     onSurfaceVariant #D6BFC5
secondaryContainer #5B3F48     outline          #9E848A
onSecondaryContainer #FFD9E3   outlineVariant   #4A393E
tertiary           #EFBD8B     error            #F2B8B5
onTertiary         #4A2800     onError          #601410
tertiaryContainer  #693C0D     errorContainer   #8C1D18
onTertiaryContainer #FFDDB8    onErrorContainer #F9DEDC
```

Nota do design: `outline` (#8A7176) é **só para borda e eixo, nunca texto** —
tem 4,25:1, abaixo de AA para corpo.

---

## Tipografia → `res/values/type.xml` + `res/font/`

| Token | Spec | Slot M3 |
|---|---|---|
| Número herói | Bricolage 800 · 64/0.92 · -0.035em | `displayLarge` |
| Número secundário | Bricolage 700 · 34/1.0 · -0.02em | `headlineMedium` |
| Título de tela | Bricolage 700 · 26/1.15 | `headlineSmall` |
| Título de seção | Instrument Sans 600 · 18/1.3 | `titleMedium` |
| Corpo | Instrument Sans 400 · 15/1.55 | `bodyLarge` |
| Rótulo | Instrument Sans 500 · 13/1.2 | `labelLarge` |
| Rótulo micro | Instrument Sans 600 · 11/1.2 · +0.09em · CAIXA ALTA | `labelSmall` |

Em Android isso vira `TextAppearance.*` no tema. **Sempre `sp`**, nunca `dp`
(o app antigo erra isso em `fragment_statistics.xml`).

O protótipo usa 84px no número herói da tela Hoje, acima dos 64 do token —
é a variação de destaque da tela principal.

---

## Gráfico → MPAndroidChart

O design já mapeia para as APIs da lib:

| Elemento | Claro | Escuro |
|---|---|---|
| Linha | `#A81F52` | `#FFB1C6` |
| Preenchimento sob a linha | `#F7C9D8` (alpha 60) | `#5E2338` (alpha 55) |
| Círculo do ponto | `#A81F52` | `#FFB1C6` |
| Ponto destacado | `#C2185B` | `#FFD9E3` |
| 2ª série | `#8A5A2B` | `#EFBD8B` |
| Grade (`gridColor`) | `#E6D2D6` | `#31252A` |
| Eixo (`axisLineColor`) | `#D9C2C7` | `#4A393E` |
| Texto do eixo (`textColor`) | `#7A6469` | `#B9A2A8` |
| Faixa de referência (`LimitLine`) | `#F4E4E6` | `#2C2023` |

Regra do design: **uma série por vez** é o padrão. A cor âmbar (2ª série) existe
só para comparar duas métricas no mesmo eixo. Faixa de referência é preenchimento
chapado, sem gradiente — decisão de performance.

---

## Telas do protótipo

Navegação: **bottom nav de 4 abas** (Hoje · Histórico · Medições · Perfil),
com pill de `primaryContainer` marcando a ativa. Sem drawer lateral.

### Hoje
Header com chip de usuário (avatar circular com inicial) + ícone de ajustes.
Peso herói em Bricolage 84px, chip de variação em `primaryContainer`, sparkline,
e grade de métricas (cada uma clicável → leva ao Histórico daquela métrica).
CTA "Pesar agora".

### Histórico
Chips de métrica (rolagem horizontal) + seletor de período (Semana/Mês/Ano).
Gráfico com área preenchida, rótulos de eixo, e estatísticas abaixo.
Nota de agregação explicando o que cada período mostra.

### Medições
Lista agrupada por mês (cabeçalho "Setembro 2026"), cada linha com dia, hora,
peso, delta colorido e subtítulo com as outras métricas. Toque abre sheet de
detalhe com todas as métricas + anotação.

### Perfil
Campos (nome, nascimento, altura, sexo) — cada um com a frase do que ele
**alimenta** ("Entra em gordura estimada, água e TMB"). Detalhe bom: conecta
o dado pedido ao benefício.

### Ajustes
Aparência (claro/escuro), acento (rosa/azul — há uma paleta azul alternativa
no código do protótipo), toggles de métricas com a origem de cada uma
("balança", "estimada", "calculado", "manual"), e escolha de fórmula.

### Sheets
- **Pesar agora** — estado de espera → leitura recebida → salvar.
- **Inserir manual** — peso com +/-, delta em relação à última, campos manuais.
- **Usuários** — troca de perfil.
- **Detalhe de medição** — todas as métricas do registro.

---

## O que falta decidir antes de implementar

Ver `.claude/docs/inventario-legado.md` para o detalhe. Em resumo:

1. **Statistics** não tem equivalente no design novo (meta de peso, dias restantes).
2. **Import/export e backup** não aparecem — mas são proposta central do app.
3. **Lembretes** não aparecem.
4. **Escaneamento/pareamento Bluetooth** e status de conexão persistente.
5. **16 das 25 métricas** não estão no design (calipers, TDEE, WHR, WHtR, circunferências).
6. **Widget** de tela inicial tem visual próprio — entra no escopo?
7. **Onboarding** (7 slides) não tratado.

Nenhum desses bloqueia começar por Tokens → Hoje. São telas de Ajustes ou decisões
de escopo que podem vir depois.

---

## Pré-requisitos técnicos

Antes de qualquer tela nova:

1. **Subir `com.google.android.material`** — `1.3.0-alpha04` não tem Material 3.
   Precisa de `1.5.0+` para `Theme.Material3.*`. Verificar compatibilidade com
   AGP 4.1 / minSdk 21.
2. **Trocar o parent do tema** de `Theme.AppCompat.DayNight` para `Theme.Material3.DayNight`.
   Isso é o que faz os slots `primaryContainer`, `surfaceVariant` etc. existirem.
3. **Criar `res/values-night/`**.
4. **Adicionar as fontes** em `res/font/` (Bricolage Grotesque, Instrument Sans).
5. **Habilitar ViewBinding** (opcional, mas barato e evita bugs de `findViewById`).
6. **Resolver o build local** ou aceitar validar por leitura (`.claude/docs/build.md`).

O passo 1 é o que tem mais risco de atrito — vale ser o primeiro teste real.
