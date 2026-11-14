package presentacion;

import entidades.Usuario;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

//La vista de registro construye el usuario que la aplicacion entrega a ServicioAcceso.
public class UI_SignUp {

    private final Scanner entrada;

    public UI_SignUp(Scanner entrada) {
        this.entrada = Objects.requireNonNull(entrada);
    }

    public Usuario capturarDatosRegistro() {
        String cedula = leerDato("Ingrese su cedula.");
        String nombre = leerDato("Ingrese su nombre.");
        String tipo = leerDato("Ingrese el tipo de usuario, MCP o VISITANTE.")
                .toUpperCase(Locale.ROOT);

        if (!"MCP".equals(tipo) && !"VISITANTE".equals(tipo)) {
            throw new IllegalArgumentException("El tipo debe ser MCP o VISITANTE.");
        }

        String placa = leerDato("Ingrese la placa del vehiculo.")
                .toUpperCase(Locale.ROOT);
        String tipoVehiculo = leerDato("Ingrese el tipo de vehiculo (CARRO, MOTO).")
                .toUpperCase(Locale.ROOT);

        return new Usuario(cedula, nombre, tipo, placa, tipoVehiculo);
    }

    public String capturarClave() {
        System.out.println("Ingrese una clave para su cuenta.");
        return entrada.nextLine();
    }

    public void mostrarResultado(String mensaje) {
        System.out.println(mensaje);
    }

    private String leerDato(String mensaje) {
        System.out.println(mensaje);
        String valor = entrada.nextLine().trim();

        if (valor.isEmpty()) {
            throw new IllegalArgumentException("Complete el dato solicitado.");
        }

        return valor;
    }
}