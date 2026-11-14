package entidades;

//Espacio representa un puesto del parqueadero que puede estar libre u ocupado.
public class Espacio {

    private final String idEspacio;
    private String estado;

    public Espacio(String idEspacio, String estado) {
        if (!"LIBRE".equals(estado) && !"OCUPADO".equals(estado)) {
            throw new IllegalArgumentException("Estado de espacio invalido.");
        }

        this.idEspacio = idEspacio;
        this.estado = estado;
    }

    //La interfaz de asignacion muestra este identificador al usuario.
    public String getIdEspacio() {
        return idEspacio;
    }

    //EspacioDAO consulta el estado cuando busca un puesto libre.
    public String getEstado() {
        return estado;
    }

    //ServicioIngreso ocupa el espacio al registrar el ingreso.
    public void ocupar() {
        if (!"LIBRE".equals(estado)) {
            throw new IllegalStateException("El espacio ya esta ocupado.");
        }

        estado = "OCUPADO";
    }

    //ServicioSalida libera el espacio que recupera de la constancia.
    public void liberar() {
        estado = "LIBRE";
    }
}