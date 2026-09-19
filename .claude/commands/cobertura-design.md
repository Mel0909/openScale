---
description: Compara o app antigo com o design novo e aponta o que ainda não tem destino
---

Compare o que o app faz hoje com o que o design novo cobre, e produza um relatório
de lacunas.

Fontes:
- `.claude/docs/inventario-legado.md` — o checklist do que existe.
- `design/App.dc.html` e `design/Tokens.dc.html` — o design.
- O código atual em `android_app/app/src/main/` quando precisar confirmar um detalhe.

Para cada funcionalidade do inventário, classifique:

- **Já implementada no novo** — existe tela nova funcionando.
- **Coberta pelo design, não implementada** — tem desenho, falta código.
- **Sem destino** — nem desenho nem decisão. **Este é o achado que importa.**
- **Cortada por decisão** — se houver registro explícito da escolha.

Entregue:

1. Uma tabela só com os itens **sem destino**, ordenada por risco de perda
   (quanto mais central ao propósito do app, mais alto). Lembre que a proposta
   declarada no README é: sem conta, sem nuvem, dados exportáveis.
2. Para cada um, uma frase sobre onde ele *poderia* morar no design novo.
3. As decisões de produto que continuam pendentes.

Não altere arquivos — isto é análise. Se identificar algo que o inventário não
registrou, diga, para que o documento seja atualizado.
