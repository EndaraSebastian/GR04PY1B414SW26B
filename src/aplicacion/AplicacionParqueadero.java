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
import negocio.ResultadoIngreso;
import negocio.ServicioAcceso;
import negocio.ServicioAutorizacion;
import negocio.ServicioIngreso;
import negocio.ServicioSalida;

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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public final class AplicacionParqueadero {
    private static final String COOKIE = "parqueadero";
    private static final Duration DURACION_SESION = Duration.ofHours(8);
    private static final int LIMITE_FORMULARIO = 8192;
    private static final Map<String, String> TIPOS = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "webmanifest", "application/manifest+json; charset=utf-8",
            "svg", "image/svg+xml",
            "png", "image/png");
    private static final Set<String> ARCHIVOS_WEB = Set.of(
            "index.html", "app.js", "estilos.css", "qr.js", "vendor/jsQR.js",
            "manifest.webmanifest", "icono.svg", "icono-192.png", "icono-512.png",
            "icono-180.png", "codigos.html", "codigos.css", "qr-entrada.png", "qr-salida.png");

    private record Sesion(Usuario usuario, Instant vence) {}

    private final Path web;
    private final boolean cookieSegura;
    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();

    private final ServicioAcceso acceso;
    private final ServicioAutorizacion autorizacion;
    private final ServicioIngreso ingreso;
    private final ServicioSalida salida;
    private final AutorizacionDAO autorizaciones;
    private final ConstanciaDAO constancias;
    private boolean administradorPreparado;
    private int siguienteSolicitud = 1;

    AplicacionParqueadero(Path web, boolean cookieSegura) {
        this.web = web;
        this.cookieSegura = cookieSegura;

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

    public static void iniciar(String[] args) throws Exception {
        Path web = args.length > 0 ? Path.of(args[0]).toAbsolutePath().normalize() : null;
        if (web == null && recursoWeb(null, "index.html") == null) {
            web = Path.of("web").toAbsolutePath().normalize();
        }
        if (recursoWeb(web, "index.html") == null) {
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
        AplicacionParqueadero app = new AplicacionParqueadero(web, tls || proxyHttps);
        app.prepararAdministrador(clave);

        InetSocketAddress direccion = new InetSocketAddress(host, puerto);
        HttpServer servidor = tls ? crearHttps(direccion, archivoTls) : HttpServer.create(direccion, 0);
        servidor.createContext("/", app::atender);
        servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        servidor.start();
        String ipMostrada = host.contains(":") ? "[" + host + "]" : host;
        System.out.printf("Parqueadero EPN activo en %s://%s:%d%n",
                tls ? "https" : "http", ipMostrada, servidor.getAddress().getPort());
    }

    private static HttpsServer crearHttps(InetSocketAddress direccion, String archivo) throws Exception {
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
            responder(intercambio, 200, datosSesion(usuarioActual(intercambio)));
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
                responder(intercambio, 201, Map.of("mensaje", "Cuenta creada. Ya puedes iniciar sesión."));
                return;
            }
            case "login" -> {
                Usuario usuario = autenticar(datos.get("cedula"), datos.get("clave"));
                if (usuario == null) {
                    responder(intercambio, 401, Map.of("error", "La cédula o la clave son incorrectas."));
                    return;
                }
                crearSesion(intercambio, usuario);
                responder(intercambio, 200, datosSesion(usuario));
                return;
            }
            case "logout" -> {
                cerrarSesion(intercambio);
                responder(intercambio, 200, datosSesion(null));
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
            case "ingreso" -> ingresar(usuario, datos.get("qr"), datos.get("placa"));
            case "salida" -> salir(usuario, datos.get("qr"), datos.get("placa"));
            default -> null;
        };
        if (respuesta == null) {
            responder(intercambio, 404, Map.of("error", "Ruta no disponible."));
            return;
        }
        responder(intercambio, 200, respuesta);
    }

    private static Map<String, String> leerFormulario(HttpExchange intercambio) throws IOException {
        String tipo = intercambio.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !tipo.split(";", 2)[0].trim().equalsIgnoreCase("application/x-www-form-urlencoded")) {
            throw new IllegalArgumentException("Formato de formulario no válido.");
        }
        byte[] bytes = intercambio.getRequestBody().readNBytes(LIMITE_FORMULARIO + 1);
        if (bytes.length > LIMITE_FORMULARIO) {
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

    private static int leerId(Map<String, String> datos) {
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
                if (cookie.startsWith(COOKIE + "=")) {
                    return cookie.substring(COOKIE.length() + 1);
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
        sesiones.put(id, new Sesion(usuario, ahora.plus(DURACION_SESION)));
        enviarCookie(intercambio, id, DURACION_SESION.toSeconds());
    }

    private void cerrarSesion(HttpExchange intercambio) {
        sesiones.remove(leerCookie(intercambio));
        enviarCookie(intercambio, "", 0);
    }

    private void enviarCookie(HttpExchange intercambio, String id, long segundos) {
        intercambio.getResponseHeaders().set("Set-Cookie", COOKIE + "=" + id + "; Path=/; "
                + "HttpOnly; SameSite=Strict; Max-Age=" + segundos + (cookieSegura ? "; Secure" : ""));
    }

    private static Map<String, Object> datosSesion(Usuario usuario) {
        if (usuario == null) {
            return Map.of("autenticado", false);
        }
        return Map.of("autenticado", true, "nombre", usuario.getNombre(), "cedula", usuario.getCedula(),
                "tipoUsuario", usuario.getTipoUsuario(), "placa", usuario.getPlaca(),
                "tipoVehiculo", usuario.getTipoVehiculo());
    }

    private void estatico(HttpExchange intercambio, String ruta) throws IOException {
        if (!"GET".equals(intercambio.getRequestMethod()) && !"HEAD".equals(intercambio.getRequestMethod())) {
            intercambio.getResponseHeaders().set("Allow", "GET, HEAD");
            responder(intercambio, 405, Map.of("error", "Método no disponible."));
            return;
        }
        String nombre = "/".equals(ruta) ? "index.html" : ruta.substring(1);
        if (!ARCHIVOS_WEB.contains(nombre)) {
            responder(intercambio, 404, Map.of("error", "Archivo no disponible."));
            return;
        }
        URL archivo = recursoWeb(web, nombre);
        int punto = nombre.lastIndexOf('.');
        String tipo = punto < 0 ? null : TIPOS.get(nombre.substring(punto + 1));
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

    private static URL recursoWeb(Path web, String nombre) throws IOException {
        if (web == null) {
            return AplicacionParqueadero.class.getResource("/web/" + nombre);
        }
        Path archivo = web.resolve(nombre);
        return Files.isRegularFile(archivo) ? archivo.toUri().toURL() : null;
    }

    private static void responder(HttpExchange intercambio, int estado, Map<String, ?> datos) throws IOException {
        byte[] respuesta = datos.entrySet().stream()
                .map(par -> json(par.getKey()) + ":" + json(par.getValue()))
                .collect(Collectors.joining(",", "{", "}")).getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        intercambio.sendResponseHeaders(estado, respuesta.length);
        intercambio.getResponseBody().write(respuesta);
    }

    private static String json(Object dato) {
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
    synchronized void prepararAdministrador(String clave) {
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

    synchronized void registrar(Map<String, String> datos) {
        String cedula = requerido(datos.get("cedula"), "Ingrese su cédula.");
        if ("ADMIN".equalsIgnoreCase(cedula)) {
            throw new IllegalArgumentException("Ese identificador está reservado para el administrador.");
        }
        String nombre = requerido(datos.get("nombre"), "Ingrese su nombre.");
        String tipoUsuario = requerido(datos.get("tipoUsuario"), "Seleccione MCP o VISITANTE.")
                .toUpperCase(Locale.ROOT);
        if (!"MCP".equals(tipoUsuario) && !"VISITANTE".equals(tipoUsuario)) {
            throw new IllegalArgumentException("El tipo de usuario debe ser MCP o VISITANTE.");
        }
        String placa = requerido(datos.get("placa"), "Ingrese la placa.").toUpperCase(Locale.ROOT);
        String tipoVehiculo = requerido(datos.get("tipoVehiculo"), "Seleccione MOTO o AUTOMOVIL.")
                .toUpperCase(Locale.ROOT);
        if (!"MOTO".equals(tipoVehiculo) && !"AUTOMOVIL".equals(tipoVehiculo)) {
            throw new IllegalArgumentException("El tipo de vehículo debe ser MOTO o AUTOMOVIL.");
        }

        String clave = datos.get("clave");
        validarClave(clave);
        Usuario usuario = new Usuario(cedula, nombre, tipoUsuario, placa, tipoVehiculo);
        if (!acceso.registrarUsuario(usuario, clave)) {
            throw new IllegalArgumentException("La cédula ya está registrada.");
        }
    }

    synchronized Usuario autenticar(String cedula, String clave) {
        String identificador = requerido(cedula, "Ingrese su cédula o el identificador ADMIN.");
        if ("ADMIN".equalsIgnoreCase(identificador)) {
            identificador = "ADMIN";
        }

        //La sesión del servicio es transitoria. El navegador conserva su propia sesión HTTP.
        try {
            return acceso.iniciarSesion(identificador, clave);
        } finally {
            acceso.cerrarSesion();
        }
    }

    synchronized Map<String, Object> solicitar(Usuario usuario, String motivo) {
        exigirUsuario(usuario);
        boolean visitante = "VISITANTE".equals(usuario.getTipoUsuario());
        String tipo = visitante ? "TEMPORAL" : "ORDINARIA";
        String motivoVisita = visitante ? requerido(motivo, "Ingrese el motivo de su visita.") : "";
        Solicitud solicitud = new Solicitud(siguienteSolicitud, usuario, tipo, motivoVisita);
        autorizacion.registrarSolicitud(solicitud);
        siguienteSolicitud++;
        return Map.of("id", solicitud.getIdSolicitud());
    }

    synchronized Map<String, Object> consultar(Usuario usuario, int id) {
        exigirSesion(usuario);
        Solicitud solicitud = buscarSolicitud(id);
        if (!esAdministrador(usuario) && !usuario.getCedula().equals(solicitud.getIdentificacion())) {
            throw new IllegalStateException("Esta solicitud pertenece a otro usuario.");
        }
        return detalle(solicitud);
    }

    synchronized Map<String, Object> consultarRevision(Usuario usuario, int id) {
        exigirAdministrador(usuario);
        return detalle(buscarSolicitud(id));
    }

    synchronized Map<String, Object> revisar(Usuario usuario, int id, String decision, String motivo) {
        exigirAdministrador(usuario);
        Solicitud solicitud = buscarSolicitud(id);
        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new IllegalStateException("La solicitud ya fue revisada.");
        }

        switch (requerido(decision, "Seleccione una decisión.")) {
            case "APROBAR" -> autorizacion.emitirAutorizacion(solicitud);
            case "CORRECCION" -> autorizacion.solicitarCorreccion(solicitud,
                    requerido(motivo, "Ingrese el motivo de la corrección."));
            case "RECHAZAR" -> autorizacion.rechazarSolicitud(solicitud,
                    requerido(motivo, "Ingrese el motivo del rechazo."));
            default -> throw new IllegalArgumentException("Seleccione una decisión válida.");
        }
        return detalle(solicitud);
    }

    synchronized Map<String, Object> ingresar(Usuario usuario, String qr, String placa) {
        exigirUsuario(usuario);
        if ("QR_ENTRADA".equals(qr)) {
            Autorizacion permiso = autorizaciones.buscarVigentePorUsuario(usuario.getCedula(), new Date());
            if (permiso != null && !coincidePlaca(permiso.getSolicitud().getPlaca(), placa)) {
                return Map.of("estado", "PLACA_NO_COINCIDE",
                        "mensaje", "La placa no coincide con la de la autorización.");
            }
        }
        ResultadoIngreso resultado = ingreso.procesarIngreso(usuario, qr);
        String estado = resultado.estado.name();

        return switch (resultado.estado) {
            case ASIGNADO -> Map.of("estado", estado,
                    "espacio", resultado.constancia.getEspacio().getIdEspacio(),
                    "mensaje", "Ingreso registrado.");
            case QR_INVALIDO -> Map.of("estado", estado, "mensaje", "Código QR de entrada inválido.");
            case SIN_AUTORIZACION -> Map.of("estado", estado, "mensaje", "El usuario no tiene una autorización vigente.");
            case INGRESO_ACTIVO -> Map.of("estado", estado, "mensaje", "El usuario ya tiene un ingreso activo.");
            case SIN_CUPO -> Map.of("estado", estado, "mensaje", "No hay espacios libres.");
        };
    }

    synchronized Map<String, Object> salir(Usuario usuario, String qr, String placa) {
        exigirUsuario(usuario);
        if (!"QR_SALIDA".equals(qr)) {
            return Map.of("estado", "QR_INVALIDO", "mensaje", "Código QR de salida inválido.");
        }
        Constancia constancia = constancias.buscarActiva(usuario.getCedula());
        if (constancia != null && !coincidePlaca(constancia.getUsuario().getPlaca(), placa)) {
            return Map.of("estado", "PLACA_NO_COINCIDE", "mensaje", "La placa no coincide con la del ingreso.");
        }
        if (salida.procesarSalida(usuario)) {
            return Map.of("estado", "SALIDA_REGISTRADA", "mensaje", "Salida registrada. Se liberó su espacio.");
        }
        return Map.of("estado", "SIN_INGRESO_ACTIVO", "mensaje", "No existe un ingreso activo.");
    }

    private static boolean coincidePlaca(String placaRegistrada, String placaLeida) {
        return placaLeida != null && placaRegistrada.strip().equalsIgnoreCase(placaLeida.strip());
    }

    private Solicitud buscarSolicitud(int id) {
        Solicitud solicitud = autorizacion.consultarSolicitud(id);
        if (solicitud == null) {
            throw new IllegalArgumentException("No existe una solicitud con ese número.");
        }
        return solicitud;
    }

    private Map<String, Object> detalle(Solicitud solicitud) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", solicitud.getIdSolicitud());
        datos.put("tipo", solicitud.getTipoSolicitud());
        datos.put("estado", solicitud.getEstado());
        datos.put("observaciones", solicitud.getObservaciones());
        datos.put("nombre", solicitud.getNombreSolicitante());
        datos.put("cedula", solicitud.getIdentificacion());
        datos.put("placa", solicitud.getPlaca());
        datos.put("tipoVehiculo", solicitud.getTipoVehiculo());
        datos.put("motivo", solicitud.getMotivoVisita());

        Autorizacion permiso = autorizacion.consultarAutorizacion(solicitud.getIdSolicitud());
        if (permiso != null) {
            datos.put("codigo", permiso.getCodigo());
            datos.put("fechaEmision", permiso.getFechaEmision().toInstant().toString());
            datos.put("fechaExpiracion", permiso.getFechaFin().toInstant().toString());
        }
        return datos;
    }

    private static boolean esAdministrador(Usuario usuario) {
        return "ADMIN".equals(usuario.getTipoUsuario());
    }

    private static void exigirSesion(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalStateException("Inicie sesión para continuar.");
        }
    }

    private static void exigirAdministrador(Usuario usuario) {
        exigirSesion(usuario);
        if (!esAdministrador(usuario)) {
            throw new IllegalStateException("Solo el administrador puede revisar solicitudes.");
        }
    }

    private static void exigirUsuario(Usuario usuario) {
        exigirSesion(usuario);
        if (esAdministrador(usuario)) {
            throw new IllegalStateException("Ingrese como miembro o visitante para usar esta opción.");
        }
    }

    private static void validarClave(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("Ingrese una clave.");
        }
    }

    private static String requerido(String texto, String mensaje) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return texto.strip();
    }
}
