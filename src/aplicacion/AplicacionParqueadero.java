package aplicacion;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import datos.AutorizacionDAO;
import datos.ConstanciaDAO;
import datos.EspacioDAO;
import datos.SolicitudDAO;
import datos.UsuarioDAO;
import entidades.Autorizacion;
import entidades.Constancia;
import entidades.Solicitud;
import entidades.Usuario;
import negocio.ServicioAcceso;
import negocio.ServicioAutorizacion;
import negocio.ServicioIngreso;
import negocio.ServicioSalida;
import presentacion.UI_Asignacion;
import presentacion.UI_FormularioSolicitud;
import presentacion.UI_Home;
import presentacion.UI_LecturaQR;
import presentacion.UI_Login;
import presentacion.UI_ResultadoAutorizacion;
import presentacion.UI_ResultadoSalida;
import presentacion.UI_RevisionSolicitud;
import presentacion.UI_SignUp;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public final class AplicacionParqueadero {
    private final String nombreCookie = "parqueadero";
    private final Duration duracionSesion = Duration.ofHours(8);
    private final int limiteFormulario = 8192;
    private final Map<String, String> tipos = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "webmanifest", "application/manifest+json; charset=utf-8",
            "svg", "image/svg+xml",
            "png", "image/png");
    private final Set<String> archivosWeb = Set.of(
            "index.html", "app.js", "estilos.css", "qr.js", "vendor/jsQR.js",
            "manifest.webmanifest", "icono.svg", "icono-192.png", "icono-512.png",
            "icono-180.png", "codigos.html", "codigos.css", "qr-entrada.png", "qr-salida.png");

    private record Sesion(Usuario usuario, Instant vence) {}

    private Path web;
    private boolean cookieSegura;
    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final UI_Home inicio = new UI_Home();
    private final UI_Login login = new UI_Login();
    private final UI_SignUp registro = new UI_SignUp();
    private final UI_FormularioSolicitud formulario = new UI_FormularioSolicitud();
    private final UI_RevisionSolicitud revision = new UI_RevisionSolicitud();
    private final UI_ResultadoAutorizacion resultadoAutorizacion = new UI_ResultadoAutorizacion();
    private final UI_LecturaQR lectorQR = new UI_LecturaQR();
    private final UI_Asignacion asignacion = new UI_Asignacion();
    private final UI_ResultadoSalida resultadoSalida = new UI_ResultadoSalida();

    private final ServicioAcceso acceso;
    private final ServicioAutorizacion autorizacion;
    private final ServicioIngreso ingreso;
    private final ServicioSalida salida;
    private final AutorizacionDAO autorizaciones;
    private final ConstanciaDAO constancias;
    private boolean administradorPreparado;
    private int siguienteSolicitud = 1;

    public AplicacionParqueadero() {
        UsuarioDAO usuarios = new UsuarioDAO();
        SolicitudDAO solicitudes = new SolicitudDAO();
        autorizaciones = new AutorizacionDAO();
        constancias = new ConstanciaDAO();
        EspacioDAO espacios = new EspacioDAO(3);

        acceso = new ServicioAcceso(usuarios);
        autorizacion = new ServicioAutorizacion(usuarios, solicitudes, autorizaciones);
        ingreso = new ServicioIngreso(autorizaciones, constancias, espacios);
        salida = new ServicioSalida(constancias, espacios);
    }

    public synchronized void iniciar(String[] args) throws Exception {
        if (administradorPreparado) {
            throw new IllegalStateException("La aplicación ya fue iniciada.");
        }
        Path carpetaWeb = args.length > 0 ? Path.of(args[0]).toAbsolutePath().normalize() : null;
        if (carpetaWeb == null && recursoWeb(null, "index.html") == null) {
            carpetaWeb = Path.of("web").toAbsolutePath().normalize();
        }
        if (recursoWeb(carpetaWeb, "index.html") == null) {
            throw new IOException("No se encontro web/index.html.");
        }
        String archivoTls = System.getenv("PARQUEADERO_TLS_ARCHIVO");
        boolean tls = archivoTls != null && !archivoTls.isBlank();
        String host = System.getenv().getOrDefault("PARQUEADERO_HOST", "127.0.0.1");
        if (!tls && !host.equals("127.0.0.1") && !host.equals("localhost") && !host.equals("::1")) {
            throw new IllegalArgumentException("Para escuchar en la red se requiere HTTPS.");
        }
        int puerto = Integer.parseInt(System.getenv().getOrDefault("PARQUEADERO_PUERTO", tls ? "8443" : "8080"));

        String clave = System.getenv("PARQUEADERO_ADMIN_CLAVE");
        if (clave == null && System.console() != null) {
            char[] entrada = System.console().readPassword("Crea la clave de ADMIN para esta ejecucion: ");
            if (entrada != null) {
                clave = new String(entrada);
                Arrays.fill(entrada, '\0');
            }
        }
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("Crea la clave de ADMIN en la terminal o configura PARQUEADERO_ADMIN_CLAVE.");
        }
        boolean proxyHttps = "true".equalsIgnoreCase(System.getenv("PARQUEADERO_PROXY_HTTPS"));

        InetSocketAddress direccion = new InetSocketAddress(host, puerto);
        HttpServer servidor = tls ? crearHttps(direccion, archivoTls) : HttpServer.create(direccion, 0);
        var ejecutor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            web = carpetaWeb;
            cookieSegura = tls || proxyHttps;
            servidor.createContext("/", this::atender);
            servidor.setExecutor(ejecutor);
            prepararAdministrador(clave);
            servidor.start();
        } catch (RuntimeException | Error fallo) {
            servidor.stop(0);
            ejecutor.shutdown();
            throw fallo;
        }
        String ipMostrada = host.contains(":") ? "[" + host + "]" : host;
        System.out.printf("Parqueadero EPN activo en %s://%s:%d%n",
                tls ? "https" : "http", ipMostrada, servidor.getAddress().getPort());
    }

    private HttpsServer crearHttps(InetSocketAddress direccion, String archivo) throws Exception {
        String clave = System.getenv("PARQUEADERO_TLS_CLAVE");
        if (clave == null) {
            throw new IllegalArgumentException("Configura PARQUEADERO_TLS_CLAVE para el archivo PKCS12.");
        }
        char[] secreto = clave.toCharArray();
        KeyManagerFactory gestores = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        try (var entrada = Files.newInputStream(Path.of(archivo))) {
            KeyStore almacen = KeyStore.getInstance("PKCS12");
            almacen.load(entrada, secreto);
            gestores.init(almacen, secreto);
        } finally {
            Arrays.fill(secreto, '\0');
        }
        SSLContext contexto = SSLContext.getInstance("TLS");
        contexto.init(gestores.getKeyManagers(), null, null);
        HttpsServer servidor = HttpsServer.create(direccion, 0);
        servidor.setHttpsConfigurator(new HttpsConfigurator(contexto));
        return servidor;
    }

    private void atender(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            var cabeceras = intercambio.getResponseHeaders();
            cabeceras.set("X-Content-Type-Options", "nosniff");
            cabeceras.set("Referrer-Policy", "no-referrer");
            cabeceras.set("Permissions-Policy", "camera=(self), microphone=()");
            cabeceras.set("Content-Security-Policy", "default-src 'self'; img-src 'self' blob:; "
                    + "media-src 'self' blob:; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
            String ruta = intercambio.getRequestURI().getPath();
            try {
                if (ruta.startsWith("/api/")) {
                    cabeceras.set("Cache-Control", "no-store");
                    api(intercambio, ruta.substring(5));
                } else {
                    estatico(intercambio, ruta);
                }
            } catch (IllegalArgumentException | IllegalStateException error) {
                responder(intercambio, 400, Map.of("error", error.getMessage()));
            }
        }
    }

    private void api(HttpExchange intercambio, String recurso) throws IOException {
        String metodo = intercambio.getRequestMethod();
        if ("GET".equals(metodo) && "sesion".equals(recurso)) {
            responder(intercambio, 200, inicio.mostrarMenu(usuarioActual(intercambio)));
            return;
        }
        if (!"POST".equals(metodo)) {
            intercambio.getResponseHeaders().set("Allow", "GET, POST");
            responder(intercambio, 405, Map.of("error", "Método no disponible."));
            return;
        }
        // Las solicitudes de formularios de otros sitios no pueden enviar este encabezado.
        if (!"1".equals(intercambio.getRequestHeaders().getFirst("X-Parqueadero"))) {
            responder(intercambio, 403, Map.of("error", "Solicitud no permitida."));
            return;
        }
        Map<String, String> datos = leerFormulario(intercambio);
        switch (recurso) {
            case "registro" -> {
                registrar(datos);
                responder(intercambio, 201, registro.mostrarResultado());
                return;
            }
            case "login" -> {
                Usuario usuario = autenticar(datos.get("cedula"), datos.get("clave"));
                if (usuario == null) {
                    responder(intercambio, 401, login.mostrarError());
                    return;
                }
                crearSesion(intercambio, usuario);
                responder(intercambio, 200, inicio.mostrarMenu(usuario));
                return;
            }
            case "logout" -> {
                cerrarSesion(intercambio);
                responder(intercambio, 200, inicio.mostrarMenu(null));
                return;
            }
            default -> { }
        }
        Usuario usuario = usuarioActual(intercambio);
        if (usuario == null) {
            responder(intercambio, 401, Map.of("error", "Primero inicia sesión."));
            return;
        }
        Map<String, Object> respuesta = switch (recurso) {
            case "solicitud" -> solicitar(usuario, datos.get("motivo"));
            case "consulta" -> consultar(usuario, leerId(datos));
            case "revision" -> consultarRevision(usuario, leerId(datos));
            case "decision" -> revisar(usuario, leerId(datos), datos.get("decision"), datos.get("motivo"));
            case "ingreso" -> ingresar(usuario, lectorQR.leerQR(datos), datos.get("placa"));
            case "salida" -> salir(usuario, lectorQR.leerQR(datos), datos.get("placa"));
            default -> null;
        };
        if (respuesta == null) {
            responder(intercambio, 404, Map.of("error", "Ruta no disponible."));
            return;
        }
        responder(intercambio, 200, respuesta);
    }

    private Map<String, String> leerFormulario(HttpExchange intercambio) throws IOException {
        String tipo = intercambio.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !tipo.split(";", 2)[0].trim().equalsIgnoreCase("application/x-www-form-urlencoded")) {
            throw new IllegalArgumentException("Formato de formulario no válido.");
        }
        byte[] bytes = intercambio.getRequestBody().readNBytes(limiteFormulario + 1);
        if (bytes.length > limiteFormulario) {
            throw new IllegalArgumentException("El formulario es demasiado largo.");
        }
        Map<String, String> datos = new HashMap<>();
        for (String campo : new String(bytes, StandardCharsets.UTF_8).split("&")) {
            String[] par = campo.split("=", 2);
            if (par.length == 2) {
                datos.put(URLDecoder.decode(par[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(par[1], StandardCharsets.UTF_8));
            }
        }
        return datos;
    }

    private int leerId(Map<String, String> datos) {
        try {
            int id = Integer.parseInt(datos.getOrDefault("id", ""));
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignorado) { }
        throw new IllegalArgumentException("Ingresa un número de solicitud válido.");
    }

    private String leerCookie(HttpExchange intercambio) {
        String cabecera = intercambio.getRequestHeaders().getFirst("Cookie");
        if (cabecera != null) {
            for (String parte : cabecera.split(";")) {
                String cookie = parte.trim();
                if (cookie.startsWith(nombreCookie + "=")) {
                    return cookie.substring(nombreCookie.length() + 1);
                }
            }
        }
        return "";
    }

    private Usuario usuarioActual(HttpExchange intercambio) {
        String id = leerCookie(intercambio);
        Sesion sesion = sesiones.get(id);
        if (sesion == null) {
            return null;
        }
        if (!Instant.now().isBefore(sesion.vence())) {
            sesiones.remove(id, sesion);
            return null;
        }
        return sesion.usuario();
    }

    private void crearSesion(HttpExchange intercambio, Usuario usuario) {
        Instant ahora = Instant.now();
        sesiones.remove(leerCookie(intercambio));
        sesiones.values().removeIf(sesion -> !ahora.isBefore(sesion.vence()));
        String id = UUID.randomUUID().toString();
        sesiones.put(id, new Sesion(usuario, ahora.plus(duracionSesion)));
        enviarCookie(intercambio, id, duracionSesion.toSeconds());
    }

    private void cerrarSesion(HttpExchange intercambio) {
        sesiones.remove(leerCookie(intercambio));
        enviarCookie(intercambio, "", 0);
    }

    private void enviarCookie(HttpExchange intercambio, String id, long segundos) {
        intercambio.getResponseHeaders().set("Set-Cookie", nombreCookie + "=" + id + "; Path=/; "
                + "HttpOnly; SameSite=Strict; Max-Age=" + segundos + (cookieSegura ? "; Secure" : ""));
    }

    private void estatico(HttpExchange intercambio, String ruta) throws IOException {
        if (!"GET".equals(intercambio.getRequestMethod()) && !"HEAD".equals(intercambio.getRequestMethod())) {
            intercambio.getResponseHeaders().set("Allow", "GET, HEAD");
            responder(intercambio, 405, Map.of("error", "Método no disponible."));
            return;
        }
        String nombre = "/".equals(ruta) ? "index.html" : ruta.substring(1);
        if (!archivosWeb.contains(nombre)) {
            responder(intercambio, 404, Map.of("error", "Archivo no disponible."));
            return;
        }
        URL archivo = recursoWeb(web, nombre);
        int punto = nombre.lastIndexOf('.');
        String tipo = punto < 0 ? null : tipos.get(nombre.substring(punto + 1));
        if (tipo == null || archivo == null) {
            responder(intercambio, 404, Map.of("error", "Archivo no disponible."));
            return;
        }
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.getResponseHeaders().set("Cache-Control", "no-cache");
        var recurso = archivo.openConnection();
        long longitud = recurso.getContentLengthLong();
        if ("HEAD".equals(intercambio.getRequestMethod())) {
            if (longitud >= 0) {
                intercambio.getResponseHeaders().set("Content-Length", Long.toString(longitud));
            }
            intercambio.sendResponseHeaders(200, -1);
        } else {
            try (var contenido = recurso.getInputStream()) {
                intercambio.sendResponseHeaders(200, longitud > 0 ? longitud : 0);
                contenido.transferTo(intercambio.getResponseBody());
            }
        }
    }

    private URL recursoWeb(Path web, String nombre) throws IOException {
        if (web == null) {
            return AplicacionParqueadero.class.getResource("/web/" + nombre);
        }
        Path archivo = web.resolve(nombre);
        return Files.isRegularFile(archivo) ? archivo.toUri().toURL() : null;
    }

    private void responder(HttpExchange intercambio, int estado, Map<String, ?> datos) throws IOException {
        byte[] respuesta = datos.entrySet().stream()
                .map(par -> json(par.getKey()) + ":" + json(par.getValue()))
                .collect(Collectors.joining(",", "{", "}")).getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        intercambio.sendResponseHeaders(estado, respuesta.length);
        intercambio.getResponseBody().write(respuesta);
    }

    private String json(Object dato) {
        if (dato == null || dato instanceof Boolean || dato instanceof Number) {
            return String.valueOf(dato);
        }
        StringBuilder texto = new StringBuilder("\"");
        for (char caracter : dato.toString().toCharArray()) {
            if (caracter == '"' || caracter == '\\') {
                texto.append('\\').append(caracter);
            } else if (caracter < ' ') {
                texto.append(String.format("\\u%04x", (int) caracter));
            } else {
                texto.append(caracter);
            }
        }
        return texto.append('"').toString();
    }

    // Los servicios comparten sus listas entre las sesiones de los navegadores.
    private void prepararAdministrador(String clave) {
        if (administradorPreparado) {
            throw new IllegalStateException("El administrador ya fue configurado.");
        }

        validarClave(clave);
        Usuario administrador = new Usuario("ADMIN", "Administrador", "ADMIN", "", "");
        if (!acceso.registrarUsuario(administrador, clave)) {
            throw new IllegalStateException("No se pudo configurar al administrador.");
        }
        administradorPreparado = true;
    }

    public synchronized void registrar(Map<String, String> datos) {
        Usuario usuario = registro.capturarDatosRegistro(datos);
        String clave = registro.capturarClave(datos);
        if (!acceso.registrarUsuario(usuario, clave)) {
            throw new IllegalArgumentException("La cédula ya está registrada.");
        }
    }

    public synchronized Usuario autenticar(String cedula, String clave) {
        String identificador = login.capturarCedula(cedula);

        //La sesión del servicio es transitoria. El navegador conserva su propia sesión HTTP.
        try {
            return acceso.iniciarSesion(identificador, clave);
        } finally {
            acceso.cerrarSesion();
        }
    }

    public synchronized Map<String, Object> solicitar(Usuario usuario, String motivo) {
        exigirUsuario(usuario);
        Solicitud solicitud = formulario.recibirDatosSolicitud(usuario, siguienteSolicitud, motivo);
        autorizacion.registrarSolicitud(solicitud);
        siguienteSolicitud++;
        return formulario.mostrarConfirmacion(solicitud);
    }

    public synchronized Map<String, Object> consultar(Usuario usuario, int id) {
        exigirSesion(usuario);
        Solicitud solicitud = buscarSolicitud(id);
        if (!esAdministrador(usuario) && !usuario.getCedula().equals(solicitud.getIdentificacion())) {
            throw new IllegalStateException("Esta solicitud pertenece a otro usuario.");
        }
        return detalle(solicitud);
    }

    public synchronized Map<String, Object> consultarRevision(Usuario usuario, int id) {
        exigirAdministrador(usuario);
        return detalle(buscarSolicitud(id));
    }

    public synchronized Map<String, Object> revisar(Usuario usuario, int id, String decision, String motivo) {
        exigirAdministrador(usuario);
        Solicitud solicitud = buscarSolicitud(id);
        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new IllegalStateException("La solicitud ya fue revisada.");
        }

        switch (revision.registrarDecision(decision)) {
            case "APROBAR" -> autorizacion.emitirAutorizacion(solicitud);
            case "CORRECCION" -> autorizacion.solicitarCorreccion(solicitud,
                    revision.capturarMotivo(motivo, "Ingrese el motivo de la corrección."));
            case "RECHAZAR" -> autorizacion.rechazarSolicitud(solicitud,
                    revision.capturarMotivo(motivo, "Ingrese el motivo del rechazo."));
            default -> throw new IllegalArgumentException("Seleccione una decisión válida.");
        }
        return detalle(solicitud);
    }

    public synchronized Map<String, Object> ingresar(Usuario usuario, String qr, String placa) {
        exigirUsuario(usuario);
        if ("QR_ENTRADA".equals(qr)) {
            Autorizacion permiso = autorizaciones.buscarVigentePorUsuario(usuario.getCedula(), new Date());
            if (permiso != null && !coincidePlaca(permiso.getSolicitud().getPlaca(), placa)) {
                return asignacion.mostrarRechazo("PLACA_NO_COINCIDE", "La placa no coincide con la de la autorización.");
            }
        }
        return asignacion.mostrarResultado(ingreso.procesarIngreso(usuario, qr));
    }

    public synchronized Map<String, Object> salir(Usuario usuario, String qr, String placa) {
        exigirUsuario(usuario);
        if (!"QR_SALIDA".equals(qr)) {
            return lectorQR.mostrarError();
        }
        Constancia constancia = constancias.buscarActiva(usuario.getCedula());
        if (constancia != null && !coincidePlaca(constancia.getUsuario().getPlaca(), placa)) {
            return resultadoSalida.mostrarError("PLACA_NO_COINCIDE", "La placa no coincide con la del ingreso.");
        }
        if (salida.procesarSalida(usuario)) {
            return resultadoSalida.mostrarSalidaExitosa();
        }
        return resultadoSalida.mostrarError("SIN_INGRESO_ACTIVO", "No existe un ingreso activo.");
    }

    public boolean coincidePlaca(String placaRegistrada, String placaLeida) {
        return placaLeida != null && placaRegistrada.strip().equalsIgnoreCase(placaLeida.strip());
    }

    public Solicitud buscarSolicitud(int id) {
        Solicitud solicitud = autorizacion.consultarSolicitud(id);
        if (solicitud == null) {
            throw new IllegalArgumentException("No existe una solicitud con ese número.");
        }
        return solicitud;
    }

    public Map<String, Object> detalle(Solicitud solicitud) {
        Autorizacion permiso = autorizacion.consultarAutorizacion(solicitud.getIdSolicitud());
        return resultadoAutorizacion.mostrarResultado(solicitud, permiso);
    }

    public boolean esAdministrador(Usuario usuario) {
        return "ADMIN".equals(usuario.getTipoUsuario());
    }

    public void exigirSesion(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalStateException("Inicie sesión para continuar.");
        }
    }

    public void exigirAdministrador(Usuario usuario) {
        exigirSesion(usuario);
        if (!esAdministrador(usuario)) {
            throw new IllegalStateException("Solo el administrador puede revisar solicitudes.");
        }
    }

    public void exigirUsuario(Usuario usuario) {
        exigirSesion(usuario);
        if (esAdministrador(usuario)) {
            throw new IllegalStateException("Ingrese como miembro o visitante para usar esta opción.");
        }
    }

    public void validarClave(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("Ingrese una clave.");
        }
    }

}
