document.addEventListener('DOMContentLoaded', function () {
  var dialog = document.getElementById('produtoModal');

  if (dialog) {
    if (dialog.dataset.abrir === 'true') {
      dialog.showModal();
    }

    dialog.querySelectorAll('[data-fechar-modal]').forEach(function (botao) {
      botao.addEventListener('click', function () {
        dialog.close();
      });
    });

    // fecha ao clicar fora (no backdrop do <dialog>)
    dialog.addEventListener('click', function (evento) {
      if (evento.target === dialog) {
        dialog.close();
      }
    });
  }

  configurarUploadFotos();
  configurarCorPersonalizada();
  configurarTamanhoPorCategoria();
  configurarPrecoAutomatico();

  var tabela = document.getElementById('tabelaProdutos');
  if (!tabela) {
    return;
  }

  var linhas = Array.prototype.slice.call(tabela.querySelectorAll('tbody tr'));
  var campoBusca = document.getElementById('filtroBusca');
  var selCategoria = document.getElementById('filtroCategoria');
  var selMarca = document.getElementById('filtroMarca');
  var selCor = document.getElementById('filtroCor');
  var selStatus = document.getElementById('filtroStatus');
  var mensagemVazio = document.getElementById('filtroVazio');
  var botaoExibirMais = document.getElementById('produtosExibirMais');

  // com filtro nenhum aplicado, mostra so um lote de linhas por vez (o resto ja esta no
  // HTML, so escondido) - evita carregar de uma vez as fotos de todas as linhas da tabela.
  // Assim que algum filtro entra em jogo, mostra todas as linhas que combinam.
  var LOTE = 30;
  var revelados = LOTE;

  function popularOpcoes(select, valores) {
    if (!select) return;
    Array.from(valores).sort(function (a, b) {
      return a.localeCompare(b, 'pt-BR');
    }).forEach(function (valor) {
      var opcao = document.createElement('option');
      opcao.value = valor;
      opcao.textContent = valor;
      select.appendChild(opcao);
    });
  }

  var categorias = new Set();
  var marcas = new Set();
  var cores = new Set();

  linhas.forEach(function (linha) {
    if (linha.dataset.categoria) categorias.add(linha.dataset.categoria);
    if (linha.dataset.marca) marcas.add(linha.dataset.marca);
    if (linha.dataset.cor) cores.add(linha.dataset.cor);
  });

  popularOpcoes(selCategoria, categorias);
  popularOpcoes(selMarca, marcas);
  popularOpcoes(selCor, cores);

  // guarda os filtros usados por ultimo nesta aba (sessionStorage), para nao perde-los
  // ao editar/excluir um produto: essas acoes recarregam a pagina inteira, e sem isso os
  // filtros (que so existem no DOM, escondendo/mostrando linhas) voltavam a zero.
  var CHAVE_FILTROS = 'admin-produtos-filtros';

  function salvarFiltros() {
    var filtros = {
      busca: campoBusca ? campoBusca.value : '',
      categoria: selCategoria ? selCategoria.value : '',
      marca: selMarca ? selMarca.value : '',
      cor: selCor ? selCor.value : '',
      status: selStatus ? selStatus.value : '',
      revelados: revelados
    };
    sessionStorage.setItem(CHAVE_FILTROS, JSON.stringify(filtros));
  }

  function restaurarFiltros() {
    var salvos;
    try {
      salvos = JSON.parse(sessionStorage.getItem(CHAVE_FILTROS));
    } catch (e) {
      salvos = null;
    }
    if (!salvos) return;

    if (campoBusca) campoBusca.value = salvos.busca || '';
    if (selCategoria) selCategoria.value = salvos.categoria || '';
    if (selMarca) selMarca.value = salvos.marca || '';
    if (selCor) selCor.value = salvos.cor || '';
    if (selStatus) selStatus.value = salvos.status || '';
    if (salvos.revelados) revelados = salvos.revelados;
  }

  function aplicarFiltros() {
    var termo = (campoBusca && campoBusca.value) || '';
    var categoria = selCategoria ? selCategoria.value : '';
    var marca = selMarca ? selMarca.value : '';
    var cor = selCor ? selCor.value : '';
    var status = selStatus ? selStatus.value : '';
    var filtroAtivo = !!termo || !!categoria || !!marca || !!cor || !!status;
    var correspondentes = 0;

    linhas.forEach(function (linha) {
      var combina =
        correspondeABusca(linha.dataset.busca, termo) &&
        (!categoria || linha.dataset.categoria === categoria) &&
        (!marca || linha.dataset.marca === marca) &&
        (!cor || linha.dataset.cor === cor) &&
        (!status || linha.dataset.status === status);

      if (!combina) {
        linha.style.display = 'none';
        return;
      }

      correspondentes++;
      linha.style.display = (filtroAtivo || correspondentes <= revelados) ? '' : 'none';
    });

    if (mensagemVazio) {
      mensagemVazio.style.display = correspondentes === 0 ? '' : 'none';
    }
    if (botaoExibirMais) {
      botaoExibirMais.style.display = (!filtroAtivo && revelados < correspondentes) ? '' : 'none';
    }

    salvarFiltros();
  }

  restaurarFiltros();
  aplicarFiltros();

  [campoBusca, selCategoria, selMarca, selCor, selStatus].forEach(function (elemento) {
    if (elemento) {
      elemento.addEventListener('input', aplicarFiltros);
    }
  });

  if (botaoExibirMais) {
    botaoExibirMais.addEventListener('click', function () {
      revelados += LOTE;
      aplicarFiltros();
    });
  }
});

function cabecalhosComCsrf() {
  var meta = document.querySelector('meta[name="_csrf"]');
  var metaHeader = document.querySelector('meta[name="_csrf_header"]');
  var cabecalhos = { 'Content-Type': 'application/json' };
  if (meta && metaHeader) {
    cabecalhos[metaHeader.content] = meta.content;
  }
  return cabecalhos;
}

// Upload de fotos com pre-visualizacao imediata e X para remover antes de salvar.
// Tres formas de adicionar: escolher do computador, colar o link de uma imagem da internet
// (o servidor baixa e devolve os bytes) ou colar (Ctrl+V) uma imagem copiada de outro lugar.
// Todas caem no mesmo array de File em memoria, que reconstroi o FileList do input via DataTransfer.
function configurarUploadFotos() {
  var input = document.getElementById('novasImagens');
  var botaoEscolher = document.getElementById('btnEscolherFotos');
  var preview = document.getElementById('imagensPreview');
  if (!input || !botaoEscolher || !preview) {
    return;
  }

  var campoUrl = document.getElementById('urlImagemColar');
  var botaoColarUrl = document.getElementById('btnColarUrl');
  var statusUrl = document.getElementById('urlImagemStatus');
  var dialog = document.getElementById('produtoModal');

  var arquivosSelecionados = [];

  botaoEscolher.addEventListener('click', function () {
    input.click();
  });

  input.addEventListener('change', function () {
    Array.prototype.slice.call(input.files).forEach(function (arquivo) {
      arquivosSelecionados.push(arquivo);
    });
    sincronizarInput();
    renderizarPreview();
  });

  // baixa uma imagem de uma URL (via /admin/produtos/baixar-imagem-url) e cai no mesmo
  // array/preview dos outros metodos de upload. Usado pelo botao "colar link".
  function baixarEAdicionarPorUrl(url, aoTerminar) {
    return fetch('/admin/produtos/baixar-imagem-url', {
      method: 'POST',
      headers: cabecalhosComCsrf(),
      body: JSON.stringify({ url: url })
    })
      .then(function (resposta) {
        if (!resposta.ok) {
          return resposta.json().then(function (dados) {
            throw new Error(dados.erro || 'Não foi possível baixar essa imagem.');
          });
        }
        return resposta.blob();
      })
      .then(function (blob) {
        var extensao = (blob.type.split('/')[1] || 'jpg').split('+')[0];
        var arquivo = new File([blob], 'imagem-' + Date.now() + '-' + Math.random().toString(36).slice(2) + '.' + extensao, { type: blob.type });
        arquivosSelecionados.push(arquivo);
        sincronizarInput();
        renderizarPreview();
        if (aoTerminar) aoTerminar(null);
      })
      .catch(function (erro) {
        if (aoTerminar) aoTerminar(erro);
      });
  }

  if (campoUrl && botaoColarUrl) {
    botaoColarUrl.addEventListener('click', function () {
      var url = campoUrl.value.trim();
      if (!url) {
        return;
      }

      botaoColarUrl.disabled = true;
      var textoOriginal = botaoColarUrl.textContent;
      botaoColarUrl.textContent = 'Baixando...';
      if (statusUrl) {
        statusUrl.textContent = '';
        statusUrl.className = 'upload-status';
      }

      baixarEAdicionarPorUrl(url, function (erro) {
        botaoColarUrl.disabled = false;
        botaoColarUrl.textContent = textoOriginal;
        if (erro) {
          if (statusUrl) {
            statusUrl.textContent = erro.message;
            statusUrl.className = 'upload-status upload-status-erro';
          }
        } else {
          campoUrl.value = '';
        }
      });
    });
  }

  document.addEventListener('paste', function (evento) {
    if (dialog && !dialog.open) {
      return;
    }
    if (!evento.clipboardData) {
      return;
    }
    var itens = evento.clipboardData.items || [];
    var encontrouImagem = false;
    for (var i = 0; i < itens.length; i++) {
      if (itens[i].kind === 'file' && itens[i].type.indexOf('image/') === 0) {
        var arquivo = itens[i].getAsFile();
        if (arquivo) {
          arquivosSelecionados.push(arquivo);
          encontrouImagem = true;
        }
      }
    }
    if (encontrouImagem) {
      sincronizarInput();
      renderizarPreview();
    }
  });

  function sincronizarInput() {
    var transferencia = new DataTransfer();
    arquivosSelecionados.forEach(function (arquivo) {
      transferencia.items.add(arquivo);
    });
    input.files = transferencia.files;
  }

  function renderizarPreview() {
    preview.innerHTML = '';
    arquivosSelecionados.forEach(function (arquivo, indice) {
      var item = document.createElement('div');
      item.className = 'imagem-preview-item';

      var img = document.createElement('img');
      img.src = URL.createObjectURL(arquivo);
      img.alt = arquivo.name;
      img.onload = function () {
        URL.revokeObjectURL(img.src);
      };

      var botaoRemover = document.createElement('button');
      botaoRemover.type = 'button';
      botaoRemover.className = 'imagem-preview-remover';
      botaoRemover.setAttribute('aria-label', 'Remover foto');
      botaoRemover.textContent = '×';
      botaoRemover.addEventListener('click', function () {
        arquivosSelecionados.splice(indice, 1);
        sincronizarInput();
        renderizarPreview();
      });

      item.appendChild(img);
      item.appendChild(botaoRemover);
      preview.appendChild(item);
    });
  }
}

// Permite digitar uma cor que nao esta na lista pronta: ao escolher "Outra (digitar)"
// no select, aparece um campo de texto, e o valor digitado vira uma option nova no
// select na hora de enviar o formulario (assim continua sendo so um campo de texto no
// banco, sem precisar mudar nada no back-end).
function configurarCorPersonalizada() {
  var selectCor = document.getElementById('cor');
  var inputCorOutra = document.getElementById('corOutraInput');
  var campoCorAtual = document.getElementById('corAtual');

  if (!selectCor || !inputCorOutra) {
    return;
  }

  var OPCAO_OUTRA = 'Outra';
  var form = selectCor.closest('form');

  function valoresPredefinidos() {
    return Array.prototype.map.call(selectCor.options, function (opcao) {
      return opcao.value;
    });
  }

  function removerOpcaoPersonalizada() {
    var existente = selectCor.querySelector('option[data-personalizada="true"]');
    if (existente) {
      existente.remove();
    }
  }

  function mostrarCampoTexto(valorInicial) {
    inputCorOutra.hidden = false;
    inputCorOutra.value = valorInicial || '';
  }

  function esconderCampoTexto() {
    inputCorOutra.hidden = true;
    inputCorOutra.value = '';
    inputCorOutra.setCustomValidity('');
  }

  // se o produto ja tem uma cor salva que nao esta na lista pronta (foi digitada antes),
  // seleciona "Outra" e mostra o valor real no campo de texto.
  if (campoCorAtual && campoCorAtual.value && valoresPredefinidos().indexOf(campoCorAtual.value) === -1) {
    selectCor.value = OPCAO_OUTRA;
    mostrarCampoTexto(campoCorAtual.value);
  } else if (selectCor.value === OPCAO_OUTRA) {
    mostrarCampoTexto('');
  } else {
    esconderCampoTexto();
  }

  selectCor.addEventListener('change', function () {
    removerOpcaoPersonalizada();
    if (selectCor.value === OPCAO_OUTRA) {
      mostrarCampoTexto('');
      inputCorOutra.focus();
    } else {
      esconderCampoTexto();
    }
  });

  if (form) {
    form.addEventListener('submit', function (evento) {
      if (selectCor.value !== OPCAO_OUTRA) {
        return;
      }
      var texto = inputCorOutra.value.trim();
      if (!texto) {
        evento.preventDefault();
        inputCorOutra.setCustomValidity('Digite o nome da cor.');
        inputCorOutra.reportValidity();
        return;
      }
      inputCorOutra.setCustomValidity('');
      removerOpcaoPersonalizada();
      var opcaoPersonalizada = document.createElement('option');
      opcaoPersonalizada.value = texto;
      opcaoPersonalizada.textContent = texto;
      opcaoPersonalizada.dataset.personalizada = 'true';
      selectCor.appendChild(opcaoPersonalizada);
      selectCor.value = texto;
    });
  }
}

// So categorias que costumam variar por tamanho (cama, conjunto box, colchao) mostram o
// campo "Tamanho" - mantido em sincronia manualmente com CategoriasCatalogo.CATEGORIAS_COM_TAMANHO
// (Java) e a mesma lista em catalogo.js.
var CATEGORIAS_COM_TAMANHO = ['Cama', 'Conjunto box', 'Colchão'];

function configurarTamanhoPorCategoria() {
  var selectCategoria = document.getElementById('categoria');
  var campoTamanho = document.getElementById('campoTamanho');
  var selectTamanho = document.getElementById('tamanho');

  if (!selectCategoria || !campoTamanho || !selectTamanho) {
    return;
  }

  function atualizar() {
    var precisaTamanho = CATEGORIAS_COM_TAMANHO.indexOf(selectCategoria.value) !== -1;
    campoTamanho.hidden = !precisaTamanho;
    if (!precisaTamanho) {
      selectTamanho.value = '';
    }
  }

  atualizar();
  selectCategoria.addEventListener('change', atualizar);
}

// Ao digitar (ou alterar) o preco parcelado, sugere o preco a vista com 10% de desconto -
// o admin ve o valor sugerido e pode ajustar antes de salvar, tanto criando um produto novo
// quanto editando um ja existente. Para de sugerir assim que o admin mexe direto no campo
// de preco a vista (nao fica recalculando por cima de um valor editado de proposito).
function configurarPrecoAutomatico() {
  var campoPreco = document.getElementById('preco');
  var campoPrecoCartao = document.getElementById('precoCartao');

  if (!campoPreco || !campoPrecoCartao) {
    return;
  }

  var precoTocadoManualmente = false;

  campoPreco.addEventListener('input', function () {
    precoTocadoManualmente = true;
  });

  campoPrecoCartao.addEventListener('input', function () {
    if (precoTocadoManualmente) {
      return;
    }
    var precoCartao = parseFloat(campoPrecoCartao.value);
    if (isNaN(precoCartao) || precoCartao <= 0) {
      return;
    }
    campoPreco.value = (precoCartao * 0.9).toFixed(2);
  });
}
