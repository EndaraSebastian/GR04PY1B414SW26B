package datos;

import entidades.Solicitud;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//SolicitudDAO conserva las solicitudes ordinarias y temporales en memoria.
public class SolicitudDAO {

    private final List<Solicitud> solicitudes = new ArrayList<>();

    //ServicioAutorizacion guarda la solicitud presentada por el usuario.
    public void guardar(Solicitud s) {
        Objects.requireNonNull(s);

        if (buscarPorId(s.getIdSolicitud()) != null) {
            throw new IllegalArgumentException("El numero de solicitud ya existe.");
        }

        solicitudes.add(s);
    }

    //El servicio usa el numero de solicitud para recuperar el tramite.
    public Solicitud buscarPorId(int id) {
        for (Solicitud solicitud : solicitudes) {
            if (solicitud.getIdSolicitud() == id) {
                return solicitud;
            }
        }

        return null;
    }

    //ServicioAutorizacion guarda el estado y las observaciones de la revision.
    public void actualizar(Solicitud s) {
        Objects.requireNonNull(s);

        for (int i = 0; i < solicitudes.size(); i++) {
            if (solicitudes.get(i).getIdSolicitud() == s.getIdSolicitud()) {
                solicitudes.set(i, s);
                return;
            }
        }

        throw new IllegalArgumentException("La solicitud no esta registrada.");
    }

    public void eliminarPorId(int id) {
        solicitudes.removeIf(solicitud -> solicitud.getIdSolicitud() == id);
    }
}