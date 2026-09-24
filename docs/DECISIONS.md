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
