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

### `server.servlet.encoding.force=true` explícito
Sem isso, o `Content-Type` das respostas JSON não declara `charset`, e o
`MockHttpServletResponse.getContentAsString()` (sem argumento) usado nos testes de
integração cai para ISO-8859-1 por padrão do Servlet, corrompendo acentos só na leitura do
corpo da resposta nos testes (a aplicação real e o browser sempre tratam JSON como UTF-8,
então não havia bug em produção — só nos testes). Forçar UTF-8 na resposta evita ter que
lembrar de `getContentAsString(StandardCharsets.UTF_8)` em cada teste.

### `TransactionKind.TRANSFER` implementado de forma mínima
A especificação lista `TRANSFER` como um dos tipos de lançamento mas não detalha suas regras
de negócio (ex.: conta de origem/destino) em nenhuma seção posterior. Tratamos por ora como
uma variação de lançamento de conta única (mesma exigência de `account_id` que `INCOME`),
sem inventar um modelo de duas pontas que a spec não pediu. Registrar aqui para revisitar
se alguma fase futura detalhar transferências entre contas.

## Fase 4

### Arredondamento: `HALF_EVEN`
A seção 3 pede para documentar a escolha entre `HALF_EVEN`/`HALF_UP`. Usamos `HALF_EVEN`
("banker's rounding") em todas as divisões monetárias (parcela sem juros, Tabela Price):
é o padrão do `BigDecimal.ROUND_HALF_EVEN`/`RoundingMode.HALF_EVEN`, reduz viés sistemático
de arredondamento em séries longas de cálculos (relevante para 12–24 parcelas) e é o
comportamento usado por sistemas financeiros como o padrão IEEE 754.

### Parcela sem juros: ajuste de centavos só na última; com juros (Tabela Price): parcelas iguais
A seção 7.2 pede ajuste de centavos na última parcela para o caso simples (total/n). Para
parcelamento com juros, a Tabela Price já produz parcelas matematicamente iguais por
construção (esse é o objetivo do sistema de amortização francês) — não há "total informado
pelo usuário" para reconciliar contra a soma das parcelas, então não aplicamos nenhum ajuste
adicional na última parcela nesse caso.

### `installment_plan.installment_amount` guarda um valor representativo, não uma lista
O campo é singular (`NUMERIC`), como no modelo de dados da seção 6. Quando há ajuste de
centavos (parcela sem juros com divisão não exata), ele guarda a parcela "base" (todas menos
a última); o valor exato de cada parcela fica nas `transaction`s geradas, não duplicado aqui.

### "Compromissos futuros": leitura direta das `transaction`s já materializadas
Em vez de recalcular parcelas/recorrências sob demanda, o endpoint `/api/v1/commitments` soma
as `transaction`s que já têm `installment_plan_id` ou `recurring_rule_id` preenchido, agrupadas
por mês. Isso reaproveita a materialização das Fases 3/4 (nenhuma lógica de projeção
duplicada) e garante que a tela sempre reflita exatamente o que está lançado.

## Fase 5

### Dízimo calculado sobre receitas **recebidas**, não esperadas
A base do `tithe_ledger` soma apenas `income_entry` com `status=RECEIVED`. Cobrar dízimo
sobre receita ainda não recebida (`EXPECTED`) anteciparia uma obrigação sobre dinheiro que
ainda não entrou — contraria a lógica de "dízimo sobre tudo que entra" da seção 1/7.5.

### "Sobra normalizada" (seção 7.3) adiada
A spec pede, além da "sobra do mês", uma versão normalizada "sem itens fora do padrão". Como
"fora do padrão" não é definido precisamente e depende de médias históricas mais robustas
(gastos variáveis dos últimos 3–6 meses), implementamos por ora só a sobra simples (renda
base recebida − despesas do mês − dízimo pendente) e deixamos a versão normalizada para a
Fase 9 (Acabamento), quando também entram os alertas do dashboard.

### `allocation_rule` não implementada nesta fase
A seção 6 lista `allocation_rule` na "Destinação do dinheiro", mas o plano de fases (seção 15)
não a exige na Fase 5 — o critério de aceite é só "bolsa e 13º fora da renda base; dízimo de
10% sobre tudo", que não depende dela. Fica para a Fase 7, quando aposentadoria/metas
realmente consomem uma regra de destinação percentual.

## Fase 6

### Simulador é stateless; renda base assume só fontes MENSAIS ativas
Diferente do dashboard (que usa `income_entry`s já lançadas do mês), o simulador projeta
meses futuros onde ainda não existem lançamentos de receita. Por isso a "renda base mensal"
usada na projeção é a soma de `income_source.expected_amount` das fontes com
`recurrence=MONTHLY` e `counts_in_base_budget=true` — não depende de `income_entry`s
existirem para os meses futuros. O dízimo projetado usa essa mesma base × percentual
configurado, não o `tithe_ledger` real (que só existe para meses já lançados).

### Precedência do veredito: Não recomendado > Atenção > Seguro, por mês, pior caso vence
As três faixas da seção 7.4 não são mutuamente exclusivas como descritas (ex.: comprometimento
>40% também é >30%). Aplicamos precedência explícita — primeiro checamos as condições de "Não
recomendado" (saldo negativo ou >40%), depois "Atenção" (>30% ou saldo <10% da renda), e o
veredito final do mês é o pior entre todos os meses projetados.

### "Valor máximo de parcela seguro hoje" considera só o teto de parcelas e o saldo livre
Calculado por mês como `min(limite% × renda base − parcelas já existentes, saldo livre sem a
nova compra)`, tomando o mínimo entre todos os meses projetados. Não considera despesas
variáveis futuras hipotéticas além da média histórica já embutida no saldo livre — manter o
cálculo determinístico e auditável era mais importante aqui do que tentar prever variações.

### Sugestões de alternativas são heurísticas simples, não otimização
"Parcelar em menos vezes" sugere metade do número de parcelas atual; "esperar" aponta o
parcelamento existente com o fim mais próximo dentro do horizonte projetado; a sugestão de
"reserva de emergência" é textual (a Fase 7 ainda não calcula saldo de reserva). São heurísticas
de UX, não uma busca pela alternativa ótima — evita complexidade desproporcional ao pedido da
seção 7.4 ("sugerir alternativas").

### Corrige HEALTHCHECK do frontend (bug pré-existente da Fase 1)
O `HEALTHCHECK` do `frontend/Dockerfile` usava `wget http://localhost:80/`; dentro do
container, `localhost` resolve primeiro para `::1` (IPv6), mas o nginx só escuta em
`0.0.0.0:80` (IPv4), então o healthcheck falhava com "connection refused" mesmo com o site
funcionando normalmente para requisições externas. Trocado para `http://127.0.0.1:80/`.

## Fase 7

### Saldo de conta é aproximado, não um ledger de saldo corrente
O app não mantém um saldo por lançamento (não há requisito nesse sentido nas fases
anteriores). `AccountBalanceService` deriva o saldo sob demanda: saldo inicial + lançamentos
`INCOME` na conta + `income_entry`s recebidos nela − lançamentos `EXPENSE`/`TRANSFER` da conta
− faturas pagas a partir dela. Isso cobre os fluxos que o app já registra, mas não é um livro-
-razão auditável linha a linha. Suficiente para "patrimônio total" e "reserva atual" (seção
7.6/7.7); se um ledger de verdade for necessário depois, revisitar aqui.

### Reserva de emergência: gastos "essenciais" = todas as despesas (sem marcação própria)
A seção 7.7 pede a média de gastos "fixos + variáveis essenciais" dos últimos 6 meses.
Categorias não têm uma flag de "essencial" — só `nature` (`FIXED`/`VARIABLE`). Usamos a média
de TODAS as despesas dos últimos 6 meses como aproximação, documentado no próprio
`EmergencyReserveService`. Adicionar uma flag "essencial" em `category` é uma opção futura se
o usuário achar a média superestimada (por incluir gastos supérfluos).

### Preço médio ponderado: venda não altera o preço médio; ganho realizado é derivado, não guardado
Documentando a escolha pedida na seção 6: cada compra recalcula
`preço médio = custo total acumulado / quantidade total`; vendas reduzem a quantidade mas
mantêm o preço médio (o ganho/perda da venda é `quantidade vendida × (preço de venda − preço
médio)`, calculado on-the-fly em `PositionCalculator`, não persistido). Dividendo/JCP/juros só
somam a "renda total" do ativo, sem afetar posição ou preço médio.

### Projeção de aposentadoria: taxa real via `(1+nominal)/(1+inflação)−1`, não deflação do total
A seção 7.6 dá a fórmula da taxa real explicitamente. Em vez de simplesmente deflacionar o
saldo nominal final por `(1+inflação)^anos`, rodamos a MESMA simulação mês a mês usando essa
taxa real no lugar da nominal — é a leitura mais literal da fórmula dada e evita presumir uma
correção só no valor final.

### 3 cenários de aposentadoria são sempre 6%/8%/10%, independente do `expected_return_nominal_annual` do plano
A seção 7.6 pede "cenários prontos (pessimista 6%, base 8%, otimista 10%)" como algo fixo,
distinto do campo `expected_return_nominal_annual` armazenado no plano (que registra a
expectativa do próprio usuário, editável, mas não usado para gerar um 4º cenário — evita
poluir a comparação lado a lado que a spec pede).

### Meta "casa" + ativo de renda variável: alerta é heurístico, não bloqueio
Seguindo a seção 7.7 ("exibir aviso"), o alerta aparece quando existe algum
`investment_asset` não arquivado com `purpose=HOUSE` em classe de renda variável (FII/ação/
ETF/cripto) e a meta tem `type=HOUSE` com `target_date` a menos de 3 anos. Não impede o
cadastro nem sugere venda automaticamente — é só um aviso textual, a decisão fica com o
usuário.

## Fase 8

### Upsert por UUID: importar não gera IDs novos, usa o UUID da própria linha do CSV
A seção 9 pede "MERGE (atualiza por id)". Cada `EntityCsvHandler.importRow` faz
`findByIdAndUserId` — se existe, atualiza; se não existe mas o UUID já está em uso por
**outro** usuário (`existsById`), a linha vira erro ("id já pertence a outro registro") em
vez de ser silenciosamente ignorada ou sobrescrita, porque o `id` é chave primária global da
tabela, não escopada por usuário. Isso significa que reimportar o export de um usuário
dentro de outro usuário (ex.: copiar dados entre contas) nunca funciona por MERGE — é
esperado: o cenário coberto é "exportei meus dados, preciso restaurá-los" (mesmo usuário,
banco vazio ou não), não "clonar dados para outra conta".

### Entidades sem setters (parcelamento, movimentação de investimento, aporte de meta) são "imutáveis" na importação
`InstallmentPlan`, `InvestmentTransaction` e `GoalContribution` não expõem setters (decisão
das fases 4/7 — criá-los dispara efeitos colaterais como gerar faturas/lançamentos). Na
importação, se a linha já existe e os valores batem exatamente com o que está no banco, conta
como "atualizado" (no-op); se algum campo diverge, vira erro pedindo para excluir e recriar em
vez de tentar uma edição parcial que quebraria a consistência dessas entidades. `Budget.
categoryId`/`month`, `IncomeEntry.sourceId`, `AllocationTarget.(purpose,assetClass)`,
`AppSetting.key` e os campos imutáveis de `Invoice`/`TitheLedger` (cartão/mês/fechamento/
vencimento e mês de referência) seguem a mesma regra por serem identidade/chave lógica, não
apenas um valor editável.

### Fatura e dízimo: valor pago do CSV é absoluto, mas as entidades só somam delta
`Invoice.registerPayment` e `TitheLedger.registerPayment` foram desenhados nas fases 3/5 para
"registrar um pagamento adicional" (soma ao já pago), não para "definir o valor pago". Como o
CSV traz o valor pago **absoluto** (mais simples de auditar/editar manualmente), o handler
calcula `delta = valorDoCsv − valorAtualNoBanco` e chama `registerPayment(delta, ...)` — dá o
mesmo resultado sem duplicar as entidades com um segundo método `setPaidAmount`.

### Validação de import cobre tipo/formato, não integridade referencial entre entidades
`CsvFieldParser` garante que cada campo tem o tipo certo (UUID, decimal, data ISO, enum,
booleano) e reporta erro de linha em pt-BR quando não tem. Não verificamos se um
`category_id`/`account_id`/etc. referenciado por uma linha realmente existe — isso fica a
cargo da constraint de FK do Postgres no momento do `apply` (dentro da transação única, que
faz rollback de tudo se qualquer coisa falhar). Verificar FK entre 18 entidades no dry-run
exigiria injetar todos os repositórios em todos os handlers só para essa checagem; como o
caso de uso real é "reimportar meu próprio export" (FKs sempre válidas, já que vieram do
próprio banco), não vale a complexidade agora — fica registrado aqui como limite conhecido.

### `manifest.json` com checksum SHA-256 por arquivo; importação rejeita adulteração ou versão futura
Cada CSV dentro do zip tem seu SHA-256 gravado no manifesto; na importação, qualquer
divergência (arquivo editado à mão de forma inconsistente, corrupção de transferência) aborta
a importação inteira antes de tocar no banco. `schema_version` (hoje `"1"`) permite rejeitar
com uma mensagem clara um arquivo exportado por uma versão futura do app; versões antigas
seriam aceitas via conversores quando o formato mudar de forma incompatível — ainda não
necessário, só existe uma versão.

### REPLACE ("Substituir tudo") exige o texto literal "SUBSTITUIR" e roda em duas passadas dentro da mesma transação
Primeiro valida tudo com `apply=false` (nenhuma escrita); só se zero erros é que apaga os
dados do usuário (ordem inversa de dependência, para respeitar FKs) e reimporta com
`apply=true` — tudo dentro do mesmo `@Transactional`, então qualquer falha inesperada na
segunda passada desfaz também o apagamento. Sem a palavra de confirmação exata, a requisição
é rejeitada antes de ler o zip.

### Sem `allocation_rules.csv`
Não existe entidade `allocation_rule` no banco (decisão da Fase 5: alocação-alvo já cobre a
seção 7.7 sem uma tabela de regras separada) — não há o que exportar/importar com esse nome.

## Fase 9

### "E se a bolsa acabar agora?" não precisou de cálculo próprio
A seção 11 pede essa simulação como "orçamento só com a renda base". Como `DashboardService`
(Fase 5) já separa `baseIncomeReceived` de `extrasReceived` — e a bolsa, com
`counts_in_base_budget=false`, sempre cai em "extras" — a `sobra` do mês já é exatamente esse
cenário. `DashboardExtrasService` só adiciona a contagem regressiva (meses restantes por fonte
`TEMPORARY`) e um texto explicando isso na tela; nenhuma rota nova de simulação foi criada.

### Checklist "pague-se primeiro": item de aposentadoria sempre aparece como pendente
Não existe hoje um registro de "este mês eu já fiz o aporte de aposentadoria" — `retirement_plan`
guarda só o valor mensal planejado, não um histórico de transferências. Marcar esse item como
"feito" exigiria criar esse rastreamento só para isso; por ora o checklist sempre mostra o valor-
-alvo com `done=false`, servindo de lembrete, não de confirmação automática. Dízimo (via
`tithe_ledger.paid_amount` vs `due_amount`) e reserva (via meta de `EmergencyReserveService`)
já têm dado suficiente para marcar "feito" de verdade.

### Alertas: limiares fixos (3 dias para fatura, 2 meses para bolsa terminando)
A seção 8 dá exemplos ("fatura fecha em 2 dias") sem fixar o número. Escolhidos 3 dias para
fechamento/vencimento de fatura e 2 meses para o fim de uma receita `TEMPORARY` (bolsa) — dá
tempo de reação sem poluir o dashboard com avisos cedo demais. Não são configuráveis por
enquanto; se o usuário achar cedo/tarde demais, é um ajuste pontual nessas constantes.

### Evolução patrimonial: contas exatas, investimentos por custo (não por cotação)
Documentado também no Javadoc de `NetWorthHistoryService`: o saldo de contas em cada mês passado
é reconstruído com precisão a partir do histórico de lançamentos (mesma lógica de
`AccountBalanceService`, com um corte de data). Investimentos usam o capital aportado até a
data (via `PositionCalculator` sobre as transações filtradas), não o valor de mercado histórico
— o app não tem cotação automática (seção 13), então não haveria como saber o preço de um ativo
em um mês passado. Registrado também em BACKLOG.md como possível melhoria futura.

### `seed-demo`: endpoint autenticado idempotente, não um profile/perfil Spring separado
Cogitamos um `ApplicationRunner` ativado por variável de ambiente/profile, mas isso exigiria
reiniciar o container para (des)ativar. Em vez disso, `POST /api/v1/seed-demo` roda para o
usuário logado, verifica um marcador (`app_setting` com chave `seed_demo_applied`) e não faz
nada se já rodou — mais simples, funciona com o container já no ar, e dá pra chamar tanto de
`make seed-demo` (via curl com login) quanto de um botão em Configurações.
