.PHONY: up down logs test backup-db restore-db seed-demo

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

# Carrega dados de exemplo (seção 10) para o usuário administrador, via a API já no ar
# (docker compose up). Idempotente — rodar de novo não duplica nada.
seed-demo:
	@set -e; \
	COOKIES=$$(mktemp); \
	ADMIN_USER=$${APP_ADMIN_USER:-marlus}; \
	ADMIN_PASS=$${APP_ADMIN_PASSWORD:-marlus}; \
	BASE_URL=http://127.0.0.1:8080; \
	curl -sf -c "$$COOKIES" "$$BASE_URL/api/v1/auth/csrf" > /dev/null; \
	CSRF=$$(grep XSRF-TOKEN "$$COOKIES" | awk '{print $$NF}'); \
	curl -sf -b "$$COOKIES" -c "$$COOKIES" -H "X-XSRF-TOKEN: $$CSRF" -H "Content-Type: application/json" \
		-d "{\"username\":\"$$ADMIN_USER\",\"password\":\"$$ADMIN_PASS\"}" \
		"$$BASE_URL/api/v1/auth/login" > /dev/null; \
	CSRF=$$(grep XSRF-TOKEN "$$COOKIES" | awk '{print $$NF}'); \
	curl -sf -b "$$COOKIES" -c "$$COOKIES" -H "X-XSRF-TOKEN: $$CSRF" -X POST "$$BASE_URL/api/v1/seed-demo"; \
	echo; \
	rm -f "$$COOKIES"
