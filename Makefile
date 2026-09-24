.PHONY: up down logs test backup-db restore-db

up:
	docker compose up -d --build

down:
	docker compose down

logs:
	docker compose logs -f

test:
	cd backend && ./mvnw -q test || mvn -q test
	cd frontend && npm test

# Uso: make backup-db
backup-db:
	mkdir -p backups
	docker compose exec -T db pg_dump -U $${POSTGRES_USER:-financas} $${POSTGRES_DB:-financas} \
		> backups/financas_$$(date +%Y%m%d_%H%M%S).sql
	@echo "Backup salvo em backups/"

# Uso: make restore-db FILE=backups/financas_20260101_120000.sql
restore-db:
	@test -n "$(FILE)" || (echo "Use: make restore-db FILE=backups/arquivo.sql" && exit 1)
	docker compose exec -T db psql -U $${POSTGRES_USER:-financas} $${POSTGRES_DB:-financas} < $(FILE)
