package presentacion;

import entidades.Solicitud;
import entidades.Usuario;
import java.util.Objects;
import java.util.Scanner;

//El formulario construye la solicitud con los datos del usuario de la sesion.
public class UI_FormularioSolicitud {

    private final Scanner entrada;
    private final Usuario solicitante;
    private final int idSolicitud;

    public UI_FormularioSolicitud(Scanner entrada, Usuario solicitante, int idSolicitud) {
        this.entrada = Objects.requireNonNull(entrada);
        this.solicitante = Objects.requireNonNull(solicitante);
        this.idSolicitud = idSolicitud;
    }

    public void presentarFormulario() {
        System.out.println("Solicitud de autorizacion para el parqueadero.");
        System.out.println("Solicitante, " + solicitante.getNombre() + ".");
        System.out.println("Cedula, " + solicitante.getCedula() + ".");
        System.out.println("Placa, " + solicitante.getPlaca() + ".");
    }

    //La aplicacion entrega la solicitud creada a ServicioAutorizacion.
    public Solicitud recibirDatosSolicitud() {
        String tipoUsuario = solicitante.getTipoUsuario();

        if ("MCP".equals(tipoUsuario)) {
            return new Solicitud(idSolicitud, solicitante, "ORDINARIA", "");
        }

        if ("VISITANTE".equals(tipoUsuario)) {
            System.out.println("Ingrese el motivo de la visita.");
            String motivo = entrada.nextLine().trim();
            return new Solicitud(idSolicitud, solicitante, "TEMPORAL", motivo);
        }

        throw new IllegalArgumentException("El tipo de usuario no puede presentar solicitudes.");
    }
}