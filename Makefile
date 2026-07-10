SHELL:=/bin/bash

##@ Java

.PHONY: test
test: ## Run integration tests (unit + container)
	@echo -e "\033[0;32m==================> Running $@ ============\033[0m"
	mvn clean test -Pintegration

.PHONY: unit-test
unit-test: ## Run unit tests only
	@echo -e "\033[0;32m==================> Running $@ ============\033[0m"
	mvn test

.PHONY: build
build: ## Build the project (skip tests)
	@echo -e "\033[0;32m==================> Running $@ ============\033[0m"
	mvn -B package -DskipTests=true -Dmaven.javadoc.skip=true

.PHONY: format-check
format-check: ## Check code style with Checkstyle
	@echo -e "\033[0;32m==================> Running $@ ============\033[0m"
	mvn checkstyle:check -DskipTests=true

.PHONY: help
help: ## Show this help
	@echo -e "\033[1;3;34mobvector4j - OceanBase Vector JDBC Client.\033[0m\n"
	@echo -e "Usage:\n  make \033[36m<Target>\033[0m\n\nTargets:"
	@awk 'BEGIN {FS = ":.*##"; printf ""} /^[a-zA-Z_0-9-]+:.*?##/ { printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)
