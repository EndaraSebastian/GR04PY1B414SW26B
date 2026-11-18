package negocio;

import datos.UsuarioDAO;
import entidades.Usuario;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

//ServicioAcceso registra usuarios y mantiene la referencia al usuario de la sesion.
public class ServicioAcceso {

    private final UsuarioDAO usuarios;
    private Usuario usuarioActual;

    public ServicioAcceso(UsuarioDAO usuarios) {
        this.usuarios = Objects.requireNonNull(usuarios);
    }

    //La aplicacion entrega el usuario y la clave capturados por la vista de registro.
    public boolean registrarUsuario(Usuario u, String clave) {
        if (u == null || u.getCedula() == null || u.getCedula().isBlank()
                || clave == null || clave.isBlank()) {
            return false;
        }

        if (usuarios.buscarPorCedula(u.getCedula()) != null) {
            return false;
        }

        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);

        Base64.Encoder codificador = Base64.getEncoder();
        String salGuardada = codificador.encodeToString(sal);
        String hashGuardado = codificador.encodeToString(derivarClave(clave, sal));

        u.setClaveHash(salGuardada + "." + hashGuardado);
        usuarios.guardar(u);
        return true;
    }

    //UsuarioDAO devuelve la misma instancia que usan los servicios del parqueadero.
    public Usuario iniciarSesion(String cedula, String clave) {
        usuarioActual = null;

        if (cedula == null || clave == null || clave.isBlank()) {
            return null;
        }

        Usuario registrado = usuarios.buscarPorCedula(cedula);
        if (registrado == null || registrado.getClaveHash() == null) {
            return null;
        }

        String[] partes = registrado.getClaveHash().split("[.]", -1);
        if (partes.length != 2) {
            return null;
        }

        Base64.Decoder decodificador = Base64.getDecoder();
        byte[] sal = decodificador.decode(partes[0]);
        byte[] esperado = decodificador.decode(partes[1]);
        byte[] recibido = derivarClave(clave, sal);

        if (MessageDigest.isEqual(esperado, recibido)) {
            usuarioActual = registrado;
        }

        return usuarioActual;
    }

    //La aplicacion consulta la sesion antes de solicitar autorizacion, ingresar o salir.
    public Usuario obtenerUsuarioActual() {
        return usuarioActual;
    }

    public void cerrarSesion() {
        usuarioActual = null;
    }

    //El registro y el inicio de sesion usan el mismo procedimiento para procesar la clave.
    private byte[] derivarClave(String clave, byte[] sal) {
        PBEKeySpec datos = new PBEKeySpec(clave.toCharArray(), sal, 210000, 256);

        try {
            SecretKeyFactory fabrica = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return fabrica.generateSecret(datos).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo procesar la clave.", e);
        } finally {
            datos.clearPassword();
        }
    }
}