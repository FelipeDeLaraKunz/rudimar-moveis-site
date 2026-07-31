document.addEventListener('DOMContentLoaded', function () {
  var track = document.getElementById('carouselTrack');
  if (!track) {
    return;
  }

  var slides = Array.prototype.slice.call(track.children);
  if (slides.length <= 1) {
    return;
  }

  var prevBtn = document.getElementById('carouselPrev');
  var nextBtn = document.getElementById('carouselNext');
  var contador = document.getElementById('carouselContadorAtual');
  var miniaturas = Array.prototype.slice.call(document.querySelectorAll('#miniaturas .miniatura'));

  function indiceAtual() {
    var largura = track.clientWidth;
    if (!largura) return 0;
    return Math.round(track.scrollLeft / largura);
  }

  function irPara(indice) {
    var alvo = Math.max(0, Math.min(indice, slides.length - 1));
    track.scrollTo({ left: alvo * track.clientWidth, behavior: 'smooth' });
  }

  function atualizarIndicadores() {
    var indice = indiceAtual();
    if (contador) {
      contador.textContent = indice + 1;
    }
    miniaturas.forEach(function (miniatura, i) {
      miniatura.classList.toggle('miniatura-ativa', i === indice);
    });
  }

  if (prevBtn) {
    prevBtn.addEventListener('click', function () {
      irPara(indiceAtual() - 1);
    });
  }

  if (nextBtn) {
    nextBtn.addEventListener('click', function () {
      irPara(indiceAtual() + 1);
    });
  }

  miniaturas.forEach(function (miniatura) {
    miniatura.addEventListener('click', function () {
      irPara(parseInt(miniatura.dataset.index, 10));
    });
  });

  var scrollTimeout;
  track.addEventListener('scroll', function () {
    clearTimeout(scrollTimeout);
    scrollTimeout = setTimeout(atualizarIndicadores, 80);
  });

  var resizeTimeout;
  window.addEventListener('resize', function () {
    clearTimeout(resizeTimeout);
    resizeTimeout = setTimeout(function () {
      irPara(indiceAtual());
    }, 150);
  });
});
