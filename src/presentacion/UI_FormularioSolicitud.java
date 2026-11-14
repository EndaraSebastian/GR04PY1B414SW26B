package presentacion;

import entidades.Solicitud;
import entidades.Usuario;
import java.util.Map;

public final class UI_FormularioSolicitud {
    public Solicitud recibirDatosSolicitud(Usuario solicitante, int id, String motivo) {
        if (!"VISITANTE".equals(solicitante.getTipoUsuario())) {
            return new Solicitud(id, solicitante, "ORDINARIA", "");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Ingrese el motivo de su visita.");
        }
        return new Solicitud(id, solicitante, "TEMPORAL", motivo.strip());
    }

    public Map<String, Object> mostrarConfirmacion(Solicitud solicitud) {
        return Map.of("id", solicitud.getIdSolicitud());
    }
}
