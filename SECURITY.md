# Política de segurança

## Escopo

O VBank Sandbox é software educacional, não uma instituição financeira e não deve armazenar dados pessoais reais nem processar dinheiro real. Mesmo assim, falhas que comprometam autenticação, autorização, integridade do ledger, segredos ou isolamento entre usuários são tratadas como vulnerabilidades.

## Como reportar

Não abra uma issue pública com detalhes exploráveis. Use o recurso **Report a vulnerability** na aba Security do repositório GitHub. Se esse recurso ainda não estiver habilitado, contate o mantenedor por um canal privado indicado no perfil do repositório.

Inclua versão/commit, impacto, passos mínimos de reprodução e uma sugestão de correção, se houver. Não inclua dados reais, tokens ou credenciais.

## Prazo esperado

O mantenedor buscará confirmar o recebimento em até 7 dias e fornecer uma avaliação inicial em até 14 dias. Prazos de correção dependem da severidade e da disponibilidade voluntária do mantenedor.

## Versões suportadas

Por ser um projeto demonstrativo, apenas a versão mais recente da branch `main` recebe correções de segurança.

## Práticas para deploy

- Gere `JWT_SECRET` com pelo menos 64 bytes aleatórios.
- Use conexão Neon com TLS e pool (`-pooler`, `sslmode=require`).
- Não habilite bootstrap de administrador em produção após o primeiro uso.
- Mantenha cookies `HttpOnly`, `Secure` e `SameSite` conforme a topologia do deploy.
- Proteja ou desabilite Swagger/OpenAPI se o ambiente deixar de ser público e educacional.
- Nunca registre senhas, PINs, tokens ou a connection string completa.

Veja a análise completa em `docs/SECURITY-ARCHITECTURE.md`.
