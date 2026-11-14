package negocio;

//ServicioIngreso usa estos estados para comunicar el resultado del ingreso.
public enum EstadoIngreso {

    QR_INVALIDO,
    SIN_AUTORIZACION,
    INGRESO_ACTIVO,
    SIN_CUPO,
    ASIGNADO
}