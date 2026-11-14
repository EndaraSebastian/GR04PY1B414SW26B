package datos;

import entidades.Constancia;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//ConstanciaDAO conserva los ingresos activos hasta que el usuario sale.
public class ConstanciaDAO {

    private final List<Constancia> constanciasActivas = new ArrayList<>();

    //ServicioIngreso guarda la constancia del usuario y del espacio asignado.
    public void guardar(Constancia c) {
        Objects.requireNonNull(c);

        String cedula = c.getUsuario().getCedula();
        if (buscarActiva(cedula) != null) {
            throw new IllegalStateException("El usuario ya tiene un ingreso activo.");
        }

        for (Constancia activa : constanciasActivas) {
            if (activa.getIdConstancia().equals(c.getIdConstancia())) {
                throw new IllegalArgumentException("El identificador de constancia ya existe.");
            }

            if (activa.getEspacio().getIdEspacio().equals(c.getEspacio().getIdEspacio())) {
                throw new IllegalStateException("El espacio ya tiene una constancia activa.");
            }
        }

        constanciasActivas.add(c);
    }

    //ServicioIngreso comprueba si el usuario ya ingreso.
    //ServicioSalida recupera la constancia para localizar el espacio ocupado.
    public Constancia buscarActiva(String cedula) {
        for (Constancia activa : constanciasActivas) {
            if (activa.getUsuario().getCedula().equals(cedula)) {
                return activa;
            }
        }

        return null;
    }

    //ServicioSalida elimina la constancia despues de liberar el espacio.
    public void eliminar(Constancia c) {
        Objects.requireNonNull(c);

        boolean eliminada = constanciasActivas.removeIf(
            activa -> activa.getIdConstancia().equals(c.getIdConstancia())
        );

        if (!eliminada) {
            throw new IllegalArgumentException("La constancia no esta activa.");
        }
    }
}