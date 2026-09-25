# Backlog

Ideias e sugestões fora do escopo atual da especificação, para considerar depois.

- **Importador de "planilha mensal"** (seção 9, bônus explicitamente marcado "fase final"):
  mapear o formato de planilha atual do usuário (linhas = categorias/contas, colunas = meses)
  para lançamentos, com pré-visualização do mapeamento antes de gravar. Não implementado.
- **Relatório mensal exportável em PDF** (seção 11, marcado "opcional, fase final"). O CSV de
  um mês específico já é possível via exportação por entidade (Fase 8); só falta o PDF.
- **"Provisões" com sugestão automática** (seção 11): hoje o usuário já consegue criar uma
  categoria "Provisões" e um teto mensal manualmente (Fase 2). O que falta é a sugestão
  automática de valor mensal = gasto anual (IPVA, seguro, revisão, presentes) / 12.
- **Validação de integridade referencial no dry-run de importação** (ver DECISIONS.md, Fase 8):
  hoje o dry-run valida tipo/formato de cada campo, mas não confirma que um `category_id`/
  `account_id`/etc. referenciado realmente existe — isso só é pego pela constraint de FK do
  Postgres no momento de aplicar. Cobre bem o caso de uso real (reimportar seu próprio export),
  mas uma validação prévia mais completa poderia detectar isso no dry-run.
- **Evolução patrimonial com valor de mercado histórico**: hoje o componente de investimentos
  na evolução patrimonial (Fase 9) usa capital aportado (custo), não cotação histórica, porque
  o app não tem provedor de cotação automática (seção 13 — não objetivo). Se um provedor de
  preços for adicionado no futuro, dá para guardar preços de fechamento por dia e refazer esse
  gráfico com valor de mercado real.
