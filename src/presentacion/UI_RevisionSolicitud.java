package presentacion;

public final class UI_RevisionSolicitud {
    public String registrarDecision(String decision) {
        if (decision == null || decision.isBlank()) {
            throw new IllegalArgumentException("Seleccione una decisión.");
        }
        return decision.strip();
    }

    public String capturarMotivo(String motivo, String mensaje) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return motivo.strip();
    }
}
