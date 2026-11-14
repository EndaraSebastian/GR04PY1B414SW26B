package entidades;

import java.util.Date;

public class Autorizacion {
    private int idAutorizacion;
    private String codigo;
    private String tipoAutorizacion;
    private String estado;
    private Date fechaEmision;
    private Date fechaFin;
    private Solicitud solicitud;
    private Usuario usuario;
}
