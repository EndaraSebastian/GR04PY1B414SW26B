package presentacion;

//La vista comunica el resultado que la aplicacion recibe de ServicioSalida.
public class UI_ResultadoSalida {

    //La aplicacion confirma la salida despues de liberar el espacio.
    public void mostrarSalidaExitosa() {
        System.out.println("Salida registrada, espacio liberado.");
    }

    public void mostrarError(String motivo) {
        System.out.println(motivo);
    }
}