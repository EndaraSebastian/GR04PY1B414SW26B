# Parqueadero EPN

Proyecto [1B] del Grupo 4 de la Escuela Politécnica Nacional. La aplicación permite solicitar y revisar autorizaciones para el parqueadero, registrar el ingreso con un código QR y liberar el puesto al registrar la salida.

El servidor se ejecuta en una computadora con Windows y JDK 21 o posterior. Los comandos `java` y `keytool` deben estar disponibles en PowerShell. La aplicación se usa desde el navegador del teléfono.

## Abrir la aplicación desde el teléfono

1. Conecta el teléfono al Wi-Fi del mismo router que usa la computadora. La computadora puede estar conectada por cable o por Wi-Fi.
2. Abre PowerShell en la carpeta donde están `Parqueadero-EPN.jar` e `iniciar.ps1`. Ejecuta `ipconfig` y busca **Dirección IPv4** en el adaptador que conecta la computadora al router (**Ethernet** si está por cable o **Wi-Fi** si está por Wi-Fi).
3. Ejecuta las tres líneas en la misma ventana. **Sustituye `192.168.100.151` por la IPv4 de la computadora que va a ejecutar el JAR.** Ese número es un ejemplo de la red donde se probó el proyecto. Si usas otra computadora o cambias de red, vuelve a consultar `ipconfig`.

   ```powershell
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy RemoteSigned -Force
   Unblock-File -LiteralPath .\iniciar.ps1
   .\iniciar.ps1 -IpLan 192.168.100.151
   ```

   La primera línea habilita la ejecución de scripts en esa ventana de PowerShell. La segunda desbloquea `iniciar.ps1` si se descargó de GitHub. La tercera inicia el JAR por HTTPS con la IP indicada.

4. Crea la clave de `ADMIN` cuando la terminal la pida. Cuando aparezca **Parqueadero EPN activo**, deja la terminal abierta.
5. Si es la primera vez que accedes a ese servidor desde el teléfono, transfiere el archivo `parqueadero-epn-ca.cer` cuya ruta indica PowerShell e instálalo como certificado de confianza. Abre en el teléfono la dirección HTTPS que muestra la terminal. El formato es `https://<IPv4>:8443/`.

## Probar el ingreso y la salida con QR

1. En el teléfono, registra un usuario como miembro de la EPN o visitante. Envía una solicitud y anota su número.
2. En la computadora, abre la aplicación, inicia sesión como `ADMIN` y aprueba la solicitud por su número.
3. En el navegador de la computadora, agrega `/codigos.html` al final de la dirección del servidor. Queda `https://<IPv4>:8443/codigos.html`, con la misma IPv4 del arranque. Esta página muestra los QR de **Ingreso** y **Salida**.
4. En el teléfono, entra con la cuenta aprobada. Selecciona **Ingresar**, escribe la placa registrada y pulsa **Abrir cámara**. Apunta al QR **Ingreso**. La aplicación debe mostrar el puesto asignado.
5. Vuelve al inicio, selecciona **Salir**, escribe la misma placa y escanea el QR **Salida**. La aplicación debe confirmar la salida y liberar el puesto.

También puedes usar **Usar una foto** para leer un QR guardado como imagen.

## Archivos del proyecto

- `src/` contiene el código Java.
- `web/` contiene las pantallas y el lector QR.
- `Parqueadero-EPN.jar` incluye el servidor y las pantallas.
- `iniciar.ps1` ejecuta el JAR por HTTPS para que puedas abrir la aplicación desde el teléfono.

Las cuentas, las solicitudes y los puestos se mantienen en memoria durante la ejecución. Al reiniciar el servidor, la demostración empieza con los datos vacíos.
