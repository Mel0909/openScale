---
description: Verifica mudanças de UI por leitura, já que o build local não roda
---

O build Gradle não roda nesta máquina (ver `.claude/docs/build.md`). Valide as
mudanças de UI pendentes por inspeção estática.

Rode `git diff` e, para cada arquivo alterado em `res/` ou `gui/`, verifique:

1. **Recursos existem** — toda referência `@color/`, `@drawable/`, `@string/`,
   `@style/`, `@dimen/`, `@font/` introduzida tem declaração correspondente.
   Cheque `?attr/` contra os atributos que o parent do tema realmente define —
   atenção especial aos slots M3 (`colorPrimaryContainer`, `colorSurfaceVariant`…),
   que **só existem** com parent `Theme.Material3.*`.

2. **Paridade claro/escuro** — toda cor nova em `values/colors.xml` tem a
   contraparte em `values-night/colors.xml`. Compare os dois arquivos e liste
   qualquer chave presente num e ausente no outro.

3. **Fidelidade ao design** — confronte os hex usados com
   `.claude/docs/design-novo.md`. Sinalize qualquer cor que não venha dos tokens.

4. **Sem literais de cor** em layouts ou Java. Tudo deve ser `@color/` ou `?attr/`.

5. **Unidades** — `sp` para texto, `dp` para dimensão. Sinalize `px` bruto ou
   `dp` em `textSize` (o app antigo erra isso em `fragment_statistics.xml`).

6. **Strings** — nada hardcoded; tudo em `res/values/strings.xml`.
   Nenhuma edição em `res/values-*/` (Weblate).

7. **Ids** — se algo foi renomeado ou removido, confira `findViewById` no Java
   e os testes Espresso em `androidTest/.../gui/` (`saveButton`, `float_input`,
   `action_add_measurement`).

8. **Funcionalidade preservada** — se a mudança substitui uma tela antiga, cheque
   `.claude/docs/inventario-legado.md`: algum comportamento listado ali se perdeu
   sem decisão explícita?

Reporte só problemas reais, com `arquivo:linha`. Se estiver tudo certo, diga isso
de forma direta — sem inventar ressalvas.
