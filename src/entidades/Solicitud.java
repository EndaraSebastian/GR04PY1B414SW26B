package entidades;

import java.util.Objects;

//Solicitud guarda los datos presentados y mantiene la referencia al solicitante.
public class Solicitud {

    private final int idSolicitud;
    private final String tipoSolicitud;
    private final String identificacion;
    private final String nombreSolicitante;
    private final String placa;
    private final String tipoVehiculo;
    private final String motivoVisita;
    private final Usuario solicitante;
    private String estado;
    private String observaciones;

    //El formulario entrega el usuario de la sesion y los datos del tramite.
    public Solicitud(int idSolicitud, Usuario solicitante,
                     String tipoSolicitud, String motivoVisita) {
        this.solicitante = Objects.requireNonNull(solicitante);
        this.idSolicitud = idSolicitud;
        this.tipoSolicitud = Objects.requireNonNull(tipoSolicitud);
        this.motivoVisita = motivoVisita == null ? "" : motivoVisita.trim();

        //La solicitud conserva los datos del usuario al momento de presentarla.
        this.identificacion = solicitante.getCedula();
        this.nombreSolicitante = solicitante.getNombre();
        this.placa = solicitante.getPlaca();
        this.tipoVehiculo = solicitante.getTipoVehiculo();

        this.estado = "PENDIENTE";
        this.observaciones = "";
    }

    //ServicioAutorizacion registra la decision tomada por el administrador.
    public void resolver(String nuevoEstado, String motivo) {
        if (!"PENDIENTE".equals(estado)) {
            throw new IllegalStateException("La solicitud ya fue revisada.");
        }

        boolean aprobada = "APROBADA".equals(nuevoEstado);
        boolean rechazada = "RECHAZADA".equals(nuevoEstado);
        boolean requiereCorreccion = "CORRECCION".equals(nuevoEstado);

        if (!aprobada && !rechazada && !requiereCorreccion) {
            throw new IllegalArgumentException("Decision de solicitud invalida.");
        }

        String detalle = motivo == null ? "" : motivo.trim();
        if ((rechazada || requiereCorreccion) && detalle.isEmpty()) {
            throw new IllegalArgumentException("Escriba el motivo de la decision.");
        }

        estado = nuevoEstado;
        observaciones = detalle;
    }

    public int getIdSolicitud() {
        return idSolicitud;
    }

    public String getTipoSolicitud() {
        return tipoSolicitud;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public String getNombreSolicitante() {
        return nombreSolicitante;
    }

    public String getPlaca() {
        return placa;
    }

    public String getTipoVehiculo() {
        return tipoVehiculo;
    }

    public String getMotivoVisita() {
        return motivoVisita;
    }

    public String getEstado() {
        return estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }
}