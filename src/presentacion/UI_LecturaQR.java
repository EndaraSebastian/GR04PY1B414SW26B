package presentacion;

import java.util.Map;

public final class UI_LecturaQR {
    // El navegador decodifica la imagen y envía el texto leído.
    public String leerQR(Map<String, String> datos) {
        return datos.get("qr");
    }

    public Map<String, Object> mostrarError() {
        return Map.of("estado", "QR_INVALIDO", "mensaje", "Código QR de salida inválido.");
    }
}
