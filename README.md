# VBank Sandbox

> Ambiente bancário fictício para demonstração, aprendizado e testes. Nenhum valor ou transferência realizada nesta plataforma possui valor financeiro real.

O VBank Sandbox é um simulador educacional, aberto e sob licença MIT. Ele demonstra como construir uma aplicação transacional com Java 21, Spring Boot, React, TypeScript e PostgreSQL sem integrar Pix real, bancos, Banco Central, Open Finance, cartões, boletos, criptomoedas, e-mail ou SMS.

Cada conta de demonstração recebe **R$ 50.000,00 fictícios**. Usuários criam chaves internas, localizam outra conta, transferem saldo sandbox, consultam ledger/extrato, recebem notificações e baixam comprovantes PDF. O backend é sempre a fonte da verdade.

## Avisos essenciais

- Não é uma instituição financeira.
- “Chave Pix simulada” e “chave interna” só funcionam nesta plataforma.
- Não use CPF, CNPJ, cartão, conta bancária real ou dado governamental.
- Não existe dinheiro real, saque, depósito, cobrança, pagamento ou promessa de rendimento.
- Não use este projeto como sistema financeiro de produção.

## Funcionalidades

- cadastro, login, logout, recuperação de sessão e logout global;
- JWT de acesso em memória e refresh token rotativo em cookie HttpOnly;
- senha e PIN com BCrypt, bloqueio após cinco tentativas de PIN;
- saldo inicial, recarga sandbox de 24 horas e ajustes administrativos;
- chaves internas `EMAIL`, `PHONE`, `USERNAME` e `RANDOM`;
- transferência atômica com `Idempotency-Key`, limite diário e locks pessimistas;
- ledger de débito/crédito para toda alteração de saldo;
- extrato, filtros, paginação, comprovante web, impressão e PDF;
- notificações internas e auditoria sem segredos;
- administração de usuários, bloqueios, ajustes, transferências e auditoria;
- Problem Details, Swagger em desenvolvimento e health check seguro;
- interface responsiva desde 360 px, teclado, foco visível e movimento reduzido.

## Tecnologias

| Camada | Tecnologias |
|---|---|
| Backend | Java 21, Spring Boot, Security, JPA, Flyway, JWT, PDFBox |
| Frontend | React 19, TypeScript, Vite, Router, Hook Form, Zod, Axios |
| Banco | PostgreSQL, recomendado Neon Free com conexão pooled e SSL |
| Testes | JUnit, Mockito, Testcontainers, Vitest, Testing Library |
| Entrega | Docker multi-stage, Vercel OCI, GitHub Actions |

## Custo esperado

| Serviço | Plano | Custo esperado | Finalidade |
|---|---|---:|---|
| GitHub | Free | R$ 0 | Repositório público |
| GitHub Actions | Público | R$ 0 | Testes e builds em runner padrão |
| Vercel | Hobby | R$ 0 | Aplicação pessoal/não comercial |
| Neon | Free | R$ 0 | PostgreSQL |
| Domínio | `.vercel.app` | R$ 0 | Endereço público |

Planos, cotas e regras mudam. Consulte as páginas oficiais antes do deploy. Não cadastre cartão, não ative add-ons, não use larger runners, não compre domínio e não habilite recursos pagos. Configure orçamento/limite em zero quando houver essa opção e prefira indisponibilidade ao atingir uma cota em vez de cobrança. Veja [custos e limites](docs/FREE-TIER.md).

O Hobby da Vercel é destinado a uso pessoal/não comercial. Este projeto é educacional; para uso comercial, revise os termos e escolha outra hospedagem/licença de serviço compatível.

## Arquitetura

```mermaid
flowchart LR
    B["Navegador"] --> V["Vercel Hobby<br/>imagem OCI"]
    V --> S["Spring Boot<br/>API REST + React compilado"]
    S --> N["PostgreSQL Neon Free<br/>pooled + SSL"]
```

Em produção, React e Spring são um único artefato. O `Dockerfile.vercel` compila o React, copia `dist` para `classpath:/static`, gera o JAR e executa Java 21 como usuário não-root. A Vercel define `$PORT`; `server.port=${PORT:8080}` aceita esse valor. O contêiner é stateless: apenas o PostgreSQL persiste dados.

Detalhes: [arquitetura](docs/ARCHITECTURE.md), [banco](docs/DATABASE.md) e [segurança](docs/SECURITY-ARCHITECTURE.md).

## Executar localmente sem Docker — caminho principal

Docker **não é obrigatório**. Você precisa somente de:

- Git;
- Java 21;
- Node.js 24 (ou Node 22.22+);
- npm;
- banco PostgreSQL gratuito no Neon.

### 1. Criar o banco Neon

Siga a seção [Criar PostgreSQL gratuito no Neon](#criar-postgresql-gratuito-no-neon). Guarde URL, usuário e senha fora do Git.

### 2. Configurar variáveis

Copie `.env.example` para `.env`. O `.env` está ignorado pelo Git. O Spring Boot **não lê `.env` sozinho**; os scripts `run-local` o carregam no processo sem imprimir valores.

Gere um segredo JWT local com 32 bytes ou mais:

```powershell
# PowerShell
$bytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

```bash
# Linux/macOS
openssl rand -base64 48
```

Preencha:

```dotenv
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:postgresql://SEU-ENDPOINT-pooler.REGIAO.aws.neon.tech/neondb?sslmode=require
SPRING_DATASOURCE_USERNAME=SEU_USUARIO
SPRING_DATASOURCE_PASSWORD=SUA_SENHA
JWT_SECRET=SEGREDO_ALEATORIO_COM_32_BYTES_OU_MAIS
COOKIE_SECURE=false
SWAGGER_ENABLED=true
```

### 3. Iniciar com o script

```powershell
.\scripts\run-local.ps1
```

```bash
chmod +x backend/mvnw scripts/*.sh
./scripts/run-local.sh
```

- Frontend: <http://localhost:5173>
- Backend: <http://localhost:8080>
- Health: <http://localhost:8080/api/health>
- Swagger: <http://localhost:8080/swagger-ui/index.html>

O Vite encaminha `/api` para `http://localhost:8080`.

### Iniciar manualmente

PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE='dev'
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://HOST-pooler/DB?sslmode=require'
$env:SPRING_DATASOURCE_USERNAME='USUARIO'
$env:SPRING_DATASOURCE_PASSWORD='SENHA'
$env:JWT_SECRET='SEGREDO_ALEATORIO_COM_32_BYTES_OU_MAIS'
cd backend
.\mvnw.cmd spring-boot:run
```

Em outro terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Linux/macOS:

```bash
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_URL='jdbc:postgresql://HOST-pooler/DB?sslmode=require'
export SPRING_DATASOURCE_USERNAME='USUARIO'
export SPRING_DATASOURCE_PASSWORD='SENHA'
export JWT_SECRET='SEGREDO_ALEATORIO_COM_32_BYTES_OU_MAIS'
cd backend && ./mvnw spring-boot:run
```

Em outro terminal: `cd frontend && npm ci && npm run dev`.

## Testes

Testes unitários não exigem Docker:

```powershell
cd backend
.\mvnw.cmd test
cd ..\frontend
npm ci
npm test
npm run build
```

Linux/macOS usa `./mvnw test`. Integração usa PostgreSQL real em Testcontainers e é separada:

```bash
cd backend
./mvnw -Pintegration-test verify
```

Esse último comando requer Docker e é executado pelo GitHub Actions. A falta de Docker local não impede testes unitários, desenvolvimento ou deploy.

Com backend e banco ativos, execute o smoke test completo:

```powershell
.\scripts\smoke-test.ps1
```

```bash
./scripts/smoke-test.sh
```

Ele valida health, dois cadastros, PIN, chave, resolução, transferência de R$ 1.000,00, saldos R$ 49.000,00/R$ 51.000,00, extratos, comprovante e PDF.

## Docker opcional

Somente para contribuidores que já tenham Docker:

```bash
docker compose up --build
```

A aplicação unificada fica em <http://localhost:8080>. O Compose usa PostgreSQL local e credenciais exclusivamente de desenvolvimento. Não use os valores padrão em ambiente público.

Validar a mesma imagem usada na Vercel:

```bash
docker build -f Dockerfile.vercel -t vbank-sandbox .
```

Você não precisa instalar Docker para publicar: GitHub Actions valida a imagem e a Vercel a constrói remotamente.

## Criar PostgreSQL gratuito no Neon

1. Acesse [Neon](https://console.neon.tech/) e crie uma conta.
2. Crie um projeto no plano **Free**; não adicione pagamento e não selecione plano pago.
3. Escolha uma região próxima da região de execução, quando disponível.
4. No projeto, abra **Connect**.
5. Ative **Pooled connection**. O host normalmente contém `-pooler`.
6. Mantenha `sslmode=require`.
7. Copie a connection string para um gerenciador de senhas; nunca a cole em issue, commit ou log.
8. Separe host/banco, usuário e senha. Exemplo apenas com placeholders:

```text
PostgreSQL: postgresql://USUARIO:SENHA@HOST-pooler/DB?sslmode=require
JDBC:       jdbc:postgresql://HOST-pooler/DB?sslmode=require
Usuário:    USUARIO
Senha:      SENHA
```

9. Use a JDBC em `SPRING_DATASOURCE_URL` e as outras partes nas variáveis separadas.
10. Flyway cria o schema automaticamente ao iniciar um banco vazio.

Alternativa na Vercel: depois de importar o repositório, instale **Neon** pelo Marketplace da Vercel. Confirme que o recurso criado é Free e copie/mapeie os valores fornecidos para as três variáveis `SPRING_DATASOURCE_*`. Não use `DATABASE_URL` diretamente como JDBC sem adaptar o prefixo.

Documentação oficial: [conexão pooled](https://neon.com/docs/connect/connection-pooling) e [preços atuais](https://neon.com/pricing).

## Publicar no GitHub

1. Crie um repositório **público** vazio no GitHub.
2. Não marque a criação automática de README, `.gitignore` ou licença; estes arquivos já existem.
3. Antes do commit, confirme que `.env` não aparece em `git status`.
4. Na raiz:

Antes de publicar, substitua `SEU_USUARIO` pelo seu usuário GitHub em `frontend/src/pages/LandingPage.tsx` e `.github/ISSUE_TEMPLATE/config.yml`.

```bash
git init
git add .
git status
git commit -m "feat: initial VBank Sandbox"
git branch -M main
git remote add origin ENDERECO_DO_REPOSITORIO
git push -u origin main
```

O workflow usa somente `ubuntu-latest`, permissões de leitura e runners padrão. Em repositório público, runners padrão são gratuitos segundo a [documentação do GitHub Actions](https://docs.github.com/en/billing/concepts/product-billing/github-actions). Larger runners continuam proibidos.

## Deploy gratuito na Vercel

1. Envie o projeto ao GitHub.
2. Entre na [Vercel](https://vercel.com/) e mantenha o plano **Hobby**.
3. Clique em **Add New → Project** e importe o repositório.
4. Use a raiz do repositório como **Root Directory**.
5. A Vercel detecta `Dockerfile.vercel`, constrói a imagem OCI e encaminha o tráfego ao contêiner.
6. Instale/crie o Neon Free antes do primeiro build operacional.
7. Em **Settings → Environment Variables**, cadastre as variáveis abaixo no ambiente Production. Não exponha valores no frontend.
8. Faça o deploy.
9. Aguarde Flyway aplicar migrations.
10. Acesse `https://SEU-PROJETO.vercel.app/api/health` e confira `UP`.
11. Acesse a landing page e crie duas contas de teste.
12. Se precisar de admin, habilite o bootstrap uma única vez; depois defina `ADMIN_BOOTSTRAP_ENABLED=false` e faça novo deploy.

A Vercel injeta `$PORT`; não fixe outra porta no painel. O Java mantém o processo em primeiro plano e encerra de forma graciosa ao receber `SIGTERM`.

Consulte [Container Images](https://vercel.com/docs/functions/container-images), a explicação oficial sobre [Docker deployments](https://vercel.com/kb/guide/does-vercel-support-docker-deployments) e o [guia detalhado de deploy](docs/DEPLOYMENT.md).

## Variáveis de ambiente

Obrigatórias em produção:

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST-pooler/DB?sslmode=require
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
JWT_SECRET=
JWT_ACCESS_EXPIRATION_MINUTES=15
JWT_REFRESH_EXPIRATION_DAYS=7
COOKIE_SECURE=true
COOKIE_DOMAIN=
DB_POOL_SIZE=3
DB_MINIMUM_IDLE=0
SWAGGER_ENABLED=false
APP_VERSION=1.0.0
ADMIN_BOOTSTRAP_ENABLED=false
```

Somente no primeiro bootstrap administrativo, se desejado:

```text
ADMIN_BOOTSTRAP_ENABLED=true
ADMIN_BOOTSTRAP_NAME=
ADMIN_BOOTSTRAP_EMAIL=
ADMIN_BOOTSTRAP_USERNAME=
ADMIN_BOOTSTRAP_PASSWORD=
ADMIN_BOOTSTRAP_PIN=
```

Não use credencial fixa. A aplicação falha com mensagem objetiva se JWT/banco estiverem ausentes, JWT for curto, cookie estiver inseguro em produção ou JDBC de produção não exigir SSL.

## Deploy automático

- Push em `main`: GitHub Actions testa; a integração Git/Vercel cria o deploy de produção.
- Pull request/branch: Vercel pode criar preview. Não dê a previews acesso ao banco de produção; use variáveis separadas ou desabilite previews com banco.
- Frontend e backend são sempre atualizados juntos.
- Flyway aplica migrations para frente. Faça alterações compatíveis e aditivas antes de remover colunas.
- Rollback de código pode usar um deployment anterior na Vercel, mas migration destrutiva exige restauração/ação manual.

## Limitações gratuitas

- Neon e Vercel podem entrar em scale-to-zero; a primeira requisição pode sofrer cold start.
- CPU, memória, duração, armazenamento, transferência e conexões têm cotas.
- O projeto usa Hikari pequeno (`3/0`) e paginação para reduzir consumo.
- Ao atingir a cota, aguarde renovação, reduza uso ou deixe a aplicação indisponível; não ative cobrança.
- O projeto não foi projetado para alta escala, SLA, dinheiro real ou uso comercial no Hobby.
- Limites e preços mudam; consulte [FREE-TIER.md](docs/FREE-TIER.md).

## Solução de problemas

| Sintoma | Verificação |
|---|---|
| Docker não está instalado | Use o fluxo principal sem Docker; Vercel/Actions constroem remotamente. |
| `java -version` não mostra 21 | Instale/selecione JDK 21 e reabra o terminal. |
| Node/npm incorreto | Use Node 24 e execute `npm ci` novamente. |
| Porta 8080/5173 ocupada | Encerre o processo anterior; não altere o proxy sem ajustar ambos. |
| Falha JDBC | Use prefixo `jdbc:postgresql://`, host pooled, banco correto e `sslmode=require`. |
| `JWT_SECRET` ausente/curto | Gere pelo menos 32 bytes aleatórios. |
| Flyway falha | Não edite migration já aplicada; crie uma nova versão. Confira permissões do usuário. |
| 404 ao atualizar rota React | Confirme que `frontend/dist` foi copiado ao JAR; no Docker isso é automático. |
| Cookie não volta | Local usa `COOKIE_SECURE=false`; produção exige `true` e mesma origem. |
| Health `DOWN` | Neon pode estar retomando; aguarde e tente novamente. Nenhuma transferência é presumida como concluída. |
| `$PORT` | Não configure manualmente na Vercel; a plataforma injeta. |
| GitHub Actions falha em integração | Veja logs do Testcontainers e confirme uso de `ubuntu-latest`. |
| Build Docker remoto falha | Confira lockfile, caminhos na raiz e logs de build da Vercel. |
| Cota gratuita atingida | Verifique consumo nos painéis e aguarde renovação; não habilite pagamento. |

## Estrutura

```text
backend/       Spring Boot, Flyway, Maven Wrapper, testes
frontend/      React/Vite, CSS, Vitest, package-lock
docs/          arquitetura, banco, deploy, segurança e custo
scripts/       execução local e smoke test
.github/       templates e CI
Dockerfile*    imagens unificadas local/Vercel
```

## Contribuição e segurança

Leia [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md) e [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md). Não abra issue pública com vulnerabilidade explorável ou qualquer credencial.

## Licença

[MIT](LICENSE). Bibliotecas continuam sob suas respectivas licenças abertas.
