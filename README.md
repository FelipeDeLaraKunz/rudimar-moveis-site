# Rudimar Móveis — site + painel administrativo

Site para a Rudimar Móveis, feito em **Spring Boot + Thymeleaf + PostgreSQL**.

- **Site público** (`/`): informações da loja, mapa, redes sociais, uma vitrine pequena de
  produtos em destaque e as promoções ativas. Ninguém precisa fazer login para ver.
- **Catálogo completo** (`/produtos`): lista todos os produtos ativos, agrupados por
  categoria — para quando a vitrine da home não for suficiente.
- **Página do produto** (`/produtos/{id}`): fotos, descrição, preço e um botão que abre o
  WhatsApp direto com uma mensagem pronta (em vez de "comprar"/"adicionar ao carrinho").
- **Painel admin** (`/admin`): protegido por usuário e senha. É basicamente um
  **gerenciador de estoque** — cadastro de produtos com quantidade em estoque, cor,
  categoria, fotos (upload direto, não link) e se aparece ou não na home — além do
  cadastro de promoções, que podem ter foto própria e vários produtos vinculados.

## Estrutura do projeto

```
src/main/java/com/rudimarmoveis/site/
├── SiteApplication.java              # ponto de entrada
├── config/
│   ├── SecurityConfig.java           # login do admin (Spring Security)
│   ├── WebConfig.java                # expõe a pasta de uploads como /uploads/**
│   └── GlobalModelAttributes.java    # whatsapp, instagram, facebook, link do Maps
├── model/                            # Produto e Promocao (entidades JPA)
├── repository/                       # acesso ao banco (Spring Data JPA)
├── service/ArmazenamentoImagensService.java  # salva as fotos enviadas no admin
└── controller/
    ├── HomeController.java           # páginas públicas (home, catálogo, produto)
    └── AdminController.java          # estoque e promoções

src/main/resources/
├── application.properties
├── db/migration/                     # scripts do banco (Flyway, nunca editar um já aplicado)
├── templates/                        # HTML (Thymeleaf)
└── static/css/style.css
```

## Rodando localmente (com Docker — mais fácil)

Se você tem Docker instalado, isso sobe o site inteiro (app + banco) com um comando:

```bash
docker compose up --build
```

Acesse:
- Site: http://localhost:8080
- Admin: http://localhost:8080/admin/login (usuário: `admin`, senha: `troque-esta-senha`,
  definidos no `docker-compose.yml` — troque antes de usar de verdade)

## Rodando localmente (sem Docker)

1. Suba um Postgres local (ou use um já existente) e crie o banco:
   ```sql
   CREATE DATABASE loja_moveis;
   ```
2. Ajuste as credenciais no `application.properties` ou exporte as variáveis de ambiente:
   ```bash
   export DATABASE_URL=jdbc:postgresql://localhost:5432/loja_moveis
   export DATABASE_USERNAME=postgres
   export DATABASE_PASSWORD=sua_senha
   export ADMIN_USERNAME=admin
   export ADMIN_PASSWORD=uma_senha_forte
   ```
3. Rode a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```
   (ou pela sua IDE, rodando a classe `SiteApplication`)

O Flyway cria as tabelas automaticamente na primeira execução, já com alguns produtos e
uma promoção de exemplo — é só editar/excluir pelo painel admin depois.

⚠️ **Nunca edite um arquivo de migration (`V1__...`) que já rodou em algum banco.** Se quiser
mudar o schema depois, crie um novo arquivo `V2__algo.sql`, `V3__algo.sql`, etc. Editar um já
aplicado causa o erro "Migration checksum mismatch" nesse banco.

## Dados da loja (WhatsApp, endereço, redes sociais)

Ficam centralizados no `application.properties` (seção "Dados de contato da loja"), e
podem ser sobrescritos por variável de ambiente sem mexer no código:

| Propriedade | Variável de ambiente | O que é |
|---|---|---|
| `loja.whatsapp` | `LOJA_WHATSAPP` | número no formato internacional, só dígitos (ex: `5549984372223`) |
| `loja.telefone-exibicao` | `LOJA_TELEFONE_EXIBICAO` | telefone formatado, só para exibir |
| `loja.endereco` | `LOJA_ENDERECO` | usado para montar o link do Google Maps |
| `loja.instagram` | `LOJA_INSTAGRAM` | link completo do Instagram |
| `loja.facebook` | `LOJA_FACEBOOK` | link completo do Facebook |

**⚠️ Troque os links de Instagram e Facebook** — coloquei `instagram.com/rudimarmoveis` e
`facebook.com/rudimarmoveis` como exemplo, mas não sei se são as contas reais da loja.

O botão "Como chegar" e o endereço no rodapé abrem o Google Maps direto com o endereço
configurado — **não precisa de chave de API do Google**, é um link de busca comum
(`google.com/maps/search`), então funciona de graça e sem nenhuma configuração extra.

## Painel admin: estoque de produtos

Cada produto tem:
- **Categoria** e **cor**: escolhidas em uma lista pronta (não é mais texto livre) — para
  adicionar mais opções, edite as `<option>` em `templates/admin/produtos.html`.
- **Unidades em estoque**: só aparece para o admin, nunca é mostrado no site público.
- **Mostrar na página inicial**: controla se o produto entra na vitrine pequena da home,
  separado de "visível no site" (que controla se ele aparece em geral, inclusive no
  catálogo completo).
- **Fotos**: upload direto de arquivo (não é mais link) — pode enviar várias de uma vez,
  e dá pra adicionar mais fotos ou remover fotos antigas depois, editando o produto.

## Painel admin: promoções

- Pode enviar uma foto para a promoção (upload, mesma lógica dos produtos).
- Pode selecionar **vários produtos** participantes da promoção, escolhendo entre os que
  estão ativos e com estoque disponível (segure Ctrl/Cmd para marcar mais de um).

## Onde ficam as fotos enviadas (importante para o deploy!)

As imagens enviadas pelo admin são salvas em uma pasta `uploads/` no servidor (fora do
`.jar`), e servidas em `/uploads/arquivo.jpg`. Isso tem uma implicação importante:

- **Com Docker**: o `docker-compose.yml` já cria um volume (`uploads_data`) para essa
  pasta, então as fotos sobrevivem a reinícios e rebuilds do container.
- **Em Railway/Render ou qualquer host "sem estado"**: se você não configurar um **disco
  persistente** apontando para a pasta de uploads, as fotos enviadas pelo admin **somem a
  cada novo deploy**. Procure por "Persistent Disk" ou "Volumes" nas configurações do
  serviço e aponte para o caminho da variável `APP_UPLOAD_DIR` (padrão: `uploads`).

## Como funciona o controle de acesso

- Todo mundo pode ver `/`, `/produtos` e `/produtos/{id}` (as páginas públicas).
- Só quem estiver logado como admin acessa `/admin/**`.
- Não existe cadastro de usuário comum — o único usuário do sistema é o administrador,
  configurado via variáveis de ambiente (`ADMIN_USERNAME` / `ADMIN_PASSWORD`).

**Antes de colocar no ar, troque a senha padrão!** Nunca deixe `troque-esta-senha` em produção.

## Colocando o site no ar (deploy)

Existem várias formas, do mais simples ao mais robusto.

### Opção 1 — Railway ou Render (recomendado para começar)

Ambos suportam subir uma aplicação a partir do `Dockerfile` deste projeto, com um banco
Postgres gerenciado junto. O passo a passo (resumido) é:

1. Crie uma conta em [railway.app](https://railway.app) ou [render.com](https://render.com).
2. Suba este projeto para um repositório no GitHub.
3. No painel do Railway/Render, crie um novo serviço "a partir de um repositório Git" e
   aponte para o seu repositório (ele detecta o `Dockerfile` automaticamente).
4. Adicione um banco Postgres pelo próprio painel (um clique) — a plataforma gera uma
   `DATABASE_URL` automaticamente.
5. Configure as variáveis de ambiente do serviço (veja as tabelas acima e o
   `application.properties` para a lista completa).
6. Configure um **disco persistente** para a pasta de uploads (ver seção acima).
7. Faça o deploy. A plataforma te dá uma URL pública tipo `https://seu-projeto.up.railway.app`.
8. (Opcional) Configure um domínio próprio (ex: `www.rudimarmoveis.com.br`) nas configurações
   de domínio do serviço.

### Opção 2 — VPS própria (DigitalOcean, Hetzner, etc.)

Mais controle, um pouco mais de trabalho manual:

1. Crie um servidor (droplet/instância) com Ubuntu.
2. Instale Docker e Docker Compose no servidor.
3. Copie este projeto para o servidor (`git clone` ou `scp`).
4. Rode `docker compose up -d --build` (o mesmo `docker-compose.yml` deste projeto, que já
   persiste banco de dados e uploads em volumes).
5. Configure um domínio apontando para o IP do servidor.
6. Coloque um Nginx (ou Caddy, que já cuida do HTTPS sozinho) na frente da aplicação como
   proxy reverso, e gere um certificado SSL gratuito (Let's Encrypt / Certbot, ou automático
   no Caddy).

### Opção 3 — AWS (mais robusta, mais complexa)

Para quando o site crescer bastante: Elastic Beanstalk ou ECS para rodar o container, RDS
para o Postgres gerenciado, e S3 para as fotos (em vez da pasta local de uploads). Não é
necessário para começar — vale considerar mais pra frente se o negócio crescer.

## Próximos passos possíveis

- Filtro por categoria/cor no catálogo completo.
- Login soável (recuperação de senha) para o admin.
- Enviar as fotos para um serviço externo (ex: S3/Cloudinary) em vez da pasta local —
  facilita hospedar em plataformas sem disco persistente.
