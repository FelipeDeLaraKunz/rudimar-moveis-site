// Carrossel de "Nossos produtos" na home: varios cards visiveis, rolando
// horizontalmente. As setas avancam/voltam um card por vez (com base na
// largura real do primeiro card + gap), e ficam desabilitadas nas pontas.
document.addEventListener('DOMContentLoaded', function () {
  var track = document.getElementById('vitrineTrack');
  if (!track) {
    return;
  }

  var prevBtn = document.getElementById('vitrinePrev');
  var nextBtn = document.getElementById('vitrineNext');

  function passo() {
    var slide = track.querySelector('.vitrine-slide');
    if (!slide) {
      return track.clientWidth;
    }
    var estiloTrack = getComputedStyle(track);
    var gap = parseFloat(estiloTrack.columnGap || estiloTrack.gap || '0') || 0;
    return slide.getBoundingClientRect().width + gap;
  }

  function atualizarSetas() {
    if (!prevBtn && !nextBtn) {
      return;
    }
    var maximo = track.scrollWidth - track.clientWidth - 1;
    var semOverflow = maximo <= 0;
    if (prevBtn) {
      prevBtn.disabled = semOverflow || track.scrollLeft <= 0;
    }
    if (nextBtn) {
      nextBtn.disabled = semOverflow || track.scrollLeft >= maximo;
    }
  }

  if (prevBtn) {
    prevBtn.addEventListener('click', function () {
      track.scrollBy({ left: -passo(), behavior: 'smooth' });
    });
  }

  if (nextBtn) {
    nextBtn.addEventListener('click', function () {
      track.scrollBy({ left: passo(), behavior: 'smooth' });
    });
  }

  var scrollTimeout;
  track.addEventListener('scroll', function () {
    clearTimeout(scrollTimeout);
    scrollTimeout = setTimeout(atualizarSetas, 80);
  });

  window.addEventListener('resize', atualizarSetas);
  atualizarSetas();
});
