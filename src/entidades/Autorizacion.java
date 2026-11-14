package entidades;

import java.util.Date;
import java.util.Objects;

//Autorizacion vincula la solicitud aprobada con el usuario que puede ingresar.
public class Autorizacion {

    private final int idAutorizacion;
    private final String codigo;
    private final String tipoAutorizacion;
    private final String estado;
    private final Date fechaEmision;
    private final Date fechaFin;
    private final Solicitud solicitud;
    private final Usuario usuario;

    //ServicioAutorizacion crea la autorizacion al aprobar la solicitud.
    public Autorizacion(int idAutorizacion, String codigo,
                        String tipoAutorizacion, String estado,
                        Date fechaEmision, Date fechaFin,
                        Solicitud solicitud, Usuario usuario) {
        this.solicitud = Objects.requireNonNull(solicitud);
        this.usuario = Objects.requireNonNull(usuario);

        if (!solicitud.getSolicitante().getCedula().equals(usuario.getCedula())) {
            throw new IllegalArgumentException("El usuario debe ser el solicitante.");
        }

        this.fechaEmision = new Date(Objects.requireNonNull(fechaEmision).getTime());
        this.fechaFin = new Date(Objects.requireNonNull(fechaFin).getTime());

        if (this.fechaFin.before(this.fechaEmision)) {
            throw new IllegalArgumentException("La fecha final es anterior a la emision.");
        }

        this.idAutorizacion = idAutorizacion;
        this.codigo = Objects.requireNonNull(codigo);
        this.tipoAutorizacion = Objects.requireNonNull(tipoAutorizacion);
        this.estado = Objects.requireNonNull(estado);
    }

    public int getIdAutorizacion() {
        return idAutorizacion;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTipoAutorizacion() {
        return tipoAutorizacion;
    }

    public String getEstado() {
        return estado;
    }

    //AutorizacionDAO consulta estas fechas para comprobar la vigencia.
    public Date getFechaEmision() {
        return new Date(fechaEmision.getTime());
    }

    public Date getFechaFin() {
        return new Date(fechaFin.getTime());
    }

    //La referencia permite recuperar la solicitud que origino la autorizacion.
    public Solicitud getSolicitud() {
        return solicitud;
    }

    //ServicioIngreso identifica al titular mediante esta referencia.
    public Usuario getUsuario() {
        return usuario;
    }
}