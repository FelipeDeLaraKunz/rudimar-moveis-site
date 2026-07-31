// Normaliza texto para busca: remove acentos, baixa a caixa e junta espacos repetidos.
// "Sofa" e "Sofa" (sofa com acento) viram a mesma coisa.
var MAPA_ACENTOS_BUSCA = {
  'a': 'áàâã', 'e': 'éèê', 'i': 'íì', 'o': 'óòôõ', 'u': 'úù', 'c': 'ç', 'n': 'ñ'
};

function normalizarBusca(texto) {
  var resultado = (texto || '').toString().toLowerCase().trim().replace(/\s+/g, ' ');
  Object.keys(MAPA_ACENTOS_BUSCA).forEach(function (semAcento) {
    var comAcento = MAPA_ACENTOS_BUSCA[semAcento];
    for (var i = 0; i < comAcento.length; i++) {
      resultado = resultado.split(comAcento[i]).join(semAcento);
    }
  });
  return resultado;
}

// true se todas as palavras digitadas aparecem em algum lugar do texto alvo
// (nao precisa ser a frase exata nem estar na mesma ordem)
function correspondeABusca(textoAlvo, termoBusca) {
  var termo = normalizarBusca(termoBusca);
  if (!termo) {
    return true;
  }
  var alvo = normalizarBusca(textoAlvo);
  return termo.split(' ').every(function (palavra) {
    return palavra === '' || alvo.indexOf(palavra) !== -1;
  });
}
