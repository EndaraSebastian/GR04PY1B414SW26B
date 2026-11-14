package presentacion;

import entidades.Espacio;

//La vista muestra el resultado que la aplicacion recibe de ServicioIngreso.
public class UI_Asignacion {

    //La aplicacion obtiene el espacio desde la constancia del ingreso.
    public void mostrarEspacio(Espacio espacio) {
        System.out.println("Espacio asignado, " + espacio.getIdEspacio() + ".");
    }

    public void mostrarSinCupo() {
        System.out.println("No hay espacios libres.");
    }

    //El motivo distingue un ingreso repetido de una autorizacion inexistente.
    public void mostrarRechazo(String motivo) {
        System.out.println(motivo);
    }
}