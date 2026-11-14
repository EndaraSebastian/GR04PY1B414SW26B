package datos;

import entidades.Autorizacion;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

//AutorizacionDAO conserva las autorizaciones emitidas para los usuarios.
public class AutorizacionDAO {

    private final List<Autorizacion> autorizaciones = new ArrayList<>();

    //ServicioAutorizacion guarda la autorizacion emitida para una solicitud.
    public void guardar(Autorizacion a) {
        Objects.requireNonNull(a);

        if (buscarPorId(a.getIdAutorizacion()) != null) {
            throw new IllegalArgumentException("El numero de autorizacion ya existe.");
        }

        if (buscarPorSolicitud(a.getSolicitud().getIdSolicitud()) != null) {
            throw new IllegalArgumentException("La solicitud ya tiene una autorizacion.");
        }

        autorizaciones.add(a);
    }

    public Autorizacion buscarPorId(int id) {
        for (Autorizacion autorizacion : autorizaciones) {
            if (autorizacion.getIdAutorizacion() == id) {
                return autorizacion;
            }
        }

        return null;
    }

    //La consulta del resultado recupera la autorizacion creada para la solicitud.
    public Autorizacion buscarPorSolicitud(int idSolicitud) {
        for (Autorizacion autorizacion : autorizaciones) {
            if (autorizacion.getSolicitud().getIdSolicitud() == idSolicitud) {
                return autorizacion;
            }
        }

        return null;
    }

    //ServicioIngreso consulta la cedula del titular y la vigencia de su permiso.
    public Autorizacion buscarVigentePorUsuario(String cedula, Date fecha) {
        Objects.requireNonNull(fecha);

        for (Autorizacion autorizacion : autorizaciones) {
            boolean mismoUsuario = autorizacion.getUsuario().getCedula().equals(cedula);
            boolean aprobada = "APROBADA".equals(autorizacion.getEstado());
            boolean emitida = !fecha.before(autorizacion.getFechaEmision());
            boolean vigente = !fecha.after(autorizacion.getFechaFin());

            if (mismoUsuario && aprobada && emitida && vigente) {
                return autorizacion;
            }
        }

        return null;
    }

    public void actualizar(Autorizacion a) {
        Objects.requireNonNull(a);

        Autorizacion existente = buscarPorSolicitud(a.getSolicitud().getIdSolicitud());
        if (existente != null && existente.getIdAutorizacion() != a.getIdAutorizacion()) {
            throw new IllegalArgumentException("La solicitud ya tiene otra autorizacion.");
        }

        for (int i = 0; i < autorizaciones.size(); i++) {
            if (autorizaciones.get(i).getIdAutorizacion() == a.getIdAutorizacion()) {
                autorizaciones.set(i, a);
                return;
            }
        }

        throw new IllegalArgumentException("La autorizacion no esta registrada.");
    }

    public void eliminarPorId(int id) {
        autorizaciones.removeIf(autorizacion -> autorizacion.getIdAutorizacion() == id);
    }
}