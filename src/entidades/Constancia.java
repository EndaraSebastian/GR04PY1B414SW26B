package entidades;

import java.util.Date;
import java.util.Objects;

//Constancia vincula un ingreso con el usuario y el espacio ocupado.
public class Constancia {

    private final String idConstancia;
    private final Usuario usuario;
    private final Espacio espacio;
    private final Date fechaIngreso;

    //ServicioIngreso entrega el usuario y el espacio al crear la constancia.
    public Constancia(String idConstancia, Date fechaIngreso,
                      Usuario usuario, Espacio espacio) {
        this.idConstancia = Objects.requireNonNull(idConstancia);
        this.usuario = Objects.requireNonNull(usuario);
        this.espacio = Objects.requireNonNull(espacio);
        this.fechaIngreso = new Date(Objects.requireNonNull(fechaIngreso).getTime());
    }

    public String getIdConstancia() {
        return idConstancia;
    }

    //ConstanciaDAO consulta el usuario para buscar el ingreso activo por cedula.
    public Usuario getUsuario() {
        return usuario;
    }

    //ServicioSalida recupera el mismo espacio que se asigno al ingresar.
    public Espacio getEspacio() {
        return espacio;
    }

    //La copia protege la fecha original del ingreso.
    public Date getFechaIngreso() {
        return new Date(fechaIngreso.getTime());
    }
}