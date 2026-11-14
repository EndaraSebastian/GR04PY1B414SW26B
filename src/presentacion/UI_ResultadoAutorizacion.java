package presentacion;

//La vista presenta el estado, las observaciones y el codigo que recibe de la aplicacion.
public class UI_ResultadoAutorizacion {

    public void mostrarResultado(String estado) {
        System.out.println("Estado de la solicitud, " + estado + ".");
    }

    //La aplicacion obtiene el motivo desde la solicitud revisada.
    public void mostrarObservaciones(String motivo) {
        System.out.println("Observacion, " + motivo + ".");
    }

    //La aplicacion obtiene el codigo desde la autorizacion emitida.
    public void mostrarCodigo(String codigo) {
        System.out.println("Codigo de autorizacion, " + codigo + ".");
    }
}