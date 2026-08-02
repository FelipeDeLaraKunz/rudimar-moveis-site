document.addEventListener('DOMContentLoaded', function () {
  var dialog = document.getElementById('promocaoModal');

  if (dialog) {
    if (dialog.dataset.abrir === 'true') {
      dialog.showModal();
    }

    dialog.querySelectorAll('[data-fechar-modal]').forEach(function (botao) {
      botao.addEventListener('click', function () {
        dialog.close();
      });
    });

    dialog.addEventListener('click', function (evento) {
      if (evento.target === dialog) {
        dialog.close();
      }
    });
  }

  // ---- percentual de desconto e opcional (pode variar por produto) ----
  var checkboxPercentual = document.getElementById('mostrarPercentual');
  var campoDesconto = document.getElementById('percentualDesconto');

  if (checkboxPercentual && campoDesconto) {
    checkboxPercentual.addEventListener('change', function () {
      campoDesconto.disabled = !checkboxPercentual.checked;
      if (!checkboxPercentual.checked) {
        campoDesconto.value = '';
      }
    });
  }

  // ---- selecao de produtos + preco promocional sugerido ----
  var lista = document.getElementById('promoProdutosLista');

  if (lista) {
    var linhas = Array.prototype.slice.call(lista.querySelectorAll('.promo-produto-linha'));

    linhas.forEach(function (linha) {
      var checkbox = linha.querySelector('.promo-produto-toggle');
      var precoInputs = Array.prototype.slice.call(linha.querySelectorAll('.promo-produto-preco'));
      var precoInput = precoInputs[0]; // preco a vista - usado pra sugestao automatica
      if (!checkbox || !precoInputs.length) {
        return;
      }

      checkbox.addEventListener('change', function () {
        precoInputs.forEach(function (input) {
          input.disabled = !checkbox.checked;
        });
        if (checkbox.checked && precoInput && !precoInput.value) {
          var precoOriginal = parseFloat(linha.dataset.precoOriginal);
          var desconto = campoDesconto ? parseFloat(campoDesconto.value) : NaN;
          if (!isNaN(precoOriginal) && !isNaN(desconto)) {
            precoInput.value = (precoOriginal * (1 - desconto / 100)).toFixed(2);
          }
        }
      });
    });

    var filtroProdutos = document.getElementById('filtroProdutosPromo');
    if (filtroProdutos) {
      filtroProdutos.addEventListener('input', function () {
        linhas.forEach(function (linha) {
          var combina = correspondeABusca(linha.dataset.nome, filtroProdutos.value);
          linha.style.display = combina ? '' : 'none';
        });
      });
    }
  }

  // ---- filtro da tabela de promocoes cadastradas ----
  var tabela = document.getElementById('tabelaPromocoes');
  if (!tabela) {
    return;
  }

  var linhasTabela = Array.prototype.slice.call(tabela.querySelectorAll('tbody tr'));
  var campoBusca = document.getElementById('filtroBuscaPromo');
  var selStatus = document.getElementById('filtroStatusPromo');
  var mensagemVazio = document.getElementById('filtroPromoVazio');

  function aplicarFiltrosTabela() {
    var termo = (campoBusca && campoBusca.value) || '';
    var status = selStatus ? selStatus.value : '';
    var visiveis = 0;

    linhasTabela.forEach(function (linha) {
      var combina =
        correspondeABusca(linha.dataset.busca, termo) &&
        (!status || linha.dataset.status === status);
      linha.style.display = combina ? '' : 'none';
      if (combina) visiveis++;
    });

    if (mensagemVazio) {
      mensagemVazio.style.display = visiveis === 0 ? '' : 'none';
    }
  }

  [campoBusca, selStatus].forEach(function (elemento) {
    if (elemento) {
      elemento.addEventListener('input', aplicarFiltrosTabela);
    }
  });
});
