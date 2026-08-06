# Como contribuir

Obrigado por considerar uma contribuição ao VBank Sandbox. Este projeto é uma simulação educacional e nunca deve ser apresentado como instituição financeira real.

## Preparação

1. Faça um fork e crie uma branch a partir de `main`.
2. Instale Java 21 e Node.js 24 LTS ou use os wrappers/documentação do projeto.
3. Copie `.env.example` para `.env` e use somente credenciais locais ou de desenvolvimento.
4. Consulte `README.md`, `docs/ARCHITECTURE.md` e `docs/SECURITY-ARCHITECTURE.md` antes de alterar regras financeiras.

## Antes do pull request

Execute:

```bash
cd backend && ./mvnw test
cd ../frontend && npm ci && npm test && npm run build
```

Se Docker estiver disponível, execute também:

```bash
cd backend && ./mvnw -Pintegration-test verify
cd .. && docker build -t vbank-sandbox .
```

Não inclua `.env`, credenciais, dumps de banco, `node_modules`, `target` ou `dist`. Mudanças no banco devem ser uma nova migração Flyway; não edite uma migração já publicada.

## Pull requests

- Mantenha a mudança pequena e com propósito claro.
- Inclua testes para o comportamento alterado.
- Descreva impacto em segurança, dados e compatibilidade quando aplicável.
- Preserve valores monetários como centavos inteiros e a dupla entrada do ledger.
- Use linguagem inclusiva e siga o `CODE_OF_CONDUCT.md`.

Ao contribuir, você concorda que sua contribuição será licenciada sob a licença MIT do projeto.
