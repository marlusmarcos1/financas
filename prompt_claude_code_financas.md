# PROMPT PARA O CLAUDE CODE — App de Organização Financeira Pessoal ("Finanças do Marlus")

> Cole este documento inteiro no Claude Code, numa pasta vazia do projeto. Peça para ele começar pelo **modo de planejamento** (plan mode) e só codar depois de você aprovar o plano.

---

## 0. Como você (Claude Code) deve trabalhar

1. Leia toda esta especificação antes de escrever qualquer código.
2. Primeiro apresente um **plano** com a estrutura de pastas, o modelo de dados e a ordem das fases. Espere minha aprovação.
3. Trabalhe **por fases** (seção 15). Ao final de cada fase: rode os testes, suba o `docker compose`, confirme que tudo funciona, faça um commit com mensagem clara (Conventional Commits) e me diga como validar manualmente.
4. Se uma decisão não estiver clara, escolha a opção mais simples e segura, registre a decisão em `docs/DECISIONS.md` e siga em frente. Só pergunte se for bloqueante.
5. Código, nomes de classes, tabelas, colunas e commits em **inglês**. Interface, mensagens de erro para o usuário e textos em **português do Brasil (pt-BR)**.
6. Não invente funcionalidades fora do escopo. Sugestões extras vão para `docs/BACKLOG.md`.

---

## 1. Contexto e objetivo

Sou o Marlus, 28 anos (nasci em 12/12/1997), moro em Natal/RN (fuso `America/Fortaleza`), tenho renda mensal líquida de cerca de R$ 5.000 e uma bolsa temporária de R$ 3.800 por mês com duração prevista de 15 meses (pode reaparecer de forma esporádica). Recebo também 13º.

Preciso de uma aplicação **local (self-hosted, uso pessoal)** para organizar minhas finanças com estas metas:

- Controlar receitas, despesas, cartões de crédito, faturas e **compras parceladas**.
- Saber **quanto posso parcelar com segurança** antes de fazer uma compra.
- Acompanhar **dízimo (10% de tudo que entra)**, **aposentadoria** (aporte mensal fixo para 30 anos), **investimentos** (FIIs, Tesouro, etc.), **reserva** ("Minha reserva") e **metas** (entrada da casa por volta dos 30 anos, carro).
- **Exportar e importar** todos os dados em CSV para levar o sistema a outro computador.
- Rodar tudo em **Docker** (banco, back-end e front-end) com um único comando.

Este é um app de uso pessoal e local. Priorize clareza, corretude dos cálculos e facilidade de uso em vez de escala.

---

## 2. Stack obrigatória

| Camada | Tecnologia |
|---|---|
| Back-end | **Java 21**, **Spring Boot 3.x** (última estável), Maven, Spring Web, Spring Data JPA (Hibernate), Spring Security, Bean Validation, springdoc-openapi |
| Banco | **PostgreSQL 16** |
| Migrations | **Flyway** (`db/migration`, `V1__...`, `V2__...`), `spring.jpa.hibernate.ddl-auto=validate` |
| Front-end | **React 18 + TypeScript + Vite**, React Router, **TanStack Query**, React Hook Form + Zod, Tailwind CSS, Recharts, `date-fns` (locale pt-BR) |
| Mapeamento | MapStruct (entidade ↔ DTO); DTOs como `record` |
| Testes | JUnit 5, Mockito, **Testcontainers** (Postgres) no back; Vitest + Testing Library no front |
| Infra | **Docker + docker compose**: serviços `db`, `api`, `web` (Nginx servindo o build do front e fazendo proxy de `/api` para o back) |

Justificativa da escolha de React: ecossistema maior e mais suporte de bibliotecas de gráficos e formulários. Mantenha o front simples, sem over-engineering.

---

## 3. Arquitetura e padrões de projeto

**Back-end** em camadas por módulo (package-by-feature), por exemplo `com.marlus.financas.<modulo>`:

- `web` (controllers REST, DTOs de request/response)
- `service` (regras de negócio, transações `@Transactional`)
- `domain` (entidades JPA e objetos de valor)
- `repository` (Spring Data)
- `mapper` (MapStruct)

Regras:

- Injeção por construtor, sem `@Autowired` em campo. Lombok é opcional; prefira `record` para DTOs.
- **Dinheiro sempre em `BigDecimal`** no Java e `NUMERIC(14,2)` no Postgres. Nunca `double`/`float`. Moeda BRL. Arredondamento `HALF_EVEN` (ou `HALF_UP`, documente a escolha).
- **IDs UUID** (v7 ou v4) em todas as tabelas, gerados na aplicação. É isso que garante importar/exportar entre máquinas sem colisão.
- Todas as tabelas de negócio têm `user_id`, `created_at`, `updated_at` (preenchidos por auditoria JPA) e usam **soft delete** apenas onde fizer sentido (cartões, contas e ativos ficam `archived`).
- Padrões esperados: **Strategy** (cálculo de fatura por cartão, regras de projeção), **Factory/Builder** (geração de parcelas), **Specification** ou query methods (filtros de lançamentos), **Ports & Adapters simples** para import/export (`Exporter`/`Importer` por entidade, registrados por um `ExportRegistry`).
- Tratamento global de erros com `@RestControllerAdvice` e **RFC 7807 `ProblemDetail`**, com mensagens em pt-BR.
- API REST em `/api/v1`, paginação (`page`, `size`, `sort`), filtros por período/categoria/cartão, OpenAPI em `/swagger-ui.html` (somente ambiente local).
- Logs estruturados, sem dados sensíveis. Fuso `America/Fortaleza`; datas de negócio como `LocalDate`, competências como `YearMonth` (coluna `date` com dia 1 ou `char(7)` — documente).

**Front-end**:

- Estrutura por feature (`features/cards`, `features/transactions` etc.), camada `api/` com cliente tipado, hooks do TanStack Query, componentes de UI reutilizáveis.
- Formatação `Intl.NumberFormat('pt-BR', {style:'currency', currency:'BRL'})` e datas `dd/MM/yyyy`.
- Seletor de **mês/competência global** no topo (mês atual por padrão, com setas para navegar).
- Responsivo (uso no celular também), modo escuro, acessível (labels, foco, contraste).

---

## 4. Autenticação e segurança

- Tela de login. Usuário inicial: **`marlus`** / senha **`marlus`** (uso local; ver aviso abaixo).
- Senhas com **BCrypt**. Crie o usuário inicial por um `ApplicationRunner` que lê `APP_ADMIN_USER` e `APP_ADMIN_PASSWORD` do `.env` e só cria se não existir nenhum usuário (não coloque o hash fixo em migration).
- Sessão via **cookie HttpOnly + SameSite=Strict** (JWT ou sessão do Spring; escolha e documente). Não guardar token em `localStorage`. Proteção CSRF adequada para a escolha feita.
- Todas as rotas exigem autenticação, exceto login e healthcheck. Todos os dados filtrados por `user_id` do usuário autenticado (multiusuário simples, sem tela de cadastro; troca de senha na tela de configurações).
- Limitar tentativas de login (ex.: 5 falhas → bloqueio de alguns minutos).
- No `docker-compose.yml`, publicar as portas **apenas em `127.0.0.1`** (`127.0.0.1:8080:80`). Documentar no README: *"Senha padrão marlus/marlus é só para uso local. Altere antes de expor em qualquer rede."*
- CORS fechado (mesma origem via Nginx). Headers de segurança básicos no Nginx.

---

## 5. Docker

- `docker compose up -d --build` sobe tudo. Serviços: `db` (postgres:16, volume nomeado `pgdata`, healthcheck com `pg_isready`), `api` (multi-stage: build Maven → JRE 21 slim, usuário não-root, healthcheck no actuator), `web` (multi-stage: build Vite → Nginx).
- Configuração via `.env` (com `.env.example` versionado): credenciais do banco, `APP_ADMIN_USER`, `APP_ADMIN_PASSWORD`, `TZ=America/Fortaleza`.
- `Makefile` (ou scripts) com: `up`, `down`, `logs`, `test`, `backup-db` (pg_dump para `./backups`), `restore-db`.
- Perfil `dev` opcional com hot reload (front em Vite dev server, back com devtools).
- README com passo a passo: subir, acessar, exportar/importar, backup, atualizar.

---

## 6. Modelo de dados (Flyway)

Crie migrations pequenas e versionadas por fase (`V1__init_auth.sql`, `V2__accounts_cards_categories.sql`, ...). Nunca edite migration já aplicada. Constraints (`NOT NULL`, `CHECK`, FKs, índices por `user_id` + data) fazem parte do escopo.

Entidades principais (os campos abaixo são o mínimo; refine conforme necessário):

**Núcleo**
- `app_user` (id, username único, password_hash, display_name, birth_date)
- `app_setting` (user_id, key, value) — chaves: `tithe_percent` (10), `installment_limit_percent` (30), `emergency_months_target` (6), `currency` (BRL)
- `account` (id, name, type: `CHECKING|SAVINGS_BOX|BROKERAGE|CASH`, institution, initial_balance, purpose: `DAILY|EMERGENCY_RESERVE|GOAL|INVESTMENT`, archived)
- `credit_card` (id, name, issuer, credit_limit, closing_day, due_day, default_payment_account_id, color, archived)
- `category` (id, name, kind: `INCOME|EXPENSE`, nature: `FIXED|VARIABLE`, parent_id, icon, color) + `budget` (category_id, month nullable = padrão, limit_amount)

**Receitas**
- `income_source` (id, name, type: `SALARY|SCHOLARSHIP|THIRTEENTH|EXTRA|OTHER`, recurrence: `MONTHLY|TEMPORARY|SPORADIC`, expected_amount, pay_day, start_date, end_date, expected_months, tithe_applies, counts_in_base_budget)
  - Regra: **`counts_in_base_budget=false`** para bolsa, 13º e extras. Eles **não entram** na renda base usada para o teto de parcelas e para o orçamento do mês; entram na aba "Extras" e são destinados a reserva/metas.
- `income_entry` (id, source_id, account_id, reference_month, received_on, amount, status: `EXPECTED|RECEIVED`)

**Lançamentos, parcelas e faturas**
- `transaction` (id, kind: `INCOME|EXPENSE|TRANSFER`, description, amount, date, category_id, account_id nullable, card_id nullable, invoice_id nullable, status: `PLANNED|PAID`, installment_plan_id nullable, installment_number nullable, recurring_rule_id nullable, income_entry_id nullable, notes)
  - Despesa é **ou** de conta **ou** de cartão (CHECK).
- `installment_plan` (id, card_id, description, purchase_date, total_amount, installment_count, installment_amount, **first_installment_number** (para planos já em andamento: "estou na 3 de 10"), first_invoice_month, interest_rate_monthly nullable, category_id)
  - Ao criar, gera as `transaction` das parcelas restantes com o `invoice` correto e **ajuste de centavos na última parcela**.
- `invoice` (id, card_id, reference_month, closing_date, due_date, status: `OPEN|CLOSED|PAID`, paid_amount, paid_on, paid_from_account_id) — total calculado a partir das transações.
- `recurring_rule` (id, description, amount, **amount_is_variable**, frequency: `MONTHLY|YEARLY`, day_of_month, start_date, end_date nullable, category_id, card_id/account_id, active) — para assinaturas e contas fixas (Meli+, Claude, internet, crédito de celular etc.). Um job/serviço **materializa** as ocorrências futuras (`PLANNED`) para os próximos 12 meses e ao abrir cada mês.

**Destinação do dinheiro**
- `allocation_rule` (id, name, percent, destination_type: `TITHE|RETIREMENT|RESERVE|GOAL|INVESTMENT`, applies_to: lista de tipos de receita, base: `GROSS_RECEIVED`)
- `tithe_ledger` (id, reference_month, base_amount, percent, due_amount, paid_amount, paid_on, status). Dízimo de **cada** entrada de receita (salário, bolsa, 13º, extras), somado por competência, com marcação de pago.

**Investimentos e aposentadoria**
- `investment_asset` (id, ticker, name, asset_class: `FII|STOCK|ETF|TREASURY|CDB|SAVINGS_BOX|CRYPTO|OTHER`, subclass: `BRICK|PAPER|FOF|HYBRID|...`, indexer, maturity_date, current_price, price_updated_at, purpose: `RETIREMENT|HOUSE|GENERAL`, archived)
- `investment_transaction` (id, asset_id, type: `BUY|SELL|DIVIDEND|JCP|INTEREST|FEE`, date, quantity, unit_price, fees, amount, account_id) — preço médio e posição derivados (método de preço médio ponderado; documente).
- `allocation_target` (purpose, asset_class, target_percent) — alocação alvo por carteira (ex.: aposentadoria).
- `retirement_plan` (id, monthly_contribution, contribution_annual_increase_percent, start_date, horizon_years, expected_return_nominal_annual, expected_inflation_annual, current_balance) + cenários prontos (pessimista 6%, base 8%, otimista 10%).

**Metas**
- `goal` (id, name, type: `HOUSE|CAR|EMERGENCY|OTHER`, target_amount, target_date, linked_account_id nullable, monthly_contribution_planned, priority, notes)
- `goal_contribution` (id, goal_id, date, amount, source: `SALARY|SCHOLARSHIP|THIRTEENTH|EXTRA|MANUAL`)

**Sistema**
- `import_job` (id, filename, mode, status, summary_json, created_at) para auditoria de importações.

---

## 7. Regras de negócio críticas

### 7.1 Faturas de cartão
- Cada cartão tem `closing_day` e `due_day`. Compra com data **após o fechamento** vai para a fatura do mês seguinte; até o dia do fechamento (inclusive), para a fatura do mês corrente.
- `due_date` cai no mês da competência da fatura (ou no seguinte, conforme `due_day` < `closing_day`; implemente e teste esse caso).
- Pagar fatura = ação que cria uma transação de saída na conta escolhida e marca a fatura `PAID` (com suporte a pagamento parcial registrado em `paid_amount`).
- **Limite disponível** do cartão = limite − (faturas em aberto + parcelas futuras ainda não faturadas).

### 7.2 Parcelamentos
- Valor da parcela = `total_amount / installment_count`, arredondando cada parcela e ajustando a diferença na **última**.
- Suportar parcelas **com juros** (`interest_rate_monthly`) usando tabela Price, e exibir a **taxa efetiva** e o custo total quando o usuário informar valor à vista e valor parcelado.
- Suportar cadastro de **parcelamentos já em andamento** (ex.: "1ª de 12 de R$ 345", "celular 10x de R$ 325 terminando em dezembro").
- Tela "Compromissos futuros": tabela mês a mês (12 a 24 meses) mostrando quanto de parcelas e contas fixas está comprometido em cada mês e **quando cada parcelamento termina** (mostrar quanto é "liberado" nesse mês).

### 7.3 Renda base e orçamento
- **Renda base mensal** = soma das fontes com `counts_in_base_budget=true` (salário). Bolsa, 13º e extras **nunca** entram nela.
- Orçamento por categoria com **teto** (ex.: Combustível R$ 950) e indicadores verde/amarelo/vermelho (≤80%, 80–100%, >100%).
- **Sobra do mês** = renda base recebida − despesas do mês (inclui dízimo do salário e aporte de aposentadoria, que são "pague-se primeiro"). Mostrar também a sobra **normalizada** (sem itens fora do padrão).

### 7.4 Simulador de compra parcelada segura (feature central)
Entrada: descrição, valor total, nº de parcelas, cartão, data, juros (opcional).
Cálculo:
1. Projetar os próximos N meses (N = parcelas + 3) de: renda base, despesas fixas/recorrentes previstas, parcelas já existentes e a nova parcela.
2. Métricas: (a) **% de comprometimento com parcelas** = parcelas do mês / renda base; (b) **saldo livre projetado** de cada mês depois de dízimo, aporte de aposentadoria, fixas, parcelas e média de gastos variáveis (média móvel dos últimos 3–6 meses); (c) impacto no **limite** do cartão.
3. **Veredito**: 
   - **Seguro**: comprometimento ≤ limite configurado (padrão 30%) em todos os meses **e** saldo livre ≥ 0 em todos.
   - **Atenção**: passa de 30% em algum mês ou saldo livre < 10% da renda base em algum mês.
   - **Não recomendado**: saldo livre negativo em algum mês, ou comprometimento > 40%.
4. Mostrar gráfico mês a mês com e sem a compra, e o **valor máximo de parcela seguro** hoje ("você pode assumir até R$ X por mês em novas parcelas"). Sugerir alternativas (menos parcelas, esperar o mês em que uma parcela termina, à vista via reserva).
5. Tudo parametrizável em Configurações e coberto por testes unitários com cenários numéricos.

### 7.5 Dízimo
- Para **cada receita** com `tithe_applies=true`, gerar automaticamente o valor devido (percentual configurável, padrão 10%) no `tithe_ledger` da competência. Marcar como pago gera a transação correspondente. Painel mostra: devido no mês, pago, pendente e acumulado no ano.

### 7.6 Investimentos e aposentadoria
- **Aposentadoria**: aporte mensal fixo (hoje R$ 500), com regra padrão de **20% em FIIs** e 80% no restante (configurável na alocação alvo). Botão "registrar aporte do mês" cria as compras/lançamentos.
- **Projeção de longo prazo**: valor futuro com aportes mensais, taxa mensal equivalente `(1+i_anual)^(1/12)−1`, opção de reajuste anual do aporte, e conversão para **valor real** descontando a inflação (`(1+nominal)/(1+inflação)−1`). Mostrar 3 cenários (6%, 8%, 10% nominal) lado a lado, total aportado vs. juros acumulados, e gráfico de evolução.
- **FIIs**: posição, preço médio, preço atual (informado manualmente; o serviço de cotação deve ficar atrás de uma interface `PriceProvider` para permitir plugar uma API depois, sem implementar agora), **proventos mensais** recebidos, yield on cost, e evolução dos proventos. Alocação real vs. alvo por classe com sugestão de "para onde vai o próximo aporte" (rebalanceamento por aporte, sem vender).
- Regra de negócio: patrimônio total = contas + reserva + investimentos (posição × preço atual).

### 7.7 Metas e reserva
- **Reserva de emergência**: meta = `emergency_months_target` × média dos gastos essenciais (fixas + variáveis essenciais dos últimos 6 meses). Mostrar "meses de cobertura" atuais.
- **Casa**: meta (valor da entrada), data-alvo (aos 30 anos: 12/12/2027 como padrão editável), quanto já tem, quanto falta, aporte mensal necessário para chegar na data e projeção com o aporte atual. Meta **não** deve ficar investida em ativos de renda variável; exibir aviso se o vínculo for com FII/ação e a data-alvo for menor que 3 anos.
- Destinação de extras: ao registrar bolsa/13º/extra, oferecer distribuição sugerida (ex.: dízimo 10% → resto para reserva/casa) que o usuário confirma ou ajusta.

---

## 8. Telas do front-end (pt-BR)

1. **Login**
2. **Dashboard** (mês selecionado): renda base, despesas, sobra e sobra normalizada; faturas por cartão (valor, vencimento, limite usado); % de comprometimento com parcelas; próximos vencimentos (7/15/30 dias); orçamento por categoria (barras verde/amarelo/vermelho); dízimo do mês; aporte de aposentadoria do mês; patrimônio (reserva + investimentos); alertas ("gasolina em 92% do teto", "fatura fecha em 2 dias").
3. **Lançamentos**: tabela filtrável (período, categoria, cartão, conta, status), criação rápida (atalho de teclado), edição, exclusão, "duplicar", "marcar como paga".
4. **Cartões e faturas**: lista de cartões, detalhe da fatura (por mês), pagar fatura, limite disponível, histórico.
5. **Parcelamentos**: lista com "parcela X de Y", quanto falta, quando termina e quanto libera; cadastro (inclusive já em andamento).
6. **Simulador de compra** (seção 7.4).
7. **Compromissos futuros** (linha do tempo 12–24 meses).
8. **Receitas e Extras**: fontes (salário, bolsa com contagem regressiva dos 15 meses, 13º, extras), entradas do mês, distribuição sugerida.
9. **Orçamento** (tetos por categoria, realizado vs. teto, histórico de 6 meses).
10. **Recorrências/Assinaturas** (Meli+, Claude, internet, crédito celular etc., com flag de valor variável).
11. **Dízimo** (devido/pago/pendente, acumulado no ano).
12. **Investimentos**: carteira, posições, proventos, alocação real vs. alvo, registrar compra/provento, atualizar cotação manual.
13. **Aposentadoria**: plano, simulador e cenários com gráfico.
14. **Metas** (Casa, Carro, Reserva): progresso, aporte necessário, projeção.
15. **Importar/Exportar** (seção 9).
16. **Configurações**: parâmetros (dízimo %, limite de parcelas %, meses de reserva), categorias, trocar senha.

Requisitos gerais de UX: estados de carregamento/vazio/erro, confirmação antes de excluir, feedback (toasts), validação de formulários com mensagens claras, foco em fluxos rápidos (lançar uma despesa em ≤ 4 cliques).

---

## 9. Exportação e importação (requisito forte)

**Objetivo:** levar todos os dados para outro computador com poucos cliques.

**Exportar**
- Botão "Exportar tudo" gera um `.zip` contendo **um CSV por entidade** e um `manifest.json`:
  - `manifest.json`: `schema_version`, `exported_at`, `app_version`, lista de arquivos com contagem de linhas e checksum SHA-256.
  - CSVs (UTF-8, delimitador `;`, cabeçalho na 1ª linha, **decimais com ponto** e **datas ISO 8601** para não depender de locale, aspas para campos com `;`, quebras de linha e aspas duplas escapadas): `accounts.csv`, `credit_cards.csv`, `categories.csv`, `budgets.csv`, `income_sources.csv`, `income_entries.csv`, `installment_plans.csv`, `invoices.csv`, `transactions.csv`, `recurring_rules.csv`, `allocation_rules.csv`, `tithe_ledger.csv`, `investment_assets.csv`, `investment_transactions.csv`, `allocation_targets.csv`, `retirement_plans.csv`, `goals.csv`, `goal_contributions.csv`, `settings.csv`.
  - Referências entre arquivos por **UUID**. **Nunca** exportar `password_hash` nem usuários.
- Também permitir exportar **uma entidade isolada** (ex.: só lançamentos de um período) em CSV.

**Importar**
- Upload do `.zip` (ou de um CSV individual) com **etapa de validação (dry-run)** que mostra: quantas linhas serão criadas/atualizadas/ignoradas e todos os erros por arquivo/linha/coluna, sem gravar nada.
- Modos: **Mesclar** (upsert por UUID; padrão) e **Substituir tudo** (apaga os dados do usuário e importa; exige confirmação digitando "SUBSTITUIR").
- Tudo em **uma única transação**: se algo falhar, nada é gravado. Respeitar a ordem de dependências entre entidades (contas/cartões/categorias → receitas → parcelamentos → faturas → lançamentos → investimentos → metas).
- Validar `schema_version` (aceitar versões antigas por conversores versionados, recusar versões futuras com mensagem clara) e checksums.
- Registrar em `import_job`.
- **Bônus (fase final):** importador de "planilha mensal" no formato que eu uso hoje: linhas = categorias/contas (carro, ipva carro, banese, cartões, net, dízimo, extras, aposentadoria, total, salário) e colunas = meses (Maio, Junho, ...), com pré-visualização de mapeamento das linhas para categorias antes de gravar.
- Testes automatizados: export → banco vazio → import → export novamente e comparar (round-trip idêntico), inclusive com acentos, `;` e aspas nos textos.

---

## 10. Dados iniciais (seed opcional)

Crie um perfil/comando `seed-demo` (idempotente, **não** rodar por padrão) que carrega dados de exemplo baseados na minha situação (todos editáveis pela interface):

- **Cartões**: Hiper Mãe, Carrefour, Itaú, Nubank, C6 Bank (dias de fechamento/vencimento como placeholders para eu ajustar). Conta: Nubank "Caixinha – Minha reserva".
- **Receitas**: Salário ~R$ 5.031,74/mês (renda base); **Bolsa** R$ 3.800/mês, `TEMPORARY`, 15 meses, fora da renda base; **13º** (`THIRTEENTH`); Extras (`SPORADIC`).
- **Parcelamentos em andamento**: colchão 12x de R$ 345 (Carrefour, 1ª parcela na fatura de agosto/2026); cama parcela R$ 209,22 (Carrefour, nº de parcelas a informar); celular 10x de R$ 325 no Itaú (termina em dezembro/2026); outro parcelamento de ~R$ 202 no Itaú (termina em março/2027; valor a confirmar). IPVA do carro em parcelas de R$ 217,97.
- **Recorrências**: Combustível (Carrefour, variável, teto R$ 950), Meli+ ~R$ 79 e Claude ~R$ 120 (C6), Internet Net/Claro (valor variável), crédito de celular R$ 140 (tia), Banese R$ 178,90.
- **Regras de destinação**: dízimo 10% sobre todas as receitas; aposentadoria R$ 500/mês.
- **Aposentadoria**: horizonte 30 anos, aporte R$ 500, alocação alvo 20% FIIs / 80% demais (renda fixa e ações/ETFs), cenários 6%/8%/10% nominal, inflação de 4,5% a.a. como padrão editável.
- **Metas**: Reserva de emergência (6 meses de gastos essenciais), Casa (entrada; data-alvo 12/12/2027; valor a definir), Carro (sem data).
- **Categorias iniciais**: Moradia/Casa, Combustível, Transporte, Cartões, Assinaturas, Internet, Ajuda familiar, Dízimo, Aposentadoria/Investimentos, Lazer, Saúde, Compras pontuais, Extras, Receitas (Salário, Bolsa, 13º, Extras).

---

## 11. Funcionalidades extras (incluídas no escopo — tudo pequeno)

- **Regra "pague-se primeiro"**: no dia do recebimento, mostrar checklist: dízimo, aporte de aposentadoria, reserva, e só então o restante disponível para gastar.
- **Calendário de vencimentos** (faturas, recorrências, parcelas).
- **Alertas** no dashboard (teto estourando, fatura vencendo, bolsa perto do fim, sobra normalizada negativa).
- **Fim da bolsa**: contagem regressiva de meses restantes e simulação "e se a bolsa acabar agora?" (orçamento só com a renda base).
- **Fundo para gastos anuais/sazonais** (IPVA, seguro, revisão do carro, presentes): categoria "Provisões" com valor mensal sugerido = total anual / 12.
- **Evolução patrimonial** mês a mês (reserva + investimentos + metas).
- **Relatório mensal** exportável em PDF ou CSV simples (opcional, fase final).
- **Backup**: comando `make backup-db` e aviso na tela de exportação para exportar periodicamente.

---

## 12. Qualidade e testes

- **Cobertura mínima** nas regras críticas: cálculo de fatura (fechamento/vencimento), geração de parcelas (arredondamento, parcelas em andamento), Tabela Price, simulador de compra segura, projeção de aposentadoria (comparar com valores calculados à mão), dízimo, preço médio de investimentos, import/export (round-trip).
- Testes de integração com **Testcontainers** para repositórios, migrations e endpoints principais (incluindo autenticação e isolamento por `user_id`).
- Front: testes de componentes dos formulários críticos e do simulador.
- Lint/format: Spotless ou Checkstyle no back; ESLint + Prettier + `tsc --noEmit` no front.
- CI opcional: GitHub Actions rodando build e testes.

---

## 13. Não objetivos (por enquanto)

- Sem integração bancária/Open Finance, sem OCR de notas, sem app mobile nativo, sem cotação automática (apenas a interface `PriceProvider`), sem cadastro público de usuários.

---

## 14. Estrutura de repositório sugerida

```
financas/
  docker-compose.yml
  .env.example
  Makefile
  README.md
  docs/ (DECISIONS.md, BACKLOG.md, data-model.md)
  backend/ (Maven, Dockerfile, src/main/java/..., src/main/resources/db/migration)
  frontend/ (Vite + React + TS, Dockerfile, nginx.conf)
```

---

## 15. Plano de fases (entregue uma por vez)

**Fase 1 — Fundação:** repositório, Docker Compose (db/api/web), Flyway `V1`, autenticação (`marlus`/`marlus` via `.env`), tela de login, layout base, healthchecks, README. *Aceite:* `docker compose up` → login funciona.

**Fase 2 — Cadastros:** contas, cartões, categorias, configurações. *Aceite:* CRUD completo com validações e testes.

**Fase 3 — Lançamentos, faturas e recorrências:** transações, faturas com regra de fechamento/vencimento, pagar fatura, recorrências materializadas. *Aceite:* testes do cálculo de fatura passando.

**Fase 4 — Parcelamentos e compromissos futuros:** planos de parcela (inclusive em andamento e com juros), tela de compromissos 12–24 meses. *Aceite:* colchão 12x R$ 345 e celular 10x R$ 325 aparecem nos meses certos, com fim e "valor liberado".

**Fase 5 — Receitas, extras, dízimo e orçamento:** fontes de receita (bolsa temporária, 13º, extras), renda base, dízimo automático, tetos por categoria, dashboard do mês. *Aceite:* bolsa e 13º fora da renda base; dízimo de 10% sobre tudo.

**Fase 6 — Simulador de compra segura.** *Aceite:* cenários numéricos testados; veredito e valor máximo de parcela seguro.

**Fase 7 — Investimentos, aposentadoria e metas:** ativos, operações, proventos de FIIs, alocação alvo, projeção com cenários (nominal e real), reserva e casa. *Aceite:* projeção de R$ 500/mês por 30 anos a 8% a.a. ≈ R$ 704 mil nominal.

**Fase 8 — Exportar/Importar:** ZIP + CSV, manifest, dry-run, modos, round-trip testado, importador de planilha mensal (bônus). *Aceite:* exportar num ambiente e importar em outro banco vazio reproduz os dados.

**Fase 9 — Acabamento:** alertas, calendário, gráficos, acessibilidade, documentação, backup/restore, seed-demo. *Aceite:* checklist de UX e README completo.

---

## 16. Definição de pronto (para cada fase)

- Testes passando (`make test`) e `docker compose up --build` sem erro.
- Migrations novas aplicadas do zero em banco vazio.
- Sem valores monetários em `double`/`float`.
- Endpoints documentados no OpenAPI.
- README/DECISIONS atualizados; commit feito; instruções curtas de como eu valido manualmente.

**Comece agora pelo passo 2 da seção 0: apresente o plano (estrutura de pastas, modelo de dados resumido e decisões pendentes) e aguarde minha aprovação.**
