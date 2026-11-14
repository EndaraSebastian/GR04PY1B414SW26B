package aplicacion;

import datos.AutorizacionDAO;
import datos.ConstanciaDAO;
import datos.EspacioDAO;
import datos.SolicitudDAO;
import datos.UsuarioDAO;
import entidades.Autorizacion;
import entidades.Solicitud;
import entidades.Usuario;
import negocio.ResultadoIngreso;
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
import java.util.NoSuchElementException;
import java.util.Scanner;

//La aplicacion conecta las vistas con los servicios de cada caso de uso.
public class AplicacionParqueadero {

    private final Scanner entrada = new Scanner(System.in);
    private final ServicioAcceso acceso;
    private final ServicioAutorizacion autorizacion;
    private final ServicioIngreso ingreso;
    private final ServicioSalida salida;
    private int siguienteSolicitud = 1;

    public AplicacionParqueadero() {
        UsuarioDAO usuarios = new UsuarioDAO();
        SolicitudDAO solicitudes = new SolicitudDAO();
        AutorizacionDAO autorizaciones = new AutorizacionDAO();
        ConstanciaDAO constancias = new ConstanciaDAO();
        EspacioDAO espacios = new EspacioDAO(3);

        //Acceso y autorizacion consultan los mismos usuarios registrados.
        acceso = new ServicioAcceso(usuarios);
        autorizacion = new ServicioAutorizacion(usuarios, solicitudes, autorizaciones);

        //Ingreso consulta las autorizaciones emitidas por el servicio de autorizacion.
        ingreso = new ServicioIngreso(autorizaciones, constancias, espacios);

        //Salida recupera las mismas constancias y espacios que utilizo ingreso.
        salida = new ServicioSalida(constancias, espacios);
    }

    public void iniciar() {
        System.out.println("Parqueadero EPN iniciado.");

        try {
            prepararAdministrador();
            UI_Home menu = new UI_Home(entrada);

            while (true) {
                Usuario actual = acceso.obtenerUsuarioActual();
                if (actual == null) {
                    System.out.println("\nSesion sin iniciar.");
                } else {
                    System.out.printf("%nSesion de %s, %s.%n",
                            actual.getNombre(), actual.getTipoUsuario());
                }

                String opcion = menu.mostrarMenu();
                try {
                    switch (opcion) {
                        case "1":
                            abrirAcceso();
                            break;
                        case "2":
                            abrirSolicitud();
                            break;
                        case "3":
                            abrirRevision();
                            break;
                        case "4":
                            abrirConsultaResultado();
                            break;
                        case "5":
                            abrirIngreso();
                            break;
                        case "6":
                            abrirSalida();
                            break;
                        case "7":
                            acceso.cerrarSesion();
                            System.out.println("Sesion cerrada.");
                            break;
                        case "0":
                            acceso.cerrarSesion();
                            System.out.println("Programa terminado.");
                            return;
                        default:
                            System.out.println("Seleccione una opcion del menu.");
                    }
                } catch (IllegalArgumentException | IllegalStateException e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada.");
        }
    }

    //El administrador revisa solicitudes sin registrarse como visitante o miembro.
    private void prepararAdministrador() {
        System.out.println("Cree una clave de prueba para el administrador de esta ejecucion.");
        System.out.println("Su identificador para iniciar sesion es ADMIN.");

        String clave = entrada.nextLine();
        while (clave.isBlank()) {
            System.out.println("Ingrese una clave de administrador.");
            clave = entrada.nextLine();
        }

        Usuario administrador = new Usuario(
                "ADMIN", "Administrador", "ADMIN", "", "");

        if (!acceso.registrarUsuario(administrador, clave)) {
            throw new IllegalStateException("No se pudo preparar el administrador.");
        }
    }

    private Usuario requerirSesion() {
        Usuario usuario = acceso.obtenerUsuarioActual();

        if (usuario == null) {
            throw new IllegalStateException("Primero inicie sesion desde la opcion 1.");
        }

        return usuario;
    }

    private void abrirAcceso() {
        System.out.println("1. Registrar usuario.");
        System.out.println("2. Iniciar sesion.");
        String opcion = entrada.nextLine().trim();

        if ("1".equals(opcion)) {
            UI_SignUp vista = new UI_SignUp(entrada);
            Usuario usuario = vista.capturarDatosRegistro();
            String clave = vista.capturarClave();

            if (acceso.registrarUsuario(usuario, clave)) {
                vista.mostrarResultado("Usuario registrado. Ya puede iniciar sesion.");
            } else {
                vista.mostrarResultado("Revise la clave o la cedula. No se pudo registrar el usuario.");
            }
            return;
        }

        if ("2".equals(opcion)) {
            UI_Login vista = new UI_Login(entrada);
            String cedula = vista.capturarCedula();
            String clave = vista.capturarClave();
            Usuario usuario = acceso.iniciarSesion(cedula, clave);

            if (usuario == null) {
                vista.mostrarError("Credenciales incorrectas.");
            } else {
                System.out.println("Sesion iniciada, " + usuario.getNombre() + ".");
            }
            return;
        }

        System.out.println("Opcion de acceso invalida.");
    }

    private void abrirSolicitud() {
        Usuario usuario = requerirSesion();

        if ("ADMIN".equals(usuario.getTipoUsuario())) {
            throw new IllegalStateException("Ingrese como miembro o visitante para presentar una solicitud.");
        }

        //El formulario toma los datos del solicitante que inicio sesion.
        UI_FormularioSolicitud vista = new UI_FormularioSolicitud(
                entrada, usuario, siguienteSolicitud);

        vista.presentarFormulario();
        Solicitud solicitud = vista.recibirDatosSolicitud();
        autorizacion.registrarSolicitud(solicitud);
        siguienteSolicitud++;

        System.out.println("Solicitud registrada, numero " + solicitud.getIdSolicitud() + ".");
    }

    private Solicitud leerSolicitud() {
        System.out.println("Ingrese el numero de solicitud.");
        String texto = entrada.nextLine().trim();
        int id;

        try {
            id = Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El numero de solicitud debe ser un entero.");
        }

        Solicitud solicitud = autorizacion.consultarSolicitud(id);
        if (solicitud == null) {
            throw new IllegalArgumentException("No existe una solicitud con ese numero.");
        }

        return solicitud;
    }

    private void abrirRevision() {
        Usuario usuario = requerirSesion();
        if (!"ADMIN".equals(usuario.getTipoUsuario())) {
            throw new IllegalStateException("Solo el administrador puede revisar solicitudes.");
        }

        Solicitud solicitud = leerSolicitud();
        UI_RevisionSolicitud vista = new UI_RevisionSolicitud(entrada);
        vista.mostrarSolicitud(solicitud);

        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new IllegalStateException("La solicitud ya fue revisada.");
        }

        //La vista recoge la decision y el servicio actualiza la solicitud.
        String decision = vista.registrarDecision();
        switch (decision) {
            case "1":
                autorizacion.emitirAutorizacion(solicitud);
                break;
            case "2":
                autorizacion.solicitarCorreccion(solicitud, vista.capturarMotivo());
                break;
            case "3":
                autorizacion.rechazarSolicitud(solicitud, vista.capturarMotivo());
                break;
            default:
                throw new IllegalArgumentException("Decision invalida.");
        }

        System.out.println("Decision registrada, " + solicitud.getEstado() + ".");
    }

    private void abrirConsultaResultado() {
        Usuario usuario = requerirSesion();
        Solicitud solicitud = leerSolicitud();

        boolean administrador = "ADMIN".equals(usuario.getTipoUsuario());
        boolean titular = usuario.getCedula().equals(solicitud.getIdentificacion());

        if (!administrador && !titular) {
            throw new IllegalStateException("Esta solicitud pertenece a otro usuario.");
        }

        UI_ResultadoAutorizacion vista = new UI_ResultadoAutorizacion();
        vista.mostrarResultado(solicitud.getEstado());

        if (!solicitud.getObservaciones().isBlank()) {
            vista.mostrarObservaciones(solicitud.getObservaciones());
        }

        //El codigo se recupera desde la autorizacion asociada a esta solicitud.
        Autorizacion permiso = autorizacion.consultarAutorizacion(solicitud.getIdSolicitud());
        if (permiso != null) {
            vista.mostrarCodigo(permiso.getCodigo());
        }
    }

    private void abrirIngreso() {
        Usuario usuario = requerirSesion();
        UI_LecturaQR lector = new UI_LecturaQR(entrada);
        UI_Asignacion vista = new UI_Asignacion();

        System.out.println("Entrada del parqueadero, use QR_ENTRADA.");
        ResultadoIngreso resultado = ingreso.procesarIngreso(usuario, lector.leerQR());

        //El estado del resultado determina el mensaje que recibe el usuario.
        switch (resultado.estado) {
            case ASIGNADO:
                vista.mostrarEspacio(resultado.constancia.getEspacio());
                break;
            case QR_INVALIDO:
                lector.mostrarError("QR de entrada invalido.");
                break;
            case SIN_AUTORIZACION:
                vista.mostrarRechazo("El usuario no tiene una autorizacion vigente.");
                break;
            case INGRESO_ACTIVO:
                vista.mostrarRechazo("El usuario ya tiene un ingreso activo.");
                break;
            case SIN_CUPO:
                vista.mostrarSinCupo();
                break;
        }
    }

    private void abrirSalida() {
        Usuario usuario = requerirSesion();
        UI_LecturaQR lector = new UI_LecturaQR(entrada);
        UI_ResultadoSalida vista = new UI_ResultadoSalida();

        System.out.println("Salida del parqueadero, use QR_SALIDA.");
        if (!esQrSalida(lector.leerQR())) {
            lector.mostrarError("QR de salida invalido.");
            return;
        }

        //El servicio usa la constancia activa para liberar el puesto correcto.
        if (salida.procesarSalida(usuario)) {
            vista.mostrarSalidaExitosa();
        } else {
            vista.mostrarError("No existe ingreso activo.");
        }
    }

    private boolean esQrSalida(String qr) {
        return "QR_SALIDA".equals(qr);
    }
}