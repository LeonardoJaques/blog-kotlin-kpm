# Convergencia Blog

Blog multiplataforma em Kotlin, publicado em <https://jaquesprojetos.com.br/ljblog>. A interface usa Kobweb e Compose HTML; a API usa Ktor, PostgreSQL e Valkey. O projeto também contém contratos Kotlin compartilhados e um shell Android.

## Desenvolvimento

Requisitos: JDK 27, Docker Compose e Gradle Wrapper. O Kotlin e os plugins do projeto estão configurados para essa toolchain.

1. Copie `.env.example` para `.env` e defina uma senha aleatória em `DATABASE_PASSWORD`.
2. Inicie os serviços locais com `docker compose up --build`.
3. Verifique `http://localhost:8081/api/v1/health/live` e `/api/v1/health/ready`.
4. Inicie o site na pasta `site` usando `kobweb run` (porta 3000).

O Mailpit de desenvolvimento fica em `http://localhost:8025`. Não use as credenciais de exemplo do Compose em produção. PostgreSQL e Valkey não publicam portas no host por padrão.

## Módulos

- `site`: frontend Kobweb existente.
- `backend`: serviço Ktor com coroutines, pool HikariCP, migrações Flyway e rotas versionadas.
- `shared-contracts`: DTOs serializáveis compartilhados entre Kotlin/JVM e Kotlin/JS.
- `androidapp`: shell Android existente; continua sem telas de produto.
- `worker`: worker Kobweb existente.

## Identidade visual

A interface pública combina a identidade técnica do site Jaques Projetos com uma direção editorial de desenho antigo: papel texturizado, tipografia serifada, molduras de tinta e fotografias em sépia. O logotipo e o favicon Convergencia seguem a mesma linguagem de gravura.

O schema PostgreSQL inclui posts, contas e tokens de leitores, sessões, favoritos, comentários moderados, sessões/transações OIDC, chaves idempotentes, tarefas Tars e outbox.

## Fluxos implementados

- Leitores: cadastro, verificação de e-mail, login/logout, recuperação de senha, favoritos e envio de comentários para moderação.
- Administração: autenticação OIDC com Authelia, sessão administrativa, criação/edição/exclusão e publicação explícita de posts, moderação de comentários e revisão do Tars. As rotas administrativas exigem sessão e proteção CSRF.
- Tars: a API grava tarefas e eventos outbox de forma transacional; o conector usa leases, retentativas e circuit breaker. O resultado da revisão só vira um novo rascunho após ação explícita do administrador.
- Rate limits de autenticação por IP/e-mail e comentários por conta; cache público de posts com expiração e invalidação após edição/publicação/exclusão.

O site inclui a tela `/account` para cadastro, login, confirmação e recuperação de conta, e `/admin/comments` para moderação. Favoritos e comentários são acessíveis pela API; as interfaces públicas específicas ainda estão incompletas.

## Conector Tars

O conector é opcional e só deve ser iniciado quando a URL de revisão estiver configurada. Defina `TARS_REVIEW_URL` e `TARS_CONNECTOR_TOKEN` no `.env` e inicie com `docker compose --profile tars up --build`. O serviço de revisão configurado em `TARS_REVIEW_URL` deve aceitar o contrato `TarsReviewRequest` e responder com `TarsReviewResponse`. Use HTTPS fora de localhost.

## Produção

- O frontend Kotlin e a API estão publicados em <https://jaquesprojetos.com.br/ljblog> e rodam em Java 27.
- O Compose de produção está em `deploy/kotlin-blog/compose.yml`; os dados persistem em volumes separados de PostgreSQL e Valkey.
- Os dois posts públicos do Ghost foram importados para o PostgreSQL do blog Kotlin. O Ghost continua disponível em `/convergencia` para retorno.
- O login administrativo OIDC usa Authelia; o grupo autorizado é `blog-admin`. O acesso começa em `/ljblog/admin/login`.
- As imagens dos posts públicos continuam servidas pelo Ghost legado em `/convergencia/content/images`.

## Segurança e configuração

- O login antigo por usuário e senha foi removido. O painel não guarda identidade nem credenciais no `localStorage`.
- O botão administrativo encaminha para o login OIDC do Ktor. Configure `OIDC_ISSUER`, `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET`, `OIDC_REDIRECT_URI` e `OIDC_ADMIN_GROUP` no arquivo de ambiente do servidor; não versione segredos.
- Senhas de leitores usam Argon2id; tokens são armazenados como hashes e consumidos uma vez. O acesso a favoritos e comentários exige sessão de leitor verificado e proteção CSRF.
- Cookies de sessão usam `HttpOnly` e `SameSite=Strict`. Configure `COOKIE_SECURE=true` atrás de HTTPS em produção.
- SMTP é configurável por `SMTP_HOST`, `SMTP_PORT`, `SMTP_FROM`, `SMTP_USER`, `SMTP_PASSWORD` e `SMTP_STARTTLS`. No Compose local, Mailpit captura as mensagens em `http://localhost:8025`.
- Redis/Valkey aplica limites de autenticação e comentários e guarda leituras públicas com cache expirado disponível por até dez minutos durante falhas do PostgreSQL. Se Valkey estiver indisponível, o rate limiter falha aberto e as leituras consultam o PostgreSQL.
- Configure segredos somente por variáveis de ambiente ou secret manager. `local.properties` é local e ignorado pelo Git.
- A API aplica cabeçalhos defensivos e permite CORS apenas para as origens de `WEB_ORIGINS`. Use HTTPS e cookies `Secure` no proxy/ambiente de produção.

## Próximas melhorias

- Completar as telas públicas de favoritos e comentários e revisar os formulários de conta.
- Configurar SMTP no ambiente de produção para verificação e recuperação de conta.
- O Android permanece como cliente futuro; o shell atual ainda não oferece telas de produto.
