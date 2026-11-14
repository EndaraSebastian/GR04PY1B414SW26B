import { crearLectorQR } from "./qr.js";

const app = document.querySelector("#app");
const identidad = document.querySelector("#sesion-usuario");
const cerrar = document.querySelector("#cerrar-sesion");
const reloj = new Intl.DateTimeFormat("es-EC", {
    dateStyle: "medium", timeStyle: "short", timeZone: "America/Guayaquil",
});
let sesion = {};
let vista = "";
let generacion = 0;
let lector;

const iconos = {
    entrar: '<path d="M15 3h5a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1h-5M3 12h13m-5-5 5 5-5 5"/>',
    salir: '<path d="M9 3H4a1 1 0 0 0-1 1v16a1 1 0 0 0 1 1h5m0-9h12m-5-5 5 5-5 5"/>',
    agregar: '<path d="M12 5v14M5 12h14"/>',
    solicitud: '<path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9zM14 3v6h6M8 13h8m-8 4h5"/>',
    consulta: '<circle cx="10.5" cy="10.5" r="5.5"/><path d="m15 15 5 5"/>',
    revisar: '<path d="M8 5H5v16h14V5h-3M9 3h6v4H9zm0 11 2 2 4-4"/>',
};

function icono(nombre) {
    return `<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor"
        stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round">${iconos[nombre]}</svg>`;
}

function escapar(valor = "") {
    return String(valor).replace(/[&<>"']/g, caracter => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
    })[caracter]);
}

class ErrorApi extends Error {
    constructor(estado, mensaje) {
        super(mensaje);
        this.estado = estado;
    }
}

async function api(recurso, datos) {
    const opciones = { cache: "no-store" };
    if (datos !== undefined) {
        opciones.method = "POST";
        opciones.headers = { "X-Parqueadero": "1" };
        opciones.body = new URLSearchParams(datos);
    }
    let respuesta;
    try {
        respuesta = await fetch(`/api/${recurso}`, opciones);
    } catch {
        throw new Error("No hay conexión con el servidor. Intenta de nuevo.");
    }
    if (!respuesta.headers.get("content-type")?.includes("application/json")) {
        throw new Error("El servidor no respondió como se esperaba. Intenta de nuevo.");
    }
    const cuerpo = await respuesta.json();
    if (!respuesta.ok) throw new ErrorApi(respuesta.status, cuerpo.error);
    return cuerpo;
}

function prepararVista(nombre) {
    lector?.destruir();
    lector = undefined;
    vista = nombre;
    generacion++;
    return generacion;
}

function irInicio(mensaje = "") {
    prepararVista("inicio");
    cerrar.hidden = !sesion.autenticado;
    identidad.hidden = !sesion.autenticado;
    identidad.textContent = sesion.autenticado ? sesion.nombre : "";
    if (!sesion.autenticado) {
        app.innerHTML = `<section class="pantalla bienvenida" aria-labelledby="titulo">
            <div class="encabezado">
                <h1 id="titulo">Parqueadero EPN</h1>
                <p>Solicita tu autorización y escanea el código QR para entrar o salir del parqueadero.</p>
            </div>
            ${mensaje ? `<p class="aviso" role="status">${escapar(mensaje)}</p>` : ""}
            <div class="rejilla-acciones">
                ${accion("login", "entrar", "Iniciar sesión", "Accede con tu cédula y clave.")}
                ${accion("registro", "agregar", "Registrarme", "Crea tu cuenta de miembro o visitante.")}
            </div>
        </section>`;
    } else {
        const admin = sesion.tipoUsuario === "ADMIN";
        app.innerHTML = `<section class="pantalla" aria-labelledby="titulo">
            <div class="encabezado">
                <p class="ceja">${admin ? "ADMINISTRACIÓN" : tipoUsuario(sesion.tipoUsuario)}</p>
                <h1 id="titulo">${admin ? "Panel de administración" : `Hola, ${escapar(sesion.nombre)}`}</h1>
                <p>${admin ? "Busca una solicitud para revisarla y comunicar tu decisión." : "¿Qué necesitas hacer?"}</p>
            </div>
            ${mensaje ? `<p class="aviso" role="status">${escapar(mensaje)}</p>` : ""}
            <div class="rejilla-acciones">
                ${admin ? accion("revision", "revisar", "Revisar solicitud", "Aprueba, solicita correcciones o rechaza.")
                : accion("solicitud", "solicitud", "Solicitar autorización", "Envía una solicitud de uso.") +
                  accion("consulta", "consulta", "Consultar solicitud", "Revisa el estado y la vigencia.") +
                  accion("ingreso", "entrar", "Ingresar", "Escanea el QR de entrada.") +
                  accion("salida", "salir", "Salir", "Escanea el QR de salida.")}
            </div>
        </section>`;
    }
    app.focus();
}

function accion(destino, simbolo, titulo, descripcion) {
    return `<button type="button" class="accion" data-ir="${destino}">
        <span class="accion-icono">${icono(simbolo)}</span>
        <span class="accion-texto"><strong>${titulo}</strong><small>${descripcion}</small></span>
        <span aria-hidden="true">›</span>
    </button>`;
}

function pantalla(nombre, titulo, bajada, cuerpo) {
    const id = prepararVista(nombre);
    app.innerHTML = `<section class="pantalla estrecha" aria-labelledby="titulo">
        <button class="volver" type="button" data-ir="inicio">← Volver al inicio</button>
        <div class="encabezado">
            <h1 id="titulo">${titulo}</h1>
            ${bajada ? `<p>${bajada}</p>` : ""}
        </div>
        <div class="tarjeta">
            <p id="aviso" class="aviso" role="status" aria-live="polite" hidden></p>
            ${cuerpo}
        </div>
    </section>`;
    app.focus();
    return id;
}

function aviso(mensaje, clase = "") {
    const nodo = document.querySelector("#aviso");
    nodo.textContent = mensaje;
    nodo.className = `aviso ${clase}`;
    nodo.hidden = !mensaje;
}

function campo(etiqueta, contenido) {
    return `<label class="campo"><span>${etiqueta}</span>${contenido}</label>`;
}

function boton(texto, extra = "") {
    return `<button class="boton boton-primario" ${extra}>${texto}</button>`;
}

function encadenarFormulario(recurso, alCompletar) {
    const formulario = app.querySelector("form");
    const vigente = generacion;
    formulario.addEventListener("submit", async evento => {
        evento.preventDefault();
        const enviar = formulario.querySelector('[type="submit"]');
        enviar.disabled = true;
        aviso("");
        try {
            const respuesta = await api(recurso, new FormData(formulario));
            if (vigente === generacion) alCompletar(respuesta);
        } catch (error) {
            if (vigente === generacion) mostrarError(error, recurso);
        } finally {
            enviar.disabled = false;
        }
    });
}

function mostrarError(error, recurso = "") {
    if (error.estado === 401 && recurso !== "login") {
        sesion = {};
        irInicio("Tu sesión terminó. Inicia sesión de nuevo.");
    } else {
        aviso(error.message, "error");
    }
}

function acceso(registro = false, mensaje = "") {
    const cedula = campo("Cédula o identificador", '<input name="cedula" autocomplete="username" maxlength="25" required>');
    const clave = campo("Clave", `<input type="password" name="clave"
        autocomplete="${registro ? "new-password" : "current-password"}" maxlength="128" required>`);
    const perfil = registro ? campo("Nombre completo", '<input name="nombre" autocomplete="name" maxlength="100" required>') +
        campo("Tipo de usuario", `<select name="tipoUsuario" required>
            <option value="">Elige una opción</option><option value="MCP">Miembro de la comunidad politécnica</option>
            <option value="VISITANTE">Visitante</option></select>`) +
        campo("Placa", '<input name="placa" autocapitalize="characters" maxlength="20" required>') +
        campo("Tipo de vehículo", `<select name="tipoVehiculo" required>
            <option value="">Elige una opción</option><option value="MOTO">MOTO</option>
            <option value="AUTOMOVIL">AUTOMOVIL</option></select>`) : "";
    pantalla(registro ? "registro" : "login", registro ? "Crear cuenta" : "Iniciar sesión", "",
        `<form>${cedula}${perfil}${clave}${boton(registro ? "Crear cuenta" : "Ingresar", 'type="submit"')}</form>`);
    if (mensaje) aviso(mensaje, "exito");
    encadenarFormulario(registro ? "registro" : "login", respuesta => {
        if (registro) acceso(false, respuesta.mensaje);
        else {
            sesion = respuesta;
            irInicio();
        }
    });
}

function tipoUsuario(tipo) {
    return tipo === "MCP" ? "Comunidad politécnica" : "Visitante";
}

function detalle(pares) {
    return `<dl class="datos">${pares.map(([etiqueta, valor]) =>
        `<div><dt>${etiqueta}</dt><dd>${escapar(valor)}</dd></div>`).join("")}</dl>`;
}

function datosSolicitud(datos) {
    const campos = [["Tipo", datos.tipo], ["Estado", datos.estado], ["Solicitante", datos.nombre],
        ["Cédula", datos.cedula], ["Placa", datos.placa], ["Vehículo", datos.tipoVehiculo]];
    if (datos.motivo) campos.push(["Motivo de visita", datos.motivo]);
    if (datos.observaciones) campos.push(["Observaciones", datos.observaciones]);
    if (datos.codigo) {
        campos.push(["Código", datos.codigo], ["Emisión", reloj.format(new Date(datos.fechaEmision))],
            ["Expiración", reloj.format(new Date(datos.fechaExpiracion))]);
    }
    return detalle(campos);
}

function solicitar() {
    const visitante = sesion.tipoUsuario === "VISITANTE";
    const tipo = visitante ? "temporal" : "ordinaria";
    pantalla("solicitud", "Solicitar autorización", `Confirma los datos de tu solicitud ${tipo}.`,
        `${detalle([["Solicitante", sesion.nombre], ["Placa", sesion.placa], ["Vehículo", sesion.tipoVehiculo]])}
        <form>${visitante ? campo("Motivo de visita", '<textarea name="motivo" rows="3" maxlength="500" required></textarea>') : ""}
        ${boton("Enviar solicitud", 'type="submit"')}</form>`);
    encadenarFormulario("solicitud", datos => {
        pantalla("solicitud", "Solicitud enviada", "Guarda el número para consultar el resultado.",
            `${detalle([["Número de solicitud", datos.id], ["Estado", "PENDIENTE"]])}
            ${boton("Consultar esta solicitud", `type="button" data-ir="consulta" data-id="${datos.id}"`)}`);
    });
}

function buscar(revisar = false, id = "") {
    pantalla(revisar ? "revision" : "consulta", revisar ? "Revisar solicitud" : "Consultar solicitud",
        "Ingresa el número de la solicitud.",
        `<form>${campo("Número de solicitud",
            `<input name="id" inputmode="numeric" pattern="[0-9]+" maxlength="10" value="${escapar(id)}" required>`)}
        ${boton("Buscar", 'type="submit"')}</form>`);
    encadenarFormulario(revisar ? "revision" : "consulta", datos => mostrarSolicitud(datos, revisar));
}

function mostrarSolicitud(datos, revisar, mensaje = "") {
    const pendiente = revisar && datos.estado === "PENDIENTE";
    pantalla(revisar ? "revision" : "consulta", `Solicitud #${datos.id}`,
        revisar ? "Revisa los datos del solicitante y registra tu decisión." : "Estado y datos de la autorización.",
        `${datosSolicitud(datos)}
        ${pendiente ? `<form><input type="hidden" name="id" value="${datos.id}">
            ${campo("Decisión", `<select name="decision" required>
                <option value="">Elige una opción</option><option value="APROBAR">Aprobar</option>
                <option value="CORRECCION">Pedir correcciones</option><option value="RECHAZAR">Rechazar</option></select>`)}
            <div id="motivo" hidden>${campo("Motivo", '<textarea name="motivo" rows="3" maxlength="500"></textarea>')}</div>
            ${boton("Registrar decisión", 'type="submit"')}</form>` : ""}
        <button class="boton boton-secundario" type="button" data-ir="${revisar ? "revision" : "consulta"}">Buscar otra solicitud</button>`);
    if (mensaje) aviso(mensaje, "exito");
    if (pendiente) {
        const decision = app.querySelector('[name="decision"]');
        const motivo = app.querySelector("#motivo");
        decision.addEventListener("change", () => {
            motivo.hidden = !["CORRECCION", "RECHAZAR"].includes(decision.value);
            motivo.querySelector("textarea").required = !motivo.hidden;
        });
        encadenarFormulario("decision", nueva => mostrarSolicitud(nueva, true, "Decisión registrada."));
    }
}

function escanear(esSalida) {
    const operacion = esSalida ? "salida" : "ingreso";
    const actual = pantalla(operacion, esSalida ? "Salir del parqueadero" : "Ingresar al parqueadero",
        `Ingresa la placa y escanea el QR de ${esSalida ? "salida" : "entrada"}.`,
        `<div class="lector-qr">
            ${campo("Placa del vehículo", '<input id="placa-qr" name="placa" autocapitalize="characters" maxlength="20" required>')}
            <video class="video-qr" id="video-qr" playsinline muted hidden aria-label="Vista de la cámara"></video>
            <p id="estado-qr" class="estado-qr" role="status" aria-live="polite"></p>
            <div class="fila-botones">
                ${boton("Abrir cámara", 'type="button" id="abrir-camara"')}
                <button class="boton boton-secundario" type="button" id="elegir-archivo">Usar una foto</button>
                <input type="file" id="foto-qr" accept="image/*" hidden>
            </div>
        </div>`);
    const video = app.querySelector("#video-qr");
    const archivo = app.querySelector("#foto-qr");
    const placa = app.querySelector("#placa-qr");
    const estado = app.querySelector("#estado-qr");
    const camara = app.querySelector("#abrir-camara");
    const elegir = app.querySelector("#elegir-archivo");

    function comprobarPlaca() {
        if (placa.value.trim()) return true;
        aviso("Ingresa la placa del vehículo.", "error");
        placa.focus();
        return false;
    }

    let procesando = false;
    lector = crearLectorQR({
        video, archivo,
        mostrarEstado: texto => { estado.textContent = texto; },
        alLeer: async texto => {
            if (procesando || !comprobarPlaca()) return;
            procesando = true;
            video.hidden = true;
            camara.disabled = true;
            elegir.disabled = true;
            aviso("");
            estado.textContent = "Validando el acceso…";
            try {
                const resultado = await api(operacion, { qr: texto, placa: placa.value.trim() });
                if (actual !== generacion) return;
                const exito = ["ASIGNADO", "SALIDA_REGISTRADA"].includes(resultado.estado);
                if (exito) {
                    pantalla(operacion, esSalida ? "Salida registrada" : "Ingreso autorizado", "",
                        `<p class="aviso exito" role="status">${escapar(resultado.mensaje)}</p>
                        ${resultado.espacio ? detalle([["Espacio asignado", resultado.espacio]]) : ""}`);
                    return;
                }
                aviso(resultado.mensaje, "error");
            } catch (error) {
                if (actual === generacion) mostrarError(error);
            } finally {
                estado.textContent = "";
                camara.disabled = false;
                elegir.disabled = false;
                procesando = false;
            }
        },
    });
    camara.addEventListener("click", async () => {
        if (!comprobarPlaca()) return;
        aviso("");
        video.hidden = false;
        camara.disabled = true;
        await lector.iniciar();
        if (actual === generacion && !video.srcObject) video.hidden = true;
        if (!procesando) camara.disabled = false;
    });
    elegir.addEventListener("click", () => {
        if (!comprobarPlaca()) return;
        aviso("");
        video.hidden = true;
        lector.detener();
        archivo.click();
    });
}

app.addEventListener("click", evento => {
    const boton = evento.target.closest("[data-ir]");
    if (!boton) return;
    switch (boton.dataset.ir) {
        case "inicio": irInicio(); break;
        case "login": acceso(); break;
        case "registro": acceso(true); break;
        case "solicitud": solicitar(); break;
        case "consulta": buscar(false, boton.dataset.id); break;
        case "revision": buscar(true); break;
        case "ingreso": escanear(false); break;
        case "salida": escanear(true); break;
    }
});

document.querySelector("#marca-inicio").addEventListener("click", evento => {
    evento.preventDefault();
    irInicio();
});

cerrar.addEventListener("click", async () => {
    cerrar.disabled = true;
    try {
        sesion = await api("logout", {});
        irInicio("Sesión cerrada.");
    } catch (error) {
        irInicio(error.message);
    } finally {
        cerrar.disabled = false;
    }
});

window.addEventListener("pagehide", () => lector?.detener());
document.addEventListener("visibilitychange", () => {
    if (document.hidden && lector) {
        lector.detener();
        const video = document.querySelector("#video-qr");
        video.hidden = true;
    }
});

try {
    sesion = await api("sesion");
    if (!vista) irInicio();
} catch (error) {
    if (!vista) irInicio(error.message);
}
