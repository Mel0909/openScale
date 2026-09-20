---
description: Compara uma tela do app com o protótipo e aponta onde diverge do design
---

Compare a tela indicada (ou, se nenhuma for indicada, a tela inicial) com o
protótipo, e reporte as divergências.

Fontes:
- `design/App.dc.html` e `design/Tokens.dc.html` — o design.
- `.claude/docs/design-compose.md` — o que já foi aplicado e o que falta.
- O Composable correspondente em `android_app/.../ui/screen/`.

Verifique:

1. **Cor** — toda cor vem de `MaterialTheme.colorScheme`? Sinalize qualquer
   `Color(0x...)` literal fora de `ui/theme/`.
2. **Tipografia** — todo texto vem de `MaterialTheme.typography`? Sinalize
   `fontSize` avulso que não corresponda a um token do design.
3. **Forma** — compare com o protótipo: hierarquia, ordem dos elementos,
   raios de canto, espaçamento, e se há sombra (o design não usa sombra).
4. **Estados** — o protótipo mostra estado vazio? A tela trata ausência de
   dados?
5. **Acessibilidade** — alvos de toque de pelo menos 48dp, e texto em `sp`.

Entregue uma tabela: elemento · como está · como o design pede · arquivo:linha.
Ordene pelo que mais afeta a percepção visual.

Não altere arquivos — isto é análise.
