package negocio;

import datos.AutorizacionDAO;
import datos.SolicitudDAO;
import datos.UsuarioDAO;
import entidades.Autorizacion;
import entidades.Solicitud;
import entidades.Usuario;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;

//ServicioAutorizacion coordina la solicitud, la decision y la emision del permiso.
public class ServicioAutorizacion {

    private final UsuarioDAO usuarios;
    private final SolicitudDAO solicitudes;
    private final AutorizacionDAO autorizaciones;

    public ServicioAutorizacion(UsuarioDAO usuarios, SolicitudDAO solicitudes,
                                AutorizacionDAO autorizaciones) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.solicitudes = Objects.requireNonNull(solicitudes);
        this.autorizaciones = Objects.requireNonNull(autorizaciones);
    }

    //El formulario entrega una solicitud asociada al usuario registrado.
    public void registrarSolicitud(Solicitud s) {
        Objects.requireNonNull(s);

        Usuario registrado = usuarios.buscarPorCedula(s.getIdentificacion());
        if (registrado == null || registrado != s.getSolicitante()) {
            throw new IllegalArgumentException("El solicitante no corresponde al usuario registrado.");
        }

        if (!"PENDIENTE".equals(s.getEstado())) {
            throw new IllegalArgumentException("La solicitud debe iniciar pendiente.");
        }

        comprobarSinAutorizacionVigente(s, new Date());
        solicitudes.guardar(s);
    }

    //La revision y la consulta de resultados recuperan el mismo tramite.
    public Solicitud consultarSolicitud(int id) {
        return solicitudes.buscarPorId(id);
    }

    //El administrador solicita la evaluacion antes de emitir la autorizacion.
    public boolean evaluarSolicitud(Solicitud s) {
        if (s == null || solicitudes.buscarPorId(s.getIdSolicitud()) != s) {
            return false;
        }

        if (!"PENDIENTE".equals(s.getEstado()) || s.getPlaca().isBlank()) {
            return false;
        }

        String tipoUsuario = s.getSolicitante().getTipoUsuario();

        if ("ORDINARIA".equals(s.getTipoSolicitud())) {
            return "MCP".equals(tipoUsuario);
        }

        if ("TEMPORAL".equals(s.getTipoSolicitud())) {
            return "VISITANTE".equals(tipoUsuario) && !s.getMotivoVisita().isBlank();
        }

        return false;
    }

    //La autorizacion conserva la solicitud de origen y su titular.
    public Autorizacion emitirAutorizacion(Solicitud s) {
        if (!evaluarSolicitud(s)) {
            throw new IllegalArgumentException("La solicitud no cumple las condiciones de aprobacion.");
        }

        Date fechaEmision = new Date();
        comprobarSinAutorizacionVigente(s, fechaEmision);

        int id = s.getIdSolicitud();
        String codigo = "AUT-" + id;

        //La demostracion usa un dia para visita y un anio para autorizacion ordinaria.
        int dias = "TEMPORAL".equals(s.getTipoSolicitud()) ? 1 : 365;
        Date fechaFin = Date.from(fechaEmision.toInstant().plus(dias, ChronoUnit.DAYS));

        Autorizacion a = new Autorizacion(
            id, codigo, s.getTipoSolicitud(), "APROBADA",
            fechaEmision, fechaFin, s, s.getSolicitante()
        );

        autorizaciones.guardar(a);
        s.resolver("APROBADA", "");
        solicitudes.actualizar(s);
        return a;
    }

    private void comprobarSinAutorizacionVigente(Solicitud s, Date fecha) {
        if (autorizaciones.buscarVigentePorUsuario(s.getIdentificacion(), fecha) != null) {
            throw new IllegalStateException("El usuario ya tiene una autorizacion vigente.");
        }
    }

    public void rechazarSolicitud(Solicitud s, String motivo) {
        registrarDecision(s, "RECHAZADA", motivo);
    }

    public void solicitarCorreccion(Solicitud s, String motivo) {
        registrarDecision(s, "CORRECCION", motivo);
    }

    //Ambas decisiones conservan el motivo que luego se muestra al solicitante.
    private void registrarDecision(Solicitud s, String estado, String motivo) {
        if (s == null || solicitudes.buscarPorId(s.getIdSolicitud()) != s) {
            throw new IllegalArgumentException("La solicitud no esta registrada.");
        }

        s.resolver(estado, motivo);
        solicitudes.actualizar(s);
    }

    //La vista de resultado usa la autorizacion para mostrar el codigo emitido.
    public Autorizacion consultarAutorizacion(int idSolicitud) {
        return autorizaciones.buscarPorSolicitud(idSolicitud);
    }
}
