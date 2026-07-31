CREATE TABLE produtos (
    id BIGSERIAL PRIMARY KEY,
    nome_interno VARCHAR(255) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    descricao VARCHAR(1000),
    preco NUMERIC(10, 2) NOT NULL,
    preco_cartao NUMERIC(10, 2),
    categoria VARCHAR(255),
    marca VARCHAR(100),
    cor VARCHAR(50),
    quantidade_estoque INTEGER NOT NULL DEFAULT 0,
    destaque BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Varias fotos por produto, em ordem (posicao 0 = foto principal)
CREATE TABLE produto_imagens (
    produto_id BIGINT NOT NULL REFERENCES produtos(id) ON DELETE CASCADE,
    posicao INTEGER NOT NULL,
    imagem_url VARCHAR(500) NOT NULL,
    PRIMARY KEY (produto_id, posicao)
);

CREATE TABLE promocoes (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(1000),
    percentual_desconto INTEGER,
    imagem_url VARCHAR(500),
    valida_ate DATE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Produtos participantes de cada promocao, com o preco promocional definido por produto
-- (o desconto percentual da promocao e so exibicao/selo; o preco valido e sempre este daqui)
CREATE TABLE promocao_produtos (
    promocao_id BIGINT NOT NULL REFERENCES promocoes(id) ON DELETE CASCADE,
    produto_id BIGINT NOT NULL REFERENCES produtos(id) ON DELETE CASCADE,
    preco_promocional NUMERIC(10, 2) NOT NULL,
    PRIMARY KEY (promocao_id, produto_id)
);


