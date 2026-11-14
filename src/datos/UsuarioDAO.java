package datos;

import entidades.Usuario;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//UsuarioDAO conserva los usuarios registrados durante la ejecucion.
public class UsuarioDAO {

    private final List<Usuario> usuarios = new ArrayList<>();

    //ServicioAcceso guarda el usuario despues de preparar su clave.
    public void guardar(Usuario u) {
        Objects.requireNonNull(u);

        if (buscarPorCedula(u.getCedula()) != null) {
            throw new IllegalArgumentException("La cedula ya esta registrada.");
        }

        usuarios.add(u);
    }

    //ServicioAcceso y ServicioAutorizacion localizan al usuario por cedula.
    public Usuario buscarPorCedula(String cedula) {
        for (Usuario usuario : usuarios) {
            if (usuario.getCedula().equals(cedula)) {
                return usuario;
            }
        }

        return null;
    }

    public void actualizar(Usuario u) {
        Objects.requireNonNull(u);

        for (int i = 0; i < usuarios.size(); i++) {
            Usuario registrado = usuarios.get(i);

            if (registrado.getCedula().equals(u.getCedula())) {
                usuarios.set(i, u);
                return;
            }
        }

        throw new IllegalArgumentException("El usuario no esta registrado.");
    }
}