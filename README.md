# Finanças do Marlus

App local e self-hosted para organização financeira pessoal: contas, cartões de
crédito, parcelamentos, receitas, dízimo, orçamento, simulador de compra parcelada,
investimentos, aposentadoria, metas, exportação/importação de dados, alertas e
calendário de vencimentos.

Consulte `prompt_claude_code_financas.md` para a especificação completa do produto,
`docs/DECISIONS.md` para decisões técnicas registradas ao longo da implementação,
`docs/data-model.md` para o resumo do schema e `docs/BACKLOG.md` para ideias fora do
escopo atual.

## Stack

- **Back-end:** Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Flyway, PostgreSQL 16.
- **Front-end:** React 18, TypeScript, Vite, TanStack Query, React Hook Form + Zod, Tailwind CSS, Recharts.
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
make seed-demo  # carrega dados de exemplo no usuário admin (idempotente)
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

## Dados de exemplo (seed-demo)

Para explorar o app sem cadastrar tudo na mão, rode `make seed-demo` com o compose já
no ar (ou clique em "Carregar dados de exemplo" em Configurações). Carrega cartões,
receitas, parcelamentos em andamento, recorrências, plano de aposentadoria e metas
baseados na situação descrita na especificação (seção 10) — tudo editável ou removível
pela interface depois. É idempotente: rodar de novo não duplica nada.

## Exportar / importar dados

Em **Exportar / Importar**: "Exportar tudo" baixa um `.zip` com um CSV por tipo de
dado e um `manifest.json` (checksum SHA-256 por arquivo, versão de esquema). A
importação tem uma etapa de validação (dry-run, não grava nada) e dois modos —
**Mesclar** (atualiza por id, padrão) e **Substituir tudo** (apaga os dados atuais
antes de importar, exige digitar "SUBSTITUIR"). Tudo roda em uma única transação: se
algo falhar, nada é gravado. Cada importação fica registrada no histórico da própria
tela.

## Backup

Rode `make backup-db` periodicamente (ou antes de qualquer atualização grande) para
gerar um dump em `./backups`. Prefira também exportar os dados em CSV de tempos em
tempos (seção acima) — é o formato pensado para levar o app para outra máquina.

## Alertas e calendário

O Dashboard mostra alertas (teto de categoria estourando, fatura fechando/vencendo,
bolsa perto do fim, sobra do mês negativa, comprometimento com parcelas acima do
limite configurado), o checklist "pague-se primeiro" (dízimo, aposentadoria, reserva)
e a evolução patrimonial dos últimos 6 meses. **Calendário** lista os próximos
vencimentos (faturas, parcelas, recorrências) com horizonte configurável de 7 a 60
dias.

## Checklist de UX (seção 8 / definição de pronto)

- [x] Estados de carregamento, vazio e erro em todas as telas com dados assíncronos.
- [x] Confirmação antes de ações destrutivas (excluir, substituir tudo na importação).
- [x] Feedback via toasts para ações de criar/editar/excluir/importar/exportar.
- [x] Formulários com validação client-side (Zod) e mensagens de erro em pt-BR.
- [x] Lançar uma despesa em poucos cliques (formulário em modal, sem navegação extra).
- [x] Modo escuro/claro seguindo a preferência do sistema.
- [x] Navegação lateral cobrindo todas as 16 telas da seção 8, incluindo Calendário.

## Estado do projeto

Todas as 9 fases da especificação (seção 15) foram implementadas: fundação e
autenticação; cadastros; lançamentos/faturas/recorrências; parcelamentos e
compromissos futuros; receitas/dízimo/orçamento; simulador de compra segura;
investimentos/aposentadoria/metas; exportar/importar; e acabamento (alertas,
calendário, evolução patrimonial, seed-demo). Cada fase tem seu próprio commit — veja
`git log` e `docs/DECISIONS.md` para o histórico de decisões técnicas.

Itens explicitamente fora do escopo atual (bônus opcionais da própria especificação e
melhorias futuras) estão listados em `docs/BACKLOG.md`.
