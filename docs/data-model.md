# Modelo de dados

Visão geral do schema, atualizada a cada fase. Detalhes completos em
`prompt_claude_code_financas.md` (seção 6). Este documento resume o que já foi
implementado.

## Fase 1

### `app_user`
| Coluna | Tipo | Notas |
|---|---|---|
| id | UUID (PK) | UUID v7 |
| username | VARCHAR, UNIQUE, NOT NULL | |
| password_hash | VARCHAR, NOT NULL | BCrypt |
| display_name | VARCHAR, NOT NULL | |
| birth_date | DATE | nullable |
| created_at | TIMESTAMPTZ, NOT NULL | preenchido por auditoria JPA |
| updated_at | TIMESTAMPTZ, NOT NULL | preenchido por auditoria JPA |

## Fase 2

Todas as tabelas abaixo têm `user_id` (FK `app_user`), `created_at` e `updated_at`.

### `account`
name, type (`CHECKING`\|`SAVINGS_BOX`\|`BROKERAGE`\|`CASH`), institution, initial_balance
`NUMERIC(14,2)`, purpose (`DAILY`\|`EMERGENCY_RESERVE`\|`GOAL`\|`INVESTMENT`), archived.

### `credit_card`
name, issuer, credit_limit `NUMERIC(14,2)`, closing_day/due_day (`SMALLINT`, 1–31),
default_payment_account_id (FK `account`, opcional), color, archived.

### `category`
name, kind (`INCOME`\|`EXPENSE`), nature (`FIXED`\|`VARIABLE`), parent_id (FK `category`,
opcional — precisa ter o mesmo `kind` do filho), icon, color. Sem soft delete: exclusão é
física e bloqueada se existirem subcategorias ou orçamentos vinculados.

### `budget`
category_id (FK `category`), month (`VARCHAR(7)`, formato `AAAA-MM`; `NULL` = teto padrão da
categoria), limit_amount `NUMERIC(14,2)`. Índices únicos parciais garantem no máximo um teto
padrão e um teto por mês por categoria.

### `app_setting`
key/value por usuário (`UNIQUE(user_id, key)`). Chaves conhecidas: `tithe_percent` (10),
`installment_limit_percent` (30), `emergency_months_target` (6), `currency` (BRL) — valores
ausentes assumem esses padrões (não são gravados até o usuário salvar as configurações).

## Fase 3

### `recurring_rule`
description, amount, amount_is_variable, frequency (`MONTHLY`\|`YEARLY`), day_of_month
(1–31), start_date, end_date (opcional), category_id, e **exatamente um** entre card_id/
account_id (`CHECK`), active.

### `invoice`
card_id, reference_month (`VARCHAR(7)`, `AAAA-MM`), closing_date, due_date, status
(`OPEN`\|`CLOSED`\|`PAID`), paid_amount, paid_on, paid_from_account_id.
`UNIQUE(card_id, reference_month)` — uma fatura por cartão por competência.

### `transaction`
kind (`INCOME`\|`EXPENSE`\|`TRANSFER`), description, amount, date, category_id (opcional),
account_id/card_id (nunca os dois — `CHECK`), invoice_id (preenchido automaticamente quando
`card_id` está presente), status (`PLANNED`\|`PAID`), installment_plan_id/installment_number/
income_entry_id (colunas já existem, sem FK ainda — tabelas chegam nas fases 4 e 5),
recurring_rule_id (preenchido quando o lançamento vem de uma recorrência), notes.

## Fase 4

## Fase 5

### `income_source`
name, type (`SALARY`\|`SCHOLARSHIP`\|`THIRTEENTH`\|`EXTRA`\|`OTHER`), recurrence
(`MONTHLY`\|`TEMPORARY`\|`SPORADIC`), expected_amount, pay_day, start_date/end_date,
expected_months, tithe_applies, counts_in_base_budget. Regra: bolsa/13º/extras têm
`counts_in_base_budget=false` (ficam fora da renda base) e normalmente `tithe_applies=true`.

### `income_entry`
source_id, account_id (opcional), reference_month, received_on, amount, status
(`EXPECTED`\|`RECEIVED`).

### `tithe_ledger`
Uma linha por `(user_id, reference_month)` (`UNIQUE`), recalculada automaticamente sempre que
um `income_entry` **recebido** de uma fonte com `tithe_applies=true` muda: base_amount = soma
dessas receitas no mês, due_amount = base × percentual configurado (padrão 10%), paid_amount/
paid_on/status conforme os pagamentos registrados.

### `installment_plan`
card_id, description, purchase_date, total_amount, installment_count, installment_amount
(valor "representativo" — base sem juros, ou parcela fixa da Tabela Price com juros),
first_installment_number (1 para plano novo; >1 para "já em andamento"), first_invoice_month
(`AAAA-MM`), interest_rate_monthly (opcional), category_id. `transaction.installment_plan_id`
ganha FK de verdade nesta fase (a tabela já existia como coluna solta desde a Fase 3).
