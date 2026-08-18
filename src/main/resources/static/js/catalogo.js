document.addEventListener('DOMContentLoaded', function () {
  var grid = document.getElementById('catalogoGrid');
  if (!grid) {
    return;
  }

  var cards = Array.prototype.slice.call(grid.querySelectorAll('.produto-card'));
  var campoBusca = document.getElementById('catalogoSearch');
  var precoMin = document.getElementById('precoMin');
  var precoMax = document.getElementById('precoMax');
  var semResultado = document.getElementById('catalogoNoResults');
  var contagem = document.getElementById('catalogoContagem');
  var botaoLimpar = document.getElementById('limparFiltros');
  var botaoExibirMais = document.getElementById('catalogoExibirMais');

  // com filtro nenhum aplicado, mostra so um lote de cards por vez (o resto ja esta no
  // HTML, so escondido) - poupa o carregamento das fotos que estao fora da tela, que e
  // o que mais pesa numa pagina de catalogo. Assim que algum filtro entra em jogo, mostra
  // todos os produtos que combinam, sem paginacao.
  var LOTE = 24;
  var revelados = LOTE;

  var checkboxCategoria = Array.prototype.slice.call(document.querySelectorAll('input[name="filtroCategoria"]'));
  var checkboxTamanho = Array.prototype.slice.call(document.querySelectorAll('input[name="filtroTamanho"]'));
  var checkboxMarca = Array.prototype.slice.call(document.querySelectorAll('input[name="filtroMarca"]'));
  var checkboxCor = Array.prototype.slice.call(document.querySelectorAll('input[name="filtroCor"]'));
  var todosCheckbox = checkboxCategoria.concat(checkboxTamanho, checkboxMarca, checkboxCor);
  var grupoTamanho = document.getElementById('filtroTamanhoGrupo');

  // so categorias que costumam variar por tamanho mostram o filtro de Tamanho - mantido em
  // sincronia manualmente com CategoriasCatalogo.CATEGORIAS_COM_TAMANHO (Java) e a mesma
  // lista em admin-produtos.js
  var CATEGORIAS_COM_TAMANHO = ['Cama', 'Conjunto box', 'Colchão'];

  function valoresMarcados(inputs) {
    return inputs.filter(function (input) {
      return input.checked;
    }).map(function (input) {
      return input.value;
    });
  }

  // mostra o filtro de Tamanho so quando alguma categoria marcada costuma variar por
  // tamanho; ao esconder, desmarca as opcoes de tamanho pra nao deixar um filtro invisivel ativo
  function atualizarFiltroTamanho(categorias) {
    if (!grupoTamanho) return false;

    var mostrar = categorias.some(function (c) {
      return CATEGORIAS_COM_TAMANHO.indexOf(c) !== -1;
    });
    grupoTamanho.hidden = !mostrar;
    if (!mostrar) {
      checkboxTamanho.forEach(function (input) {
        input.checked = false;
      });
    }
    return mostrar;
  }

  function aplicarFiltros() {
    var termo = (campoBusca && campoBusca.value) || '';
    var categorias = valoresMarcados(checkboxCategoria);
    atualizarFiltroTamanho(categorias);
    var tamanhos = valoresMarcados(checkboxTamanho);
    var marcas = valoresMarcados(checkboxMarca);
    var cores = valoresMarcados(checkboxCor);
    var min = precoMin && precoMin.value !== '' ? parseFloat(precoMin.value) : null;
    var max = precoMax && precoMax.value !== '' ? parseFloat(precoMax.value) : null;
    var filtroAtivo = !!termo || categorias.length > 0 || tamanhos.length > 0 || marcas.length > 0 || cores.length > 0 || min !== null || max !== null;
    var correspondentes = 0;

    cards.forEach(function (card) {
      var preco = parseFloat(card.dataset.preco);
      var combina =
        correspondeABusca(card.dataset.busca, termo) &&
        (categorias.length === 0 || categorias.indexOf(card.dataset.categoria) !== -1) &&
        (tamanhos.length === 0 || tamanhos.indexOf(card.dataset.tamanho) !== -1) &&
        (marcas.length === 0 || marcas.indexOf(card.dataset.marca) !== -1) &&
        (cores.length === 0 || cores.indexOf(card.dataset.cor) !== -1) &&
        (min === null || isNaN(preco) || preco >= min) &&
        (max === null || isNaN(preco) || preco <= max);

      if (!combina) {
        card.style.display = 'none';
        return;
      }

      correspondentes++;
      card.style.display = (filtroAtivo || correspondentes <= revelados) ? '' : 'none';
    });

    if (semResultado) {
      semResultado.style.display = correspondentes === 0 ? '' : 'none';
    }
    if (contagem) {
      if (filtroAtivo) {
        contagem.textContent = correspondentes + (correspondentes === 1 ? ' produto encontrado' : ' produtos encontrados');
      } else {
        contagem.textContent = 'Mostrando ' + Math.min(revelados, correspondentes) + ' de ' + correspondentes + ' produtos';
      }
    }
    if (botaoExibirMais) {
      botaoExibirMais.style.display = (!filtroAtivo && revelados < correspondentes) ? '' : 'none';
    }
  }

  [campoBusca, precoMin, precoMax].forEach(function (elemento) {
    if (elemento) {
      elemento.addEventListener('input', aplicarFiltros);
    }
  });

  todosCheckbox.forEach(function (input) {
    input.addEventListener('change', aplicarFiltros);
  });

  atualizarFiltroTamanho(valoresMarcados(checkboxCategoria));

  if (botaoExibirMais) {
    botaoExibirMais.addEventListener('click', function () {
      revelados += LOTE;
      aplicarFiltros();
    });
  }

  if (botaoLimpar) {
    botaoLimpar.addEventListener('click', function () {
      if (campoBusca) campoBusca.value = '';
      if (precoMin) precoMin.value = '';
      if (precoMax) precoMax.value = '';
      todosCheckbox.forEach(function (input) {
        input.checked = false;
      });
      revelados = LOTE;
      aplicarFiltros();
    });
  }

  // aproxima uma cor real para a bolinha de cada opcao de cor do filtro
  var mapaCores = {
    'Branco': '#FFFFFF', 'Off-white': '#F3EFE7', 'Creme': '#F0E6D2', 'Bege': '#E8DCC4',
    'Cinza': '#9AA0A6', 'Grafite': '#4B4F54', 'Chumbo': '#33363B', 'Preto': '#141414',
    'Natural': '#D8B98A', 'Amêndoa': '#C9A66B', 'Carvalho': '#B98553', 'Freijó': '#8C6239',
    'Imbuia': '#6B4226', 'Nogueira': '#5A3825', 'Mel': '#C68642', 'Caramelo': '#A9642B',
    'Marrom': '#6F4E37', 'Marrom-café': '#4B3621',
    'Azul': '#2B6CB0', 'Azul-marinho': '#1A2E5A', 'Verde': '#3E8E4F', 'Verde-oliva': '#6B7A3A',
    'Vermelho': '#B3261E', 'Bordô': '#6E1423', 'Rosa': '#E29CB4', 'Roxo': '#6B3FA0',
    'Amarelo': '#E8C547', 'Laranja': '#E07A2C', 'Terracota': '#B8552F',
    'Dourado': '#C9A227', 'Prata': '#BFC3C7', 'Cobre': '#B36A44'
  };
  document.querySelectorAll('.cor-dot').forEach(function (dot) {
    var cor = dot.dataset.cor;
    if (mapaCores[cor]) {
      dot.style.background = mapaCores[cor];
    }
  });

  aplicarFiltros();
});
