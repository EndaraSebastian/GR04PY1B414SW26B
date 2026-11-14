package presentacion;

import entidades.Usuario;
import java.util.Locale;
import java.util.Map;

public final class UI_SignUp {
    public Usuario capturarDatosRegistro(Map<String, String> datos) {
        String cedula = leerDato(datos.get("cedula"), "Ingrese su cédula.");
        if ("ADMIN".equalsIgnoreCase(cedula)) {
            throw new IllegalArgumentException("Ese identificador está reservado para el administrador.");
        }
        String nombre = leerDato(datos.get("nombre"), "Ingrese su nombre.");
        String tipoUsuario = leerDato(datos.get("tipoUsuario"), "Seleccione MCP o VISITANTE.")
                .toUpperCase(Locale.ROOT);
        if (!"MCP".equals(tipoUsuario) && !"VISITANTE".equals(tipoUsuario)) {
            throw new IllegalArgumentException("El tipo de usuario debe ser MCP o VISITANTE.");
        }
        String placa = leerDato(datos.get("placa"), "Ingrese la placa.").toUpperCase(Locale.ROOT);
        String tipoVehiculo = leerDato(datos.get("tipoVehiculo"), "Seleccione MOTO o AUTOMOVIL.")
                .toUpperCase(Locale.ROOT);
        if (!"MOTO".equals(tipoVehiculo) && !"AUTOMOVIL".equals(tipoVehiculo)) {
            throw new IllegalArgumentException("El tipo de vehículo debe ser MOTO o AUTOMOVIL.");
        }
        return new Usuario(cedula, nombre, tipoUsuario, placa, tipoVehiculo);
    }

    public String capturarClave(Map<String, String> datos) {
        String clave = datos.get("clave");
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("Ingrese una clave.");
        }
        return clave;
    }

    public Map<String, Object> mostrarResultado() {
        return Map.of("mensaje", "Cuenta creada. Ya puedes iniciar sesión.");
    }

    private String leerDato(String dato, String mensaje) {
        if (dato == null || dato.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return dato.strip();
    }
}
