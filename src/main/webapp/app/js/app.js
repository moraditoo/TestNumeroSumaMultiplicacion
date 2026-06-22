/* =========================================================================
   Test Numerico BFA - logica de la pagina de usuarios
   Se comunica con la API REST de OpenXava (misma base de datos PostgreSQL).
   ========================================================================= */

(function () {
  "use strict";

  // ---- Base de la API -------------------------------------------------
  // Si la pagina se sirve desde el propio OpenXava (.../app/), la API queda
  // en .../api (mismo origen). Si se abre suelta, apunta a localhost:8080.
  function detectarApiBase() {
    var p = window.location.pathname;
    var i = p.indexOf("/app/");
    if (window.location.protocol.startsWith("http") && i !== -1) {
      return window.location.origin + p.substring(0, i) + "/api";
    }
    return "http://localhost:8080/TestNumericoSumaMultiplicacion/api";
  }
  var API = detectarApiBase();

  // ---- Estado ---------------------------------------------------------
  var estado = {
    usuario: null,
    test: null,
    preguntas: [],
    respuestas: {},     // idPregunta -> "A" | "B"
    indice: 0,
    segundosRestantes: 0,
    timer: null,
    enviando: false
  };

  // ---- Utilidades de pantalla ----------------------------------------
  function $(id) { return document.getElementById(id); }
  function mostrar(idPantalla) {
    document.querySelectorAll(".screen").forEach(function (s) { s.classList.remove("active"); });
    $(idPantalla).classList.add("active");
    window.scrollTo(0, 0);
  }

  function toast(mensaje, tipo) {
    var host = $("toastHost");
    var div = document.createElement("div");
    div.className = "toast align-items-center text-bg-" + (tipo || "dark") + " border-0 show mb-2";
    div.role = "alert";
    div.innerHTML = '<div class="d-flex"><div class="toast-body">' + mensaje +
      '</div><button type="button" class="btn-close btn-close-white me-2 m-auto"></button></div>';
    host.appendChild(div);
    div.querySelector(".btn-close").addEventListener("click", function () { div.remove(); });
    setTimeout(function () { div.remove(); }, 4000);
  }

  // ---- Llamadas a la API ---------------------------------------------
  async function api(ruta, opciones) {
    var resp = await fetch(API + ruta, opciones);
    var datos = null;
    try { datos = await resp.json(); } catch (e) { datos = null; }
    if (!resp.ok) {
      var msg = (datos && datos.error) ? datos.error : ("Error " + resp.status);
      throw new Error(msg);
    }
    return datos;
  }

  // ====================================================================
  //  LOGIN
  // ====================================================================
  async function iniciarSesion() {
    var usuario = $("loginUser").value.trim();
    var contrasena = $("loginPass").value;
    var err = $("loginError");
    err.classList.add("d-none");

    if (!usuario || !contrasena) {
      err.textContent = "Ingrese su usuario y contrasena.";
      err.classList.remove("d-none");
      return;
    }
    try {
      var u = await api("/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ usuario: usuario, contrasena: contrasena })
      });
      estado.usuario = u;
      sessionStorage.setItem("bfaUsuario", JSON.stringify(u));
      entrarAlPanel();
    } catch (e) {
      err.textContent = e.message;
      err.classList.remove("d-none");
    }
  }

  function cerrarSesion() {
    detenerTimer();
    estado.usuario = null;
    sessionStorage.removeItem("bfaUsuario");
    $("userBox").classList.add("d-none");
    $("loginUser").value = "";
    $("loginPass").value = "";
    mostrar("screenLogin");
  }

  function entrarAlPanel() {
    $("userName").textContent = estado.usuario.nombres + " " + estado.usuario.apellidos;
    $("userBox").classList.remove("d-none");
    mostrar("screenDashboard");
    cargarTests();
    cargarHistorial();
  }

  // ====================================================================
  //  PANEL: tests e historial
  // ====================================================================
  async function cargarTests() {
    var cont = $("testList");
    cont.innerHTML = '<div class="col-12 muted small">Cargando tests...</div>';
    try {
      var tests = await api("/tests", {});
      if (!tests.length) {
        cont.innerHTML = '<div class="col-12 muted">No hay tests disponibles. El evaluador debe activarlos y registrar preguntas.</div>';
        return;
      }
      cont.innerHTML = "";
      tests.forEach(function (t) {
        var col = document.createElement("div");
        col.className = "col-12 col-md-6";
        var esMult = (t.tipoTest || "").toUpperCase().indexOf("MULT") !== -1;
        col.innerHTML =
          '<div class="sheet test-card h-100">' +
            '<div class="d-flex justify-content-between align-items-start mb-2">' +
              '<span class="test-tag">' + (esMult ? "Multiplicaciones" : "Sumas") + '</span>' +
              '<span class="divider-note">4 min</span>' +
            '</div>' +
            '<h3 class="h5 fw-bold mb-1">' + t.nombre + '</h3>' +
            '<p class="test-meta mb-3">' + (t.descripcion || "") + '</p>' +
            '<div class="d-flex justify-content-between align-items-center">' +
              '<span class="test-meta"><strong>' + t.cantidadPreguntas + '</strong> preguntas</span>' +
              '<span class="btn btn-sm btn-teal">Realizar test</span>' +
            '</div>' +
          '</div>';
        col.querySelector(".test-card").addEventListener("click", function () { abrirInstrucciones(t); });
        cont.appendChild(col);
      });
    } catch (e) {
      cont.innerHTML = '<div class="col-12 text-danger small">No se pudieron cargar los tests: ' + e.message + '</div>';
    }
  }

  async function cargarHistorial() {
    var tbody = $("historyBody");
    try {
      var lista = await api("/intentos?idUsuario=" + estado.usuario.idUsuario, {});
      if (!lista.length) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center muted py-4">Sin intentos registrados todavia.</td></tr>';
        return;
      }
      tbody.innerHTML = "";
      lista.forEach(function (i) {
        var esMult = (i.tipoTest || "").toUpperCase().indexOf("MULT") !== -1;
        var tr = document.createElement("tr");
        tr.innerHTML =
          "<td class='mono small'>" + formatearFecha(i.fechaInicio) + "</td>" +
          "<td>" + i.test + "</td>" +
          "<td><span class='badge-soft " + (esMult ? "badge-tipo-mult" : "badge-tipo-sumas") + "'>" +
              (esMult ? "MULT" : "SUMAS") + "</span></td>" +
          "<td class='text-center fw-semibold' style='color:var(--correct)'>" + i.aciertos + "</td>" +
          "<td class='text-center fw-semibold' style='color:var(--wrong)'>" + i.errores + "</td>" +
          "<td class='text-center muted'>" + i.sinResponder + "</td>" +
          "<td class='text-center mono small'>" + formatearTiempo(i.tiempoUsadoSegundos) + "</td>" +
          "<td><span class='badge-soft'>" + traducirEstado(i.estado) + "</span></td>";
        tbody.appendChild(tr);
      });
    } catch (e) {
      tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-3">' + e.message + '</td></tr>';
    }
  }

  function traducirEstado(e) {
    if (e === "CALIFICADO") return "Calificado";
    if (e === "FINALIZADO") return "Finalizado";
    if (e === "CANCELADO") return "Cerrado por tiempo";
    if (e === "EN_PROGRESO") return "En progreso";
    return e || "";
  }

  // ====================================================================
  //  INSTRUCCIONES
  // ====================================================================
  function abrirInstrucciones(test) {
    estado.test = test;
    $("instrTitle").textContent = test.nombre;
    mostrar("screenInstructions");
  }

  // ====================================================================
  //  TEST
  // ====================================================================
  async function comenzarTest() {
    try {
      var preguntas = await api("/tests/" + estado.test.idTestNumerico + "/preguntas", {});
      if (!preguntas.length) {
        toast("Este test no tiene preguntas activas.", "warning");
        return;
      }
      estado.preguntas = preguntas;
      estado.respuestas = {};
      estado.indice = 0;
      estado.segundosRestantes = 4 * 60;

      $("testRunningName").textContent = estado.test.nombre;
      var esMult = (estado.test.tipoTest || "").toUpperCase().indexOf("MULT") !== -1;
      $("testRunningTipo").textContent = esMult ? "Numerico — Multiplicaciones" : "Numerico — Sumas";

      construirRiel();
      mostrar("screenTest");
      pintarPregunta();
      iniciarTimer();
    } catch (e) {
      toast("No se pudo iniciar el test: " + e.message, "danger");
    }
  }

  function construirRiel() {
    var rail = $("answerRail");
    rail.innerHTML = "";
    estado.preguntas.forEach(function (p, idx) {
      var dot = document.createElement("div");
      dot.className = "rail-dot";
      dot.textContent = (idx + 1);
      dot.addEventListener("click", function () { estado.indice = idx; pintarPregunta(); });
      rail.appendChild(dot);
    });
  }

  function actualizarRiel() {
    var dots = $("answerRail").children;
    estado.preguntas.forEach(function (p, idx) {
      var dot = dots[idx];
      dot.classList.toggle("answered", !!estado.respuestas[p.idPregunta]);
      dot.classList.toggle("current", idx === estado.indice);
    });
  }

  function pintarPregunta() {
    var p = estado.preguntas[estado.indice];
    $("questionCounter").textContent = "Pregunta " + (estado.indice + 1) + " de " + estado.preguntas.length;
    $("opText").textContent = p.operacion;
    $("opResult").textContent = formatearNumero(p.resultadoMostrado);

    var seleccion = estado.respuestas[p.idPregunta] || null;
    document.querySelectorAll(".bubble-option").forEach(function (b) {
      b.classList.toggle("selected", b.dataset.value === seleccion);
    });

    var pct = Math.round(((estado.indice + 1) / estado.preguntas.length) * 100);
    $("testProgress").style.width = pct + "%";

    var esUltima = estado.indice === estado.preguntas.length - 1;
    $("btnNext").classList.toggle("d-none", esUltima);
    $("btnFinish").classList.toggle("d-none", !esUltima);
    $("btnPrev").disabled = estado.indice === 0;

    actualizarRiel();
  }

  function marcar(valor) {
    var p = estado.preguntas[estado.indice];
    estado.respuestas[p.idPregunta] = valor;
    document.querySelectorAll(".bubble-option").forEach(function (b) {
      b.classList.toggle("selected", b.dataset.value === valor);
    });
    actualizarRiel();
    // Avance automatico suave si no es la ultima
    if (estado.indice < estado.preguntas.length - 1) {
      setTimeout(function () { estado.indice++; pintarPregunta(); }, 250);
    }
  }

  function siguiente() { if (estado.indice < estado.preguntas.length - 1) { estado.indice++; pintarPregunta(); } }
  function anterior() { if (estado.indice > 0) { estado.indice--; pintarPregunta(); } }

  // ---- Temporizador ---------------------------------------------------
  function iniciarTimer() {
    detenerTimer();
    pintarReloj();
    estado.timer = setInterval(function () {
      estado.segundosRestantes--;
      pintarReloj();
      if (estado.segundosRestantes <= 0) {
        detenerTimer();
        enviarIntento(true);
      }
    }, 1000);
  }
  function detenerTimer() { if (estado.timer) { clearInterval(estado.timer); estado.timer = null; } }

  function pintarReloj() {
    var clock = $("examClock");
    clock.textContent = formatearTiempo(estado.segundosRestantes);
    clock.classList.remove("warn", "danger");
    if (estado.segundosRestantes <= 15) clock.classList.add("danger");
    else if (estado.segundosRestantes <= 60) clock.classList.add("warn");
  }

  // ---- Envio ----------------------------------------------------------
  async function enviarIntento(porTiempo) {
    if (estado.enviando) return;
    estado.enviando = true;
    detenerTimer();

    var respuestas = estado.preguntas.map(function (p) {
      return { idPregunta: p.idPregunta, respuestaMarcada: estado.respuestas[p.idPregunta] || null };
    });

    try {
      var r = await api("/intentos", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          idUsuario: estado.usuario.idUsuario,
          idTestNumerico: estado.test.idTestNumerico,
          cerradoPorTiempo: !!porTiempo,
          respuestas: respuestas
        })
      });
      mostrarResultado(r, porTiempo);
    } catch (e) {
      toast("No se pudo guardar el intento: " + e.message, "danger");
    } finally {
      estado.enviando = false;
    }
  }

  function mostrarResultado(r, porTiempo) {
    $("resAciertos").textContent = r.aciertos;
    $("resErrores").textContent = r.errores;
    $("resBlanco").textContent = r.sinResponder;
    $("resultSummary").textContent =
      (porTiempo ? "El tiempo se agoto. " : "") +
      "Respondio " + (r.aciertos + r.errores) + " de " + r.cantidadPreguntas + " ejercicios.";
    $("resTiempo").textContent = "Tiempo utilizado: " + formatearTiempo(r.tiempoUsadoSegundos);
    mostrar("screenResult");
  }

  // ---- Formato --------------------------------------------------------
  function formatearTiempo(seg) {
    seg = Math.max(0, seg | 0);
    var m = Math.floor(seg / 60), s = seg % 60;
    return m + ":" + (s < 10 ? "0" : "") + s;
  }
  function formatearNumero(n) { return Number(n).toLocaleString("es-NI"); }
  function formatearFecha(iso) {
    if (!iso) return "";
    var d = new Date(iso);
    if (isNaN(d)) return iso.substring(0, 16).replace("T", " ");
    return d.toLocaleString("es-NI", { dateStyle: "short", timeStyle: "short" });
  }

  // ====================================================================
  //  EVENTOS
  // ====================================================================
  function registrarEventos() {
    $("btnLogin").addEventListener("click", iniciarSesion);
    $("loginPass").addEventListener("keydown", function (e) { if (e.key === "Enter") iniciarSesion(); });
    $("loginUser").addEventListener("keydown", function (e) { if (e.key === "Enter") $("loginPass").focus(); });
    $("btnLogout").addEventListener("click", cerrarSesion);
    $("btnReload").addEventListener("click", cargarHistorial);

    $("btnBackToDash").addEventListener("click", function () { mostrar("screenDashboard"); });
    $("btnStartTest").addEventListener("click", comenzarTest);

    document.querySelectorAll(".bubble-option").forEach(function (b) {
      b.addEventListener("click", function () { marcar(b.dataset.value); });
    });
    $("btnNext").addEventListener("click", siguiente);
    $("btnPrev").addEventListener("click", anterior);
    $("btnFinish").addEventListener("click", function () { enviarIntento(false); });

    $("btnResultDash").addEventListener("click", function () {
      mostrar("screenDashboard");
      cargarHistorial();
    });

    // Atajos de teclado durante el test: A / B / flechas
    document.addEventListener("keydown", function (e) {
      if (!$("screenTest").classList.contains("active")) return;
      if (e.key === "a" || e.key === "A") marcar("A");
      else if (e.key === "b" || e.key === "B") marcar("B");
      else if (e.key === "ArrowRight") siguiente();
      else if (e.key === "ArrowLeft") anterior();
    });
  }

  // ---- Arranque -------------------------------------------------------
  function iniciar() {
    registrarEventos();
    var guardado = sessionStorage.getItem("bfaUsuario");
    if (guardado) {
      try { estado.usuario = JSON.parse(guardado); entrarAlPanel(); return; }
      catch (e) { sessionStorage.removeItem("bfaUsuario"); }
    }
    mostrar("screenLogin");
  }

  document.addEventListener("DOMContentLoaded", iniciar);
})();
