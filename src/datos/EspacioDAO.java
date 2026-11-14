package datos;

import entidades.Espacio;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//EspacioDAO conserva los puestos que comparten los servicios de ingreso y salida.
public class EspacioDAO {

    private final List<Espacio> espacios = new ArrayList<>();

    //AplicacionParqueadero indica cuantos puestos existen al iniciar el programa.
    public EspacioDAO(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad de espacios debe ser cero o mayor.");
        }

        for (int numero = 1; numero <= cantidad; numero++) {
            espacios.add(new Espacio("E-" + numero, "LIBRE"));
        }
    }

    //ServicioIngreso solicita un puesto libre antes de registrar la constancia.
    public Espacio buscarLibre() {
        for (Espacio espacio : espacios) {
            if ("LIBRE".equals(espacio.getEstado())) {
                return espacio;
            }
        }

        return null;
    }

    //Ingreso y salida actualizan el puesto identificado por el mismo codigo.
    public void actualizar(Espacio e) {
        Objects.requireNonNull(e);

        for (int i = 0; i < espacios.size(); i++) {
            if (espacios.get(i).getIdEspacio().equals(e.getIdEspacio())) {
                espacios.set(i, e);
                return;
            }
        }

        throw new IllegalArgumentException("El espacio no pertenece al parqueadero.");
    }
}