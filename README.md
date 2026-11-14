# Parqueadero EPN

Proyecto [1B] del Grupo 4 de la Escuela Politécnica Nacional. La aplicación permite solicitar y revisar autorizaciones para el parqueadero, registrar el ingreso con un código QR y liberar el puesto al registrar la salida.

El servidor se ejecuta en una computadora con Windows y JDK 21 o posterior. La aplicación se usa desde el navegador del teléfono.

## Abrir la aplicación desde el teléfono

1. Conecta el teléfono al Wi-Fi del mismo router que usa la computadora.
2. Abre PowerShell en la carpeta donde están `Parqueadero-EPN.jar` e `iniciar.ps1`. Usa `ipconfig` para identificar la dirección IPv4 de la computadora.
3. Ejecuta este comando con la IPv4 de tu computadora:

   ```powershell
   .\iniciar.ps1 -IpLan 192.168.100.151
   ```

4. Crea la clave de `ADMIN` cuando la terminal la pida. Cuando aparezca **Parqueadero EPN activo**, deja la terminal abierta.
5. Transfiere al teléfono el archivo `parqueadero-epn-ca.cer` cuya ruta indica PowerShell. Instálalo como certificado de confianza en el teléfono y abre la dirección HTTPS que muestra la terminal. Con la IP del ejemplo, la dirección es `https://192.168.100.151:8443/`.

Si PowerShell indica que la ejecución de scripts está deshabilitada, ejecuta estos comandos en la misma ventana y vuelve a iniciar la aplicación:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy RemoteSigned
Unblock-File -LiteralPath .\iniciar.ps1
```

## Probar el ingreso y la salida con QR

1. En el teléfono, registra un usuario como miembro de la EPN o visitante. Envía una solicitud y anota su número.
2. En la computadora, abre la aplicación, inicia sesión como `ADMIN` y aprueba la solicitud por su número.
3. En el navegador de la computadora, abre `https://192.168.100.151:8443/codigos.html`, usando la misma IP del arranque. Esta página muestra los QR de **Ingreso** y **Salida**.
4. En el teléfono, entra con la cuenta aprobada. Selecciona **Ingresar**, escribe la placa registrada y pulsa **Abrir cámara**. Apunta al QR **Ingreso**. La aplicación debe mostrar el puesto asignado.
5. Vuelve al inicio, selecciona **Salir**, escribe la misma placa y escanea el QR **Salida**. La aplicación debe confirmar la salida y liberar el puesto.

También puedes usar **Usar una foto** para leer un QR guardado como imagen.

## Archivos del proyecto

- `src/` contiene el código Java.
- `web/` contiene las pantallas y el lector QR.
- `Parqueadero-EPN.jar` incluye el servidor y las pantallas.
- `iniciar.ps1` ejecuta el JAR por HTTPS para que puedas abrir la aplicación desde el teléfono.

Las cuentas, las solicitudes y los puestos se mantienen en memoria durante la ejecución. Al reiniciar el servidor, la demostración empieza con los datos vacíos.