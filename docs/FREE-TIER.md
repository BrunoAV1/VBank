# Custos e limites gratuitos

Última verificação: **5 de agosto de 2026**.

Planos e regras podem mudar sem aviso. Esta página registra a arquitetura pretendida, não garante gratuidade eterna. Antes de publicar, abra os links oficiais e confirme as condições atuais.

## Serviços usados

| Serviço | Plano/recurso | Uso no projeto | Comportamento esperado ao atingir cota |
|---|---|---|---|
| GitHub | Free, repositório público | Código e issues | Use apenas recursos incluídos. |
| GitHub Actions | runner padrão `ubuntu-latest` | Testes, build e Docker build | Interrompa/reduza workflows; não use larger runner. |
| Vercel | Hobby, domínio `.vercel.app`, Function OCI | Hospeda aplicação unificada | Pode limitar/suspender/escala a zero; aguarde ou reduza uso. |
| Neon | Free, PostgreSQL pooled | Estado persistente | Pode suspender compute ao atingir limite; aguarde renovação ou reduza uso. |

## GitHub

A documentação oficial informa que runners padrão hospedados pelo GitHub são gratuitos em repositórios públicos. Larger runners são cobrados e não são usados. O workflow não publica pacote, não envia artefato e cancela execução antiga da mesma branch.

- [GitHub Actions billing](https://docs.github.com/en/billing/concepts/product-billing/github-actions)
- [Runners padrão para repositórios públicos](https://docs.github.com/en/actions/how-tos/write-workflows/choose-where-workflows-run/choose-the-runner-for-a-job)
- [Orçamentos e alertas](https://docs.github.com/en/billing/concepts/budgets-and-alerts)

Defina orçamento com interrupção ao atingir zero quando a interface permitir. Não cadastre pagamento. Não habilite Codespaces, Packages, larger runners ou add-ons para este fluxo.

## Vercel

O Hobby é gratuito a critério da Vercel, destinado a uso pessoal/não comercial e sujeito a alteração/encerramento. Container images herdam limites e modelo de Vercel Functions. `Dockerfile.vercel` é detectado na raiz; a imagem é stateless e escala automaticamente.

Limites específicos de CPU, memória, duração, invocação, transferência e build podem mudar: **consulte a documentação oficial para o limite atual**.

- [Preços e planos](https://vercel.com/pricing)
- [Termos do Hobby](https://vercel.com/legal/terms)
- [Container Images](https://vercel.com/docs/functions/container-images)
- [Docker deployments](https://vercel.com/kb/guide/does-vercel-support-docker-deployments)
- [Uso e preços de Functions](https://vercel.com/docs/functions/usage-and-pricing)

Não use Pro, domínio comprado, Vercel Blob, observabilidade paga, Static IP, Secure Compute ou qualquer add-on. Use apenas o subdomínio `.vercel.app`. Revise consumo no dashboard e prefira o sistema temporariamente indisponível a aceitar cobrança.

## Neon

Na verificação acima, a página oficial apresentava Free a custo zero, sem limite de tempo e sem cartão obrigatório, com compute que pode escalar a zero. O plano suporta conexão pooled. Como números mudam, a fonte de verdade é a página de preços.

A documentação também informa que exceder determinadas cotas do Free pode suspender compute até o próximo ciclo ou upgrade. O projeto aceita suspensão e nunca faz upgrade automático.

- [Preços atuais do Neon](https://neon.com/pricing)
- [Conexão pooled](https://neon.com/docs/connect/connection-pooling)
- [Transferência de rede e suspensão](https://neon.com/docs/introduction/network-transfer)
- [Gerenciar compute/scale-to-zero](https://neon.com/docs/manage/endpoints)

Use um projeto, endpoint pooled, Hikari pequeno e paginação. Não ative read replica, integração paga ou plano Launch/Scale. Monitore **Billing/Usage** no console.

## Recursos proibidos

- cartão ou método de pagamento;
- Vercel Pro e recursos pagos;
- Neon Launch/Scale;
- larger runners, Packages ou imagens publicadas pelo GitHub;
- domínio personalizado pago;
- e-mail, SMS, Redis, uploads, analytics ou observabilidade externos;
- contorno de limites, múltiplas contas para burlar cota ou automação abusiva.

## Estratégia de custo zero

1. Não cadastrar pagamento.
2. Selecionar explicitamente Free/Hobby.
3. Configurar orçamento/alerta e interrupção em zero.
4. Revisar consumo depois de cada mudança.
5. Manter banco/conexões pequenos e resultados paginados.
6. Aceitar cold start e suspensão.
7. Aguardar renovação da cota ou reduzir uso; nunca ativar cobrança para manter disponibilidade.

## Nota sobre o Marketplace

Provisionar Neon pelo Marketplace da Vercel pode simplificar variáveis. Confirme na tela final que o plano é gratuito e que nenhuma cobrança/add-on será ativado. Também é válido criar a conta diretamente no Neon e cadastrar as variáveis manualmente.
