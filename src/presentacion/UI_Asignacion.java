package presentacion;

import java.util.Map;
import negocio.ResultadoIngreso;

public final class UI_Asignacion {
    public Map<String, Object> mostrarResultado(ResultadoIngreso resultado) {
        String estado = resultado.estado.name();
        return switch (resultado.estado) {
            case ASIGNADO -> Map.of("estado", estado,
                    "espacio", resultado.constancia.getEspacio().getIdEspacio(),
                    "mensaje", "Ingreso registrado.");
            case QR_INVALIDO -> mostrarRechazo(estado, "Código QR de entrada inválido.");
            case SIN_AUTORIZACION -> mostrarRechazo(estado, "El usuario no tiene una autorización vigente.");
            case INGRESO_ACTIVO -> mostrarRechazo(estado, "El usuario ya tiene un ingreso activo.");
            case SIN_CUPO -> mostrarRechazo(estado, "No hay espacios libres.");
        };
    }

    public Map<String, Object> mostrarRechazo(String estado, String motivo) {
        return Map.of("estado", estado, "mensaje", motivo);
    }
}
