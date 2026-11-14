package presentacion;

import entidades.Usuario;
import java.util.Map;

public final class UI_Home {
    public Map<String, Object> mostrarMenu(Usuario usuario) {
        if (usuario == null) {
            return Map.of("autenticado", false);
        }
        return Map.of("autenticado", true, "nombre", usuario.getNombre(), "cedula", usuario.getCedula(),
                "tipoUsuario", usuario.getTipoUsuario(), "placa", usuario.getPlaca(),
                "tipoVehiculo", usuario.getTipoVehiculo());
    }
}
