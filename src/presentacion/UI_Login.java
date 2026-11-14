package presentacion;

import java.util.Map;

public final class UI_Login {
    public String capturarCedula(String cedula) {
        if (cedula == null || cedula.isBlank()) {
            throw new IllegalArgumentException("Ingrese su cédula o el identificador ADMIN.");
        }
        String identificador = cedula.strip();
        return "ADMIN".equalsIgnoreCase(identificador) ? "ADMIN" : identificador;
    }

    public Map<String, Object> mostrarError() {
        return Map.of("error", "La cédula o la clave son incorrectas.");
    }
}
