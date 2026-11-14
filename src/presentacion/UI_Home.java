package presentacion;

import java.util.Objects;
import java.util.Scanner;

//La vista muestra el menu y devuelve la opcion elegida a la aplicacion.
public class UI_Home {

    private final Scanner entrada;

    public UI_Home(Scanner entrada) {
        this.entrada = Objects.requireNonNull(entrada);
    }

    public String mostrarMenu() {
        System.out.println();
        System.out.println("PARQUEADERO EPN");
        System.out.println("1. Registrar usuario o iniciar sesion.");
        System.out.println("2. Presentar solicitud de autorizacion.");
        System.out.println("3. Revisar solicitud como administrador.");
        System.out.println("4. Consultar resultado de solicitud.");
        System.out.println("5. Ingresar al parqueadero.");
        System.out.println("6. Salir del parqueadero.");
        System.out.println("7. Cerrar sesion.");
        System.out.println("0. Terminar el programa.");
        System.out.print("Opcion, ");

        return entrada.nextLine().trim();
    }
}