package negocio;

import datos.AutorizacionDAO;
import datos.SolicitudDAO;
import datos.UsuarioDAO;
import entidades.Autorizacion;
import entidades.Solicitud;

public class ServicioAutorizacion {
    private UsuarioDAO usuarios;
    private SolicitudDAO solicitudes;
    private AutorizacionDAO autorizaciones;

    public void registrarSolicitud(Solicitud s) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Solicitud consultarSolicitud(int id) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public boolean evaluarSolicitud(Solicitud s) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Autorizacion emitirAutorizacion(Solicitud s) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void rechazarSolicitud(Solicitud s, String motivo) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void solicitarCorreccion(Solicitud s, String motivo) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Autorizacion consultarAutorizacion(int idSolicitud) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
