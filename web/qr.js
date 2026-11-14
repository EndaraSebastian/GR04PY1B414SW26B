const ANCHO_VIDEO = 720;
const ANCHO_FOTO = 1600;
const INTERVALO_MS = 140;

function cerrarFlujo(flujo) {
    flujo?.getTracks().forEach(pista => pista.stop());
}

function explicarErrorCamara(error) {
    switch (error.name) {
        case "NotAllowedError":
        case "SecurityError":
            return "Permite el acceso a la cámara en el navegador o usa una foto del QR.";
        case "NotFoundError":
            return "No se encontró una cámara. Puedes usar una foto del QR.";
        case "NotReadableError":
            return "La cámara no está disponible. Cierra otras aplicaciones que la usen o elige una foto.";
        default:
            return "No se pudo abrir la cámara. Puedes usar una foto del QR.";
    }
}

async function abrirImagen(archivo) {
    if (typeof createImageBitmap === "function") {
        try {
            const bitmap = await createImageBitmap(archivo);
            return { imagen: bitmap, liberar: () => bitmap.close() };
        } catch {
            // Algunos navegadores aceptan más formatos en un elemento Image.
        }
    }

    const url = URL.createObjectURL(archivo);
    const imagen = new Image();
    try {
        imagen.src = url;
        await imagen.decode();
        return { imagen, liberar: () => URL.revokeObjectURL(url) };
    } catch (error) {
        URL.revokeObjectURL(url);
        throw error;
    }
}

export function crearLectorQR({ video, archivo, mostrarEstado, alLeer }) {
    const lienzo = document.createElement("canvas");
    const contexto = lienzo.getContext("2d", { willReadFrequently: true });
    let flujo;
    let fotograma;
    let numeroLectura = 0;
    let destruido = false;

    video.playsInline = true;
    video.muted = true;
    video.hidden = true;

    function detener() {
        numeroLectura++;
        cancelAnimationFrame(fotograma);
        cerrarFlujo(flujo);
        flujo = undefined;
        video.pause();
        video.srcObject = null;
        video.hidden = true;
    }

    function leerImagen(imagen, ancho, alto, maximo, inversionAttempts) {
        const escala = Math.min(maximo / Math.max(ancho, alto), 1);
        const anchoLectura = Math.max(1, Math.round(ancho * escala));
        const altoLectura = Math.max(1, Math.round(alto * escala));
        if (lienzo.width !== anchoLectura || lienzo.height !== altoLectura) {
            lienzo.width = anchoLectura;
            lienzo.height = altoLectura;
        }
        contexto.drawImage(imagen, 0, 0, anchoLectura, altoLectura);
        const pixeles = contexto.getImageData(0, 0, anchoLectura, altoLectura);
        return globalThis.jsQR(pixeles.data, anchoLectura, altoLectura, { inversionAttempts })?.data;
    }

    function listo() {
        if (destruido) return false;
        if (!contexto || typeof globalThis.jsQR !== "function") {
            mostrarEstado("No se pudo cargar el lector de QR. Vuelve a abrir la página.");
            return false;
        }
        return true;
    }

    function entregar(codigo) {
        detener();
        mostrarEstado("Código QR leído.");
        alLeer(codigo);
    }

    async function iniciar() {
        detener();
        if (!listo()) return;
        if (!globalThis.isSecureContext) {
            mostrarEstado("Para usar la cámara en vivo, abre la aplicación por HTTPS. También puedes escanear una foto.");
            return;
        }
        if (!navigator.mediaDevices?.getUserMedia) {
            mostrarEstado("Este navegador no ofrece cámara en vivo. Puedes escanear una foto del QR.");
            return;
        }

        const lectura = numeroLectura;
        mostrarEstado("Permite el acceso a la cámara para continuar.");

        try {
            const solicitado = await navigator.mediaDevices.getUserMedia({
                audio: false,
                video: {
                    facingMode: { ideal: "environment" },
                    width: { ideal: 1280 },
                    height: { ideal: 720 }
                }
            });
            if (lectura !== numeroLectura) {
                cerrarFlujo(solicitado);
                return;
            }

            flujo = solicitado;
            video.srcObject = flujo;
            video.hidden = false;
            await video.play();
            if (lectura !== numeroLectura) return;
        } catch (error) {
            if (lectura === numeroLectura) {
                detener();
                mostrarEstado(explicarErrorCamara(error));
            }
            return;
        }

        mostrarEstado("Centra el código QR dentro de la cámara.");
        flujo.getVideoTracks().forEach(pista => pista.addEventListener("ended", () => {
            if (lectura === numeroLectura) {
                detener();
                mostrarEstado("La cámara se cerró. Puedes volver a abrirla o escanear una foto.");
            }
        }, { once: true }));

        let ultimo = -INTERVALO_MS;
        function revisarFotograma(tiempo) {
            if (lectura !== numeroLectura) return;
            if (tiempo - ultimo >= INTERVALO_MS && video.readyState >= 2 && video.videoWidth) {
                ultimo = tiempo;
                const codigo = leerImagen(video, video.videoWidth, video.videoHeight, ANCHO_VIDEO, "dontInvert");
                if (codigo) {
                    entregar(codigo);
                    return;
                }
            }
            fotograma = requestAnimationFrame(revisarFotograma);
        }
        fotograma = requestAnimationFrame(revisarFotograma);
    }

    async function leerArchivo() {
        const foto = archivo.files[0];
        archivo.value = "";
        if (!foto || !listo()) return;

        detener();
        const lectura = numeroLectura;
        mostrarEstado("Buscando el código QR en la foto.");
        let codigo;
        try {
            const { imagen, liberar } = await abrirImagen(foto);
            try {
                if (lectura !== numeroLectura) return;
                const ancho = imagen.naturalWidth ?? imagen.width;
                const alto = imagen.naturalHeight ?? imagen.height;
                codigo = leerImagen(imagen, ancho, alto, ANCHO_FOTO, "attemptBoth");
            } finally {
                liberar();
            }
        } catch {
            if (lectura === numeroLectura) {
                mostrarEstado("No se pudo abrir la foto. Elige una imagen JPG o PNG.");
            }
            return;
        }
        if (codigo) {
            entregar(codigo);
        } else {
            mostrarEstado("No se encontró un código QR legible. Prueba otra foto con el código completo y buena luz.");
        }
    }

    archivo.addEventListener("change", leerArchivo);
    return {
        iniciar,
        detener,
        destruir() {
            destruido = true;
            detener();
            archivo.removeEventListener("change", leerArchivo);
        }
    };
}
