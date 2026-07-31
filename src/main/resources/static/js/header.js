document.addEventListener('DOMContentLoaded', function () {
  var botao = document.getElementById('navToggle');
  var menu = document.getElementById('headerMenu');

  if (!botao || !menu) {
    return;
  }

  botao.addEventListener('click', function () {
    var aberto = menu.classList.toggle('header-menu-aberto');
    botao.setAttribute('aria-expanded', aberto ? 'true' : 'false');
  });

  // fecha o menu ao clicar num link (evita ficar aberto ao navegar para a mesma pagina, ex: ancoras)
  menu.querySelectorAll('a').forEach(function (link) {
    link.addEventListener('click', function () {
      menu.classList.remove('header-menu-aberto');
      botao.setAttribute('aria-expanded', 'false');
    });
  });
});
