package negocio;

import entidades.Constancia;
import java.util.Objects;

//ResultadoIngreso entrega a la aplicacion el estado y la constancia del ingreso.
public class ResultadoIngreso {

    public final EstadoIngreso estado;
    public final Constancia constancia;

    //ServicioIngreso entrega una constancia solo cuando asigna un espacio.
    public ResultadoIngreso(EstadoIngreso estado, Constancia constancia) {
        this.estado = Objects.requireNonNull(estado);

        if (estado == EstadoIngreso.ASIGNADO && constancia == null) {
            throw new IllegalArgumentException("El ingreso asignado necesita una constancia.");
        }

        if (estado != EstadoIngreso.ASIGNADO && constancia != null) {
            throw new IllegalArgumentException("Solo un ingreso asignado puede tener constancia.");
        }

        this.constancia = constancia;
    }
}