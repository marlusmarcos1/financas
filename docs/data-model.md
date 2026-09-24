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
