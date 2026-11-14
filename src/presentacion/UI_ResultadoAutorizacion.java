package presentacion;

import entidades.Autorizacion;
import entidades.Solicitud;
import java.util.LinkedHashMap;
import java.util.Map;

public final class UI_ResultadoAutorizacion {
    public Map<String, Object> mostrarResultado(Solicitud solicitud, Autorizacion permiso) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", solicitud.getIdSolicitud());
        datos.put("tipo", solicitud.getTipoSolicitud());
        datos.put("estado", solicitud.getEstado());
        datos.put("observaciones", solicitud.getObservaciones());
        datos.put("nombre", solicitud.getNombreSolicitante());
        datos.put("cedula", solicitud.getIdentificacion());
        datos.put("placa", solicitud.getPlaca());
        datos.put("tipoVehiculo", solicitud.getTipoVehiculo());
        datos.put("motivo", solicitud.getMotivoVisita());

        if (permiso != null) {
            datos.put("codigo", permiso.getCodigo());
            datos.put("fechaEmision", permiso.getFechaEmision().toInstant().toString());
            datos.put("fechaExpiracion", permiso.getFechaFin().toInstant().toString());
        }
        return datos;
    }
}
