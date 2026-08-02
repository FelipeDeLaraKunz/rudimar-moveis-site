document.addEventListener('DOMContentLoaded', function () {
  var botao = document.getElementById('btnCompartilhar');
  if (!botao) {
    return;
  }

  var status = document.getElementById('compartilharStatus');

  function mostrarStatus(texto) {
    if (!status) return;
    status.textContent = texto;
    status.classList.add('compartilhar-status-visivel');
    setTimeout(function () {
      status.classList.remove('compartilhar-status-visivel');
    }, 2500);
  }

  botao.addEventListener('click', function () {
    var dados = {
      title: document.title,
      text: 'Confira esse produto na Rudimar Móveis!',
      url: window.location.href
    };

    // no celular usa o menu nativo de compartilhamento (WhatsApp, Instagram, etc.)
    if (navigator.share) {
      navigator.share(dados).catch(function () {
        // usuario cancelou o compartilhamento - nao precisa mostrar erro
      });
      return;
    }

    // no desktop (sem suporte a navigator.share), copia o link pro usuario colar onde quiser
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href)
        .then(function () {
          mostrarStatus('Link copiado!');
        })
        .catch(function () {
          mostrarStatus('Não foi possível copiar o link.');
        });
    }
  });
});
