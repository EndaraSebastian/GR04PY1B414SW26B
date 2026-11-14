package presentacion;

import java.util.Objects;
import java.util.Scanner;

//La vista entrega a la aplicacion el texto leido del QR de entrada o salida.
public class UI_LecturaQR {

    private final Scanner entrada;

    public UI_LecturaQR(Scanner entrada) {
        this.entrada = Objects.requireNonNull(entrada);
    }

    //En la consola se escribe el mismo texto que entregaria el lector.
    public String leerQR() {
        System.out.println("Ingrese el texto del QR.");
        return entrada.nextLine().trim();
    }

    public void mostrarError(String mensaje) {
        System.out.println(mensaje);
    }
}