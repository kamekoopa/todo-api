
.PHONY: docker-build-local-es
docker-build-local-es:
	docker-compose -f docker-compose.local-db.yml build es

.PHONY: compose-up-local-dbs
compose-up-local-dbs:
	docker-compose -f docker-compose.local-db.yml up -d rdb es

.PHONY: test
test: compose-up-local-dbs
	sbt test
