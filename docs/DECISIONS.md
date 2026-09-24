# Decisões de arquitetura

Registro de decisões técnicas tomadas durante a implementação quando a especificação
(`prompt_claude_code_financas.md`) deixava a escolha em aberto, seguindo a regra da
seção 0.4: escolher a opção mais simples e segura, registrar aqui e seguir em frente.

## Fase 1

### Autenticação: sessão do Spring Security em vez de JWT
A spec permitia escolher entre JWT ou sessão do Spring, desde que o cookie fosse
HttpOnly + SameSite=Strict e o token nunca ficasse em `localStorage`. Optamos por
**sessão do Spring Security** (cookie `JSESSIONID`) porque:
- É um app local, single-tenant por usuário, sem necessidade de escalar horizontalmente
  nem de autenticação stateless entre serviços.
- Evita implementar/manter assinatura, expiração e revogação de JWT.
- Integra nativamente com a proteção CSRF do Spring Security (cookie `XSRF-TOKEN` +
  header `X-XSRF-TOKEN`), que é exatamente o que a spec pede.

### IDs: UUID v7 via biblioteca `com.github.f4b6a3:uuid-creator`
A spec pede UUID v7 ou v4, gerados na aplicação. Usamos UUID v7 (ordenável por tempo,
melhor para índices B-tree do Postgres) através da biblioteca `uuid-creator`, leve e
sem dependências transitivas problemáticas.

### Rate limiting de login: contador em memória (Caffeine)
5 tentativas falhas por username → bloqueio de alguns minutos. Implementado com cache
em memória (Caffeine, já trazido pelo Spring Boot starter cache) em vez de tabela no
banco: é um app local de um único usuário, não há necessidade de persistir tentativas
entre reinícios do processo, e evita migração/tabela extra só para isso.

### Timezone da aplicação: `America/Fortaleza`
Fixado via `TZ` no `.env` e propagado para os containers `db` e `api`, conforme pedido
na seção 1 (fuso do usuário, Natal/RN).

## Fase 2

### Mapeamento entidade→DTO: MapStruct nos módulos com mais campos, `record` puro nos triviais
Para `account`, `credit_card` e `category` usamos interfaces `@Mapper` do MapStruct (como
pede a seção 3). Para `AppSetting`/`Budget`, onde a resposta é montada a partir de valores
calculados (defaults mesclados com overrides) em vez de um mapeamento 1:1 de entidade, o DTO
é construído diretamente no service — usar MapStruct ali não traria benefício.

### Exclusão de conta/cartão = arquivar (soft delete); exclusão de categoria = física
Como pedido na seção 3 ("cartões, contas e ativos ficam archived"). Categoria não tem
`archived` no modelo de dados da spec, então `DELETE /categories/{id}` apaga de verdade,
mas é bloqueado (HTTP 409) se existir subcategoria ou orçamento vinculado — evita registros
órfãos sem precisar de soft delete nessa tabela.

### Tela "Contas" própria, além do que a seção 8 descrevia
A seção 8 não lista uma tela dedicada a contas (apenas "Cartões e faturas" e
"Configurações"). Como cartões, e futuramente faturas/lançamentos, referenciam contas,
criamos uma tela "Contas" simples na navegação — sem ela não haveria como cadastrar a
conta de pagamento padrão de um cartão pela interface.

## Fase 3

### Pagamento de fatura não gera uma `transaction` própria
A seção 7.1 diz que "pagar fatura" cria uma transação de saída na conta escolhida. Optamos
por registrar o pagamento apenas nos campos `paid_amount`/`paid_on`/`paid_from_account_id`
da própria `invoice`, sem duplicar como uma `transaction` de saída da conta: as
`transaction`s individuais da fatura (kind `EXPENSE`, `card_id` preenchido) já representam
o gasto; criar mais uma `transaction` de saída da conta ao pagar somaria o valor duas vezes
no fluxo de caixa. Se a Fase 5/6 (orçamento, sobra do mês) precisar enxergar o pagamento
como saída de conta, revisamos isso então — por ora é mais simples e evita dupla contagem.

### Materialização de recorrências: imediata na criação/edição + job diário
A seção 6 pede que um "job/serviço" materialize as ocorrências dos próximos 12 meses e ao
abrir cada mês. Implementamos os dois: a materialização roda de forma síncrona ao
criar/editar uma recorrência ativa (o usuário já vê os lançamentos na hora) e também via
`@Scheduled` diário (3h da manhã), que cobre a abertura do mês e cartões criados por outras
vias. A checagem de duplicidade é por `(recurring_rule_id, date)`, então rodar o job com
mais frequência que o necessário é inofensivo (idempotente).

### `TransactionKind.TRANSFER` implementado de forma mínima
A especificação lista `TRANSFER` como um dos tipos de lançamento mas não detalha suas regras
de negócio (ex.: conta de origem/destino) em nenhuma seção posterior. Tratamos por ora como
uma variação de lançamento de conta única (mesma exigência de `account_id` que `INCOME`),
sem inventar um modelo de duas pontas que a spec não pediu. Registrar aqui para revisitar
se alguma fase futura detalhar transferências entre contas.
