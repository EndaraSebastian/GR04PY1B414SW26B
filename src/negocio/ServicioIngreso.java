package negocio;

import datos.AutorizacionDAO;
import datos.ConstanciaDAO;
import datos.EspacioDAO;
import entidades.Constancia;
import entidades.Espacio;
import entidades.Usuario;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

//ServicioIngreso comprueba la autorizacion y registra el espacio ocupado.
public class ServicioIngreso {

    private final AutorizacionDAO autorizaciones;
    private final ConstanciaDAO constancias;
    private final EspacioDAO espacios;

    public ServicioIngreso(AutorizacionDAO autorizaciones, ConstanciaDAO constancias,
                           EspacioDAO espacios) {
        this.autorizaciones = Objects.requireNonNull(autorizaciones);
        this.constancias = Objects.requireNonNull(constancias);
        this.espacios = Objects.requireNonNull(espacios);
    }

    //La aplicacion entrega el usuario de la sesion y el texto leido del QR.
    public ResultadoIngreso procesarIngreso(Usuario usuario, String qr) {
        if (!"QR_ENTRADA".equals(qr)) {
            return new ResultadoIngreso(EstadoIngreso.QR_INVALIDO, null);
        }

        if (usuario == null || !comprobarAutorizacion(usuario)) {
            return new ResultadoIngreso(EstadoIngreso.SIN_AUTORIZACION, null);
        }

        if (tieneIngresoActivo(usuario)) {
            return new ResultadoIngreso(EstadoIngreso.INGRESO_ACTIVO, null);
        }

        Espacio espacio = asignarEspacio();
        if (espacio == null) {
            return new ResultadoIngreso(EstadoIngreso.SIN_CUPO, null);
        }

        Constancia c = registrarIngreso(usuario, espacio);
        return new ResultadoIngreso(EstadoIngreso.ASIGNADO, c);
    }

    private boolean comprobarAutorizacion(Usuario usuario) {
        return autorizaciones.buscarVigentePorUsuario(usuario.getCedula(), new Date()) != null;
    }

    private boolean tieneIngresoActivo(Usuario usuario) {
        return constancias.buscarActiva(usuario.getCedula()) != null;
    }

    private Espacio asignarEspacio() {
        return espacios.buscarLibre();
    }

    //La constancia conserva la referencia al espacio que salida debera liberar.
    private Constancia registrarIngreso(Usuario usuario, Espacio espacio) {
        Constancia c = new Constancia(
            UUID.randomUUID().toString(), new Date(), usuario, espacio
        );

        constancias.guardar(c);
        espacio.ocupar();
        espacios.actualizar(espacio);
        return c;
    }
}