package presentacion;

import java.util.Map;

public final class UI_ResultadoSalida {
    public Map<String, Object> mostrarSalidaExitosa() {
        return Map.of("estado", "SALIDA_REGISTRADA", "mensaje", "Salida registrada. Se liberó su espacio.");
    }

    public Map<String, Object> mostrarError(String estado, String motivo) {
        return Map.of("estado", estado, "mensaje", motivo);
    }
}
