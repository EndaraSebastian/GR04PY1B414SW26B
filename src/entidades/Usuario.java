package entidades;

//Usuario identifica al titular de las solicitudes, autorizaciones y constancias.
public class Usuario {

    private final String cedula;
    private final String nombre;
    private final String tipoUsuario;
    private final String placa;
    private final String tipoVehiculo;
    private String claveHash;

    public Usuario(String cedula, String nombre, String tipoUsuario,
                   String placa, String tipoVehiculo) {
        this.cedula = cedula;
        this.nombre = nombre;
        this.tipoUsuario = tipoUsuario;
        this.placa = placa;
        this.tipoVehiculo = tipoVehiculo;
    }

    //UsuarioDAO usa la cedula para localizar al usuario registrado.
    public String getCedula() {
        return cedula;
    }

    public String getNombre() {
        return nombre;
    }

    //ServicioAutorizacion consulta el tipo de usuario al evaluar la solicitud.
    public String getTipoUsuario() {
        return tipoUsuario;
    }

    public String getPlaca() {
        return placa;
    }

    public String getTipoVehiculo() {
        return tipoVehiculo;
    }

    //ServicioAcceso consulta este valor al verificar la clave.
    public String getClaveHash() {
        return claveHash;
    }

    //ServicioAcceso establece el hash cuando registra al usuario.
    public void setClaveHash(String claveHash) {
        this.claveHash = claveHash;
    }
}