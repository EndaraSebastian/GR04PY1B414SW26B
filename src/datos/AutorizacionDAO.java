package datos;

import entidades.Autorizacion;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AutorizacionDAO {
    private List<Autorizacion> autorizaciones = new ArrayList<>();

    public void guardar(Autorizacion a) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Autorizacion buscarPorId(int id) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Autorizacion buscarPorSolicitud(int idSolicitud) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Autorizacion buscarVigentePorUsuario(String cedula, Date fecha) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void actualizar(Autorizacion a) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void eliminarPorId(int id) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
