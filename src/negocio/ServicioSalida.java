package negocio;

import datos.ConstanciaDAO;
import datos.EspacioDAO;
import entidades.Constancia;
import entidades.Espacio;
import entidades.Usuario;
import java.util.Objects;

//ServicioSalida usa las mismas constancias y espacios que ServicioIngreso.
public class ServicioSalida {

    private final ConstanciaDAO constancias;
    private final EspacioDAO espacios;

    public ServicioSalida(ConstanciaDAO constancias, EspacioDAO espacios) {
        this.constancias = Objects.requireNonNull(constancias);
        this.espacios = Objects.requireNonNull(espacios);
    }

    //La aplicacion verifica el QR de salida antes de invocar este metodo.
    public boolean procesarSalida(Usuario usuario) {
        if (usuario == null) {
            return false;
        }

        Constancia c = buscarConstancia(usuario);
        if (c == null) {
            return false;
        }

        liberarEspacio(c.getEspacio());
        eliminarConstancia(c);
        return true;
    }

    private Constancia buscarConstancia(Usuario usuario) {
        return constancias.buscarActiva(usuario.getCedula());
    }

    //La constancia identifica exactamente el puesto que se debe liberar.
    private void liberarEspacio(Espacio espacio) {
        espacio.liberar();
        espacios.actualizar(espacio);
    }

    private void eliminarConstancia(Constancia c) {
        constancias.eliminar(c);
    }
}