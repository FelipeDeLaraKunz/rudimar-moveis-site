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

  function aplicarFiltros() {
    var termo = (campoBusca && campoBusca.value) || '';
    var categoria = selCategoria ? selCategoria.value : '';
    var marca = selMarca ? selMarca.value : '';
    var cor = selCor ? selCor.value : '';
    var status = selStatus ? selStatus.value : '';
    var visiveis = 0;

    linhas.forEach(function (linha) {
      var combina =
        correspondeABusca(linha.dataset.busca, termo) &&
        (!categoria || linha.dataset.categoria === categoria) &&
        (!marca || linha.dataset.marca === marca) &&
        (!cor || linha.dataset.cor === cor) &&
        (!status || linha.dataset.status === status);

      linha.style.display = combina ? '' : 'none';
      if (combina) visiveis++;
    });

    if (mensagemVazio) {
      mensagemVazio.style.display = visiveis === 0 ? '' : 'none';
    }
  }

  [campoBusca, selCategoria, selMarca, selCor, selStatus].forEach(function (elemento) {
    if (elemento) {
      elemento.addEventListener('input', aplicarFiltros);
    }
  });
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
