# Rudimar Móveis

Projeto de um site para uma loja de móveis (a Rudimar Móveis), com um painel administrativo
simples pra controlar o estoque. Fiz em Java com Spring Boot, Thymeleaf pro HTML e banco
Postgres.

A ideia não é ter carrinho de compras nem pagamento online, é mais uma vitrine: o cliente vê
os produtos e manda mensagem no WhatsApp pra combinar a compra direto com a loja.

## O que tem no site

Página inicial (`/`) - dados da loja, mapa, redes sociais, alguns produtos em destaque e as
promoções do momento. Não precisa estar logado pra ver nada disso.

Catálogo (`/produtos`) - todos os produtos, separados por categoria. É pra quando a home não
é suficiente e a pessoa quer ver tudo.

Página de cada produto (`/produtos/{id}`) - fotos, descrição, preço, e um botão que já abre
o WhatsApp com uma mensagem pronta perguntando sobre o produto.

Painel admin (`/admin`) - só o dono da loja acessa (login e senha). É onde cadastra os
produtos (quantidade em estoque, cor, categoria, fotos), decide o que aparece na home, e
cadastra as promoções.

## Como o projeto tá organizado

```
src/main/java/com/rudimarmoveis/site/
├── SiteApplication.java          -> classe principal, é o que roda
├── config/
│   ├── SecurityConfig.java       -> configuração do login do admin
│   ├── WebConfig.java            -> deixa a pasta de uploads acessível pelo navegador
│   └── GlobalModelAttributes.java -> whatsapp, instagram, facebook, link do maps
├── model/                        -> as entidades (Produto, Promocao)
├── repository/                   -> interfaces do Spring Data pra mexer no banco
├── service/ArmazenamentoImagensService.java -> salva as fotos que o admin envia
└── controller/
    ├── HomeController.java       -> as páginas públicas
    └── AdminController.java      -> tudo que é do painel admin

src/main/resources/
├── application.properties
├── db/migration/    -> scripts sql do Flyway (não mexer em um que já rodou!)
├── templates/        -> os htmls (Thymeleaf)
└── static/css/style.css
```

## Rodando o projeto na sua máquina

### Com Docker (mais fácil, recomendo)

Se tiver Docker instalado é só rodar:

```bash
docker compose up --build
```

Isso já sobe o site junto com o banco de dados. Depois é só acessar:

- Site: http://localhost:8080
- Admin: http://localhost:8080/admin/login

Usuário e senha do admin ficam configurados no `docker-compose.yml` (usuário `admin`,
senha `troque-esta-senha`). Óbvio que essa senha é só pra testar, tem que trocar antes de
usar de verdade.

### Sem Docker

1. Precisa ter um Postgres rodando e criar o banco:

```sql
CREATE DATABASE loja_moveis;
```

2. Configurar as credenciais, ou no `application.properties` ou criando as variáveis de
   ambiente:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/loja_moveis
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=sua_senha
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=uma_senha_forte
```

3. E rodar:

```bash
./mvnw spring-boot:run
```

(ou então dá pra rodar direto pela IDE, na classe `SiteApplication`)

Na primeira vez que roda, o Flyway já cria as tabelas e coloca uns produtos e uma
promoção de exemplo, só pra não ficar tudo vazio. Depois é só apagar/editar pelo admin.

Ah, e um aviso: nunca edite um arquivo de migration que já foi executado (tipo o
`V1__...`). Se precisar mudar alguma coisa no banco depois, cria um arquivo novo
(`V2__alguma_coisa.sql`) em vez de editar o antigo. Se editar um que já rodou, dá erro de
checksum e trava tudo.

## Configurações da loja (whatsapp, endereço, redes sociais)

Tudo isso fica no `application.properties`, na parte "Dados de contato da loja". Dá pra
trocar sem mexer no código, só sobrescrevendo a variável de ambiente correspondente:

- `loja.whatsapp` (ou `LOJA_WHATSAPP`) - número em formato internacional, só números, tipo
  5549984372223
- `loja.telefone-exibicao` (ou `LOJA_TELEFONE_EXIBICAO`) - telefone formatado bonitinho, só
  pra mostrar na tela
- `loja.endereco` (ou `LOJA_ENDERECO`) - usado pra montar o link do Google Maps
- `loja.instagram` (ou `LOJA_INSTAGRAM`) - link do Instagram
- `loja.facebook` (ou `LOJA_FACEBOOK`) - link do Facebook

Coloquei instagram.com/rudimarmoveis e facebook.com/rudimarmoveis como exemplo mas não sei
se são as contas reais da loja, então trocar antes de publicar.

O botão "Como chegar" e o endereço no rodapé abrem o Google Maps já com o endereço
configurado. Não precisa de chave de API nem nada, é só um link de busca normal do maps
mesmo (google.com/maps/search), então funciona de graça.

## Painel admin - estoque

Cada produto cadastrado tem:

- Categoria e cor, escolhidas numa lista já pronta (não é mais texto livre). Se quiser
  adicionar mais opções de categoria/cor é só editar as tags `<option>` no arquivo
  `templates/admin/produtos.html`
- Quantidade em estoque - isso só o admin vê, nunca aparece pro cliente no site
- Um checkbox pra "mostrar na página inicial" (diferente do "visível no site", que
  controla se o produto aparece em geral, inclusive no catálogo)
- Fotos - dá pra fazer upload de arquivo direto (não é mais link de imagem), pode mandar
  várias de uma vez, e depois ainda dá pra adicionar mais ou remover alguma editando o
  produto

## Painel admin - promoções

A promoção também pode ter uma foto (upload, mesmo esquema dos produtos) e pode escolher
vários produtos participantes ao mesmo tempo, entre os que estão ativos e com estoque
(segurando Ctrl ou Cmd pra marcar mais de um).

## Sobre as fotos que o admin envia (cuidado nisso ao publicar)

As fotos que o admin sobe ficam salvas numa pasta `uploads/` no servidor (fora do .jar) e
são servidas em `/uploads/nome-do-arquivo.jpg`. Isso importa bastante na hora de colocar
no ar:

- Com Docker, o `docker-compose.yml` já cria um volume (`uploads_data`) pra essa pasta,
  então as fotos não somem quando reinicia ou builda o container de novo
- Só que se for usar Railway, Render ou qualquer serviço que não guarda estado, se não
  configurar um disco persistente apontando pra pasta de uploads, toda vez que fizer um
  novo deploy as fotos cadastradas somem. Nas configurações do serviço, procurar por algo
  tipo "Persistent Disk" ou "Volumes" e apontar pro caminho da variável `APP_UPLOAD_DIR`
  (o padrão dela é `uploads`)

## Login e permissões

- `/`, `/produtos` e `/produtos/{id}` são públicas, qualquer um vê
- `/admin/**` só quem estiver logado
- não tem cadastro de usuário comum, o único login que existe é o do administrador mesmo,
  configurado pelas variáveis `ADMIN_USERNAME` e `ADMIN_PASSWORD`

Antes de colocar em produção não esquece de trocar a senha padrão. Deixar
`troque-esta-senha` valendo seria phoda.

## Deploy

Não testei todas as opções ainda, mas fica o resumo de como pretendo (ou como daria pra)
publicar isso:

### Railway ou Render (mais simples)

Os dois conseguem subir a aplicação direto a partir do Dockerfile do projeto, com um banco
Postgres gerenciado junto:

1. Criar conta no railway.app ou no render.com
2. Subir o projeto pra um repositório no GitHub
3. Criar um novo serviço "a partir de repositório Git" apontando pro repo (ele já detecta
   o Dockerfile sozinho)
4. Adicionar um Postgres pelo próprio painel, que gera a `DATABASE_URL` automaticamente
5. Preencher as variáveis de ambiente do serviço (tudo que tá listado lá em cima e no
   application.properties)
6. Configurar o disco persistente pra pasta de uploads (ver a seção de cima, isso é
   importante)
7. Deployar. A plataforma dá uma URL tipo `https://seu-projeto.up.railway.app`
8. Se quiser, dá pra configurar um domínio próprio depois

### VPS (Digital Ocean, Hetzner, etc)

Dá mais trabalho mas tem mais controle:

1. Sobe um servidor Ubuntu
2. Instala Docker e Docker Compose nele
3. Copia o projeto pro servidor (git clone ou scp)
4. Roda `docker compose up -d --build` (o mesmo docker-compose.yml do projeto já persiste
   banco e uploads em volume)
5. Aponta um domínio pro IP do servidor
6. Coloca um Nginx ou Caddy na frente como proxy reverso, com certificado SSL (o Caddy já
   resolve isso sozinho)

### AWS

Isso aí só se o negócio crescer bastante mesmo, não é necessário agora. Seria tipo Elastic
Beanstalk ou ECS pra rodar o container, RDS pro banco, S3 pras fotos em vez da pasta local.
