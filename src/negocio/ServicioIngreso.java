package negocio;

import datos.AutorizacionDAO;
import datos.ConstanciaDAO;
import datos.EspacioDAO;
import entidades.Constancia;
import entidades.Espacio;
import entidades.Usuario;

public class ServicioIngreso {
    private AutorizacionDAO autorizaciones;
    private ConstanciaDAO constancias;
    private EspacioDAO espacios;

    public ResultadoIngreso procesarIngreso(Usuario usuario, String qr) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    private boolean comprobarAutorizacion(Usuario usuario) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    private boolean tieneIngresoActivo(Usuario usuario) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    private Espacio asignarEspacio() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    private Constancia registrarIngreso(Usuario usuario, Espacio espacio) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
