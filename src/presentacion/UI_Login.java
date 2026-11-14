package presentacion;

import java.util.Objects;
import java.util.Scanner;

//La vista de acceso recoge los datos que la aplicacion entrega a ServicioAcceso.
public class UI_Login {

    private final Scanner entrada;

    public UI_Login(Scanner entrada) {
        this.entrada = Objects.requireNonNull(entrada);
    }

    public String capturarCedula() {
        System.out.println("Ingrese su cedula o el identificador del administrador.");
        return entrada.nextLine().trim();
    }

    public String capturarClave() {
        System.out.println("Ingrese su clave.");
        return entrada.nextLine();
    }

    public void mostrarError(String mensaje) {
        System.out.println(mensaje);
    }
}