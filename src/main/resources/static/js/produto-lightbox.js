document.addEventListener('DOMContentLoaded', function () {
  var imagensCarousel = Array.prototype.slice.call(document.querySelectorAll('.carousel-slide img'));
  var lightbox = document.getElementById('lightbox');
  var lightboxImg = document.getElementById('lightboxImg');
  var botaoFechar = document.getElementById('lightboxFechar');
  var botaoPrev = document.getElementById('lightboxPrev');
  var botaoNext = document.getElementById('lightboxNext');

  if (!imagensCarousel.length || !lightbox || !lightboxImg) {
    return;
  }

  var urls = imagensCarousel.map(function (img) {
    return img.src;
  });
  var indiceAtual = 0;
  var ampliado = false;

  function resetarZoom() {
    ampliado = false;
    lightboxImg.classList.remove('lightbox-zoom');
    lightboxImg.style.transform = 'scale(1)';
    lightboxImg.style.transformOrigin = 'center center';
  }

  function mostrarIndice(indice) {
    indiceAtual = (indice + urls.length) % urls.length;
    resetarZoom();
    lightboxImg.src = urls[indiceAtual];
  }

  function abrir(indice) {
    mostrarIndice(indice);
    lightbox.classList.add('lightbox-aberto');
    document.body.style.overflow = 'hidden';
    var multiplasFotos = urls.length > 1;
    if (botaoPrev) botaoPrev.hidden = !multiplasFotos;
    if (botaoNext) botaoNext.hidden = !multiplasFotos;
  }

  function fechar() {
    lightbox.classList.remove('lightbox-aberto');
    document.body.style.overflow = '';
  }

  imagensCarousel.forEach(function (img, indice) {
    img.addEventListener('click', function () {
      abrir(indice);
    });
  });

  if (botaoFechar) {
    botaoFechar.addEventListener('click', fechar);
  }
  if (botaoPrev) {
    botaoPrev.addEventListener('click', function () {
      mostrarIndice(indiceAtual - 1);
    });
  }
  if (botaoNext) {
    botaoNext.addEventListener('click', function () {
      mostrarIndice(indiceAtual + 1);
    });
  }

  lightbox.addEventListener('click', function (evento) {
    if (evento.target === lightbox) {
      fechar();
    }
  });

  document.addEventListener('keydown', function (evento) {
    if (!lightbox.classList.contains('lightbox-aberto')) {
      return;
    }
    if (evento.key === 'Escape') fechar();
    if (evento.key === 'ArrowLeft') mostrarIndice(indiceAtual - 1);
    if (evento.key === 'ArrowRight') mostrarIndice(indiceAtual + 1);
  });

  // clique na imagem alterna zoom, ampliando a partir do ponto clicado
  lightboxImg.addEventListener('click', function (evento) {
    evento.stopPropagation();
    if (!ampliado) {
      var retangulo = lightboxImg.getBoundingClientRect();
      var origemX = ((evento.clientX - retangulo.left) / retangulo.width) * 100;
      var origemY = ((evento.clientY - retangulo.top) / retangulo.height) * 100;
      lightboxImg.style.transformOrigin = origemX + '% ' + origemY + '%';
      lightboxImg.style.transform = 'scale(2.2)';
      lightboxImg.classList.add('lightbox-zoom');
      ampliado = true;
    } else {
      resetarZoom();
    }
  });
});
