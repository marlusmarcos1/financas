# Finanças do Marlus

App local e self-hosted para organização financeira pessoal: contas, cartões de
crédito, parcelamentos, dízimo, aposentadoria, investimentos e metas.

Consulte `prompt_claude_code_financas.md` para a especificação completa do produto,
`docs/DECISIONS.md` para decisões técnicas registradas ao longo da implementação e
`docs/data-model.md` para o resumo do schema.

## Stack

- **Back-end:** Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Flyway, PostgreSQL 16.
- **Front-end:** React 18, TypeScript, Vite, TanStack Query, React Hook Form + Zod, Tailwind CSS.
- **Infra:** Docker + docker compose (`db`, `api`, `web`).

## Subir o projeto

1. Copie o arquivo de variáveis de ambiente e ajuste se quiser:

   ```bash
   cp .env.example .env
   ```

2. Suba tudo com Docker:

   ```bash
   make up
   ```

   (equivalente a `docker compose up -d --build`)

3. Acesse **http://127.0.0.1:8080** e entre com o usuário `marlus` / senha `marlus`.

> ⚠️ **A senha padrão `marlus`/`marlus` é só para uso local.** Troque-a nas
> Configurações antes de expor o serviço em qualquer rede além de `127.0.0.1`.

Outros comandos úteis (veja o `Makefile`):

```bash
make down       # para os containers
make logs       # acompanha os logs
make test       # roda os testes do back-end e do front-end
make backup-db  # gera um dump do banco em ./backups
make restore-db FILE=backups/arquivo.sql  # restaura um backup
```

## Desenvolvimento local (sem Docker)

**Back-end** (requer JDK 21 — o wrapper `./mvnw` baixa o Maven automaticamente):

```bash
cd backend
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`, com Swagger UI em `/swagger-ui.html`.

**Front-end** (requer Node 20+):

```bash
cd frontend
npm install
npm run dev
```

O Vite sobe em `http://localhost:5173` e faz proxy de `/api` para `localhost:8080`.

## Exportar / importar dados

A funcionalidade de exportar/importar tudo em CSV (para levar os dados a outro
computador) chega na Fase 8 do projeto — veja `prompt_claude_code_financas.md`,
seção 15, para o plano de fases completo.

## Backup

Rode `make backup-db` periodicamente (ou antes de qualquer atualização grande) para
gerar um dump em `./backups`. Assim que a Fase 8 (exportar/importar) estiver pronta,
prefira também exportar os dados em CSV de tempos em tempos — é o formato pensado
para levar o app para outra máquina.

## Estado do projeto

Fase 1 (Fundação) concluída: Docker Compose, autenticação com usuário inicial,
tela de login, layout base. As fases seguintes estão descritas na seção 15 da
especificação e são implementadas incrementalmente, cada uma com seu próprio commit.
