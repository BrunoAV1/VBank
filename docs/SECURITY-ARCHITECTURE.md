# Arquitetura de segurança

## Modelo de ameaça educacional

O sistema protege contas fictícias contra acesso indevido, repetição de requisição, condições de corrida e exposição acidental. Ele não foi auditado para dinheiro real, conformidade regulatória ou alta criticidade.

## Controles

- BCrypt fator 12 para senha e PIN;
- JWT HMAC com segredo mínimo de 32 bytes e expiração curta;
- refresh aleatório, armazenado somente como SHA-256, rotativo e revogável;
- cookie HttpOnly, `SameSite=Lax` e `Secure` em produção;
- papéis `USER`, `ADMIN`, `SYSTEM` e autorização por endpoint;
- validação Jakarta no backend; Zod é apenas feedback de frontend;
- rate limit persistido para login, PIN, resolução e transferência;
- cinco erros de PIN causam bloqueio de 15 minutos no PostgreSQL;
- locks e constraints no banco; idempotência única;
- Problem Details sem stack trace em produção;
- trace ID aleatório sem dados pessoais;
- auditoria sanitiza palavras associadas a credenciais;
- CORS somente `localhost:5173` em dev; ausente em prod;
- JRE como usuário não-root e término gracioso;
- nenhuma escrita persistente no filesystem.

## Dados proibidos

Não colete ou armazene CPF, CNPJ, cartão, conta bancária real, chave Pix real, documento governamental, segredo de terceiro ou dinheiro. Telefone/e-mail são identificadores internos de demonstração.

## Limitações conhecidas

- O rate limiter simples usa bucket por janela; não substitui proteção de borda/WAF.
- Não há recuperação de senha por e-mail para evitar provedor externo.
- Não há segundo fator externo.
- JWT continua criptograficamente válido até expirar, mas o filtro consulta status atual antes de autenticar.
- O projeto não inclui antivírus, SIEM, KMS dedicado, pen test ou SLA.
- Administradores têm poder de ajuste; proteja suas credenciais e desative o bootstrap.

## Segredos

Somente variáveis de processo/Vercel recebem credenciais. `.env`, `.env.local`, `.vercel` e outputs são ignorados. Não use variáveis `VITE_*` para segredo: elas entram no bundle do navegador.

Rotacione imediatamente qualquer segredo exposto, remova-o do histórico Git com ferramenta apropriada e invalide tokens/senhas no provedor.

