package negocio;

import datos.UsuarioDAO;
import entidades.Usuario;

public class ServicioAcceso {
    private UsuarioDAO usuarios;
    private Usuario usuarioActual;

    public boolean registrarUsuario(Usuario u, String clave) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Usuario iniciarSesion(String cedula, String clave) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Usuario obtenerUsuarioActual() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void cerrarSesion() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
