package aplicacion;

import negocio.ServicioAcceso;
import negocio.ServicioAutorizacion;
import negocio.ServicioIngreso;
import negocio.ServicioSalida;

public class AplicacionParqueadero {
    private ServicioAcceso acceso;
    private ServicioAutorizacion autorizacion;
    private ServicioIngreso ingreso;
    private ServicioSalida salida;

    public void iniciar() {
        System.out.println("Parqueadero EPN iniciado.");
    }

    private void abrirAcceso() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    private void abrirSolicitud() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    private void abrirRevision() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    private void abrirConsultaResultado() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    private void abrirIngreso() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    private void abrirSalida() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    private boolean esQrSalida(String qr) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
