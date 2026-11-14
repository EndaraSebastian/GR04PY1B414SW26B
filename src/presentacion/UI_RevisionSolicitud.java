package presentacion;

import entidades.Solicitud;
import java.util.Objects;
import java.util.Scanner;

//La vista muestra la solicitud y recoge la decision del administrador.
public class UI_RevisionSolicitud {

    private final Scanner entrada;

    public UI_RevisionSolicitud(Scanner entrada) {
        this.entrada = Objects.requireNonNull(entrada);
    }

    public void mostrarSolicitud(Solicitud s) {
        Objects.requireNonNull(s);

        System.out.println("Solicitud numero " + s.getIdSolicitud() + ".");
        System.out.println("Tipo de solicitud, " + s.getTipoSolicitud() + ".");
        System.out.println("Solicitante, " + s.getNombreSolicitante() + ".");
        System.out.println("Cedula, " + s.getIdentificacion() + ".");
        System.out.println("Placa, " + s.getPlaca() + ".");
        System.out.println("Tipo de vehiculo, " + s.getTipoVehiculo() + ".");
        System.out.println("Estado, " + s.getEstado() + ".");

        if ("TEMPORAL".equals(s.getTipoSolicitud())) {
            String motivo = s.getMotivoVisita();
            if (motivo.isBlank()) {
                System.out.println("Motivo de visita pendiente.");
            } else {
                System.out.println("Motivo de visita, " + motivo + ".");
            }
        }
    }

    //La aplicacion usa la opcion elegida para llamar al metodo del servicio.
    public String registrarDecision() {
        System.out.println("1 Aprobar, 2 Pedir correccion, 3 Rechazar.");

        String decision = entrada.nextLine().trim();
        if (!"1".equals(decision) && !"2".equals(decision) && !"3".equals(decision)) {
            throw new IllegalArgumentException("Seleccione una decision valida.");
        }

        return decision;
    }

    //El motivo queda en la solicitud cuando se pide una correccion o se rechaza.
    public String capturarMotivo() {
        System.out.println("Ingrese el motivo de la decision.");
        String motivo = entrada.nextLine().trim();

        if (motivo.isEmpty()) {
            throw new IllegalArgumentException("Escriba el motivo de la decision.");
        }

        return motivo;
    }
}