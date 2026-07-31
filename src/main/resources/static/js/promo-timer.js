document.addEventListener('DOMContentLoaded', function () {
  var timers = Array.prototype.slice.call(document.querySelectorAll('.promo-timer[data-fim]'));
  if (timers.length === 0) {
    return;
  }

  function doisDigitos(numero) {
    return numero < 10 ? '0' + numero : '' + numero;
  }

  function formatar(ms) {
    if (ms <= 0) {
      return null;
    }
    var segundosTotais = Math.floor(ms / 1000);
    var dias = Math.floor(segundosTotais / 86400);
    var horas = Math.floor((segundosTotais % 86400) / 3600);
    var minutos = Math.floor((segundosTotais % 3600) / 60);
    var segundos = segundosTotais % 60;

    if (dias > 0) {
      return dias + 'd ' + doisDigitos(horas) + 'h ' + doisDigitos(minutos) + 'm';
    }
    return doisDigitos(horas) + ':' + doisDigitos(minutos) + ':' + doisDigitos(segundos);
  }

  function atualizar() {
    timers.forEach(function (timer) {
      var valor = timer.querySelector('.promo-timer-valor');
      if (!valor) {
        return;
      }
      var fim = new Date(timer.dataset.fim).getTime();
      var texto = formatar(fim - Date.now());
      if (texto === null) {
        valor.textContent = 'Encerrada';
        timer.classList.add('promo-timer-encerrado');
      } else {
        valor.textContent = texto;
      }
    });
  }

  atualizar();
  setInterval(atualizar, 1000);
});
