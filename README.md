# Parqueadero EPN

Proyecto [1B] del Grupo 4 de la Escuela Politécnica Nacional. La aplicación permite solicitar y revisar autorizaciones para el parqueadero, registrar el ingreso con un código QR y liberar el puesto al registrar la salida.

El servidor se ejecuta en una computadora con Windows y JDK 21 o posterior. Los comandos `java` y `keytool` deben estar disponibles en PowerShell. La aplicación se usa desde el navegador del teléfono.

## Consultar la IP de la computadora

Conecta el teléfono al Wi-Fi del mismo router que usa la computadora. La computadora puede estar conectada por cable o por Wi-Fi.

En PowerShell, ejecuta:

```powershell
ipconfig
```

Busca el adaptador que corresponde a tu conexión:

- Si la computadora está conectada por Wi-Fi, busca **Adaptador de LAN inalámbrica Wi-Fi**.
- Si está conectada por cable, busca **Adaptador de Ethernet Ethernet**.

Dentro de ese adaptador, copia el número que aparece junto a **Dirección IPv4**. Por ejemplo:

```text
Adaptador de LAN inalámbrica Wi-Fi:

   Dirección IPv4. . . . . . . . . . . . : 10.130.8.50
```

En este ejemplo, la IP que se debe usar es `10.130.8.50`.

Para esta conexión, utiliza la IPv4 del adaptador Wi-Fi o Ethernet activo. Las direcciones de Tailscale, WSL u otros adaptadores virtuales corresponden a conexiones diferentes. Tampoco se debe copiar la dirección IPv6 ni la puerta de enlace.

## Ejecución local en la computadora

Abre PowerShell en la carpeta donde está `Parqueadero-EPN.jar` y ejecuta:

```powershell
java -jar .\Parqueadero-EPN.jar
```

Crea la clave de `ADMIN` cuando la terminal la solicite. Después, abre en el navegador de la computadora:

```text
http://127.0.0.1:8080/
```

Mantén la terminal abierta mientras usas la aplicación. Para detenerla, presiona `Ctrl + C`.

Esta ejecución permite acceder únicamente desde la misma computadora. Para usar la aplicación desde el teléfono, sigue las instrucciones de las siguientes secciones.

## Iniciar la aplicación

Abre PowerShell en la carpeta donde están `Parqueadero-EPN.jar` e `iniciar.ps1`.

Ejecuta las siguientes líneas en la misma ventana. Sustituye `10.130.8.50` por la IPv4 que encontraste en tu computadora:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy RemoteSigned -Force
Unblock-File -LiteralPath .\iniciar.ps1
.\iniciar.ps1 -IpLan 10.130.8.50
```

La primera línea habilita la ejecución de scripts durante esa sesión de PowerShell. La segunda desbloquea el archivo si se descargó de GitHub. La tercera inicia el servidor por HTTPS usando la IP indicada.

Crea la clave de `ADMIN` cuando la terminal la pida. Cuando aparezca **Parqueadero EPN activo**, deja abierta esa ventana para mantener la aplicación funcionando.

En el teléfono, abre la dirección que muestre la terminal. Con la IP del ejemplo sería:

```text
https://10.130.8.50:8443/
```

## Preparar el teléfono para usar la cámara

El script muestra la ubicación del archivo `parqueadero-epn-ca.cer`. Transfiere ese archivo al teléfono e instálalo como certificado de confianza desde los ajustes del dispositivo. La ubicación de esta opción depende del teléfono.

Después, abre la dirección HTTPS de la aplicación y permite el acceso a la cámara cuando el navegador lo solicite.

Si cambias de computadora o de IP, utiliza el certificado que indique el script para ese servidor.

## Si la página no carga en el teléfono

Comprueba que:

- El teléfono y la computadora estén conectados a la misma red.
- La terminal del servidor siga abierta y no muestre errores.
- La dirección del navegador tenga `https://`, la IPv4 correcta y el puerto `8443`.

Si Windows bloquea la conexión, hace falta permitir el acceso en el firewall. Puede ocurrir cuando la red está marcada como pública y Java solo tiene permiso para redes privadas.

Deja abierta la aplicación y abre otra ventana de **PowerShell como administrador**. Ejecuta:

```powershell
$rutaJavaParqueadero = (Get-Command java.exe -ErrorAction Stop).Source

New-NetFirewallRule -DisplayName "Parqueadero EPN HTTPS" `
  -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8443 `
  -Program $rutaJavaParqueadero `
  -Profile Public,Private -RemoteAddress LocalSubnet
```

El comando obtiene la ruta de Java de esa computadora y permite conexiones al puerto `8443` desde la red local. Por eso también se puede usar en otra computadora sin escribir la ruta de instalación de Java manualmente.

Este paso requiere permisos de administrador y normalmente se realiza una sola vez. Si cambia la ubicación de Java, habrá que actualizar el permiso.

Después, vuelve a abrir la dirección de la aplicación en el teléfono.

## Actualizar la IP al cambiar de red o de computadora

La IP puede cambiar al conectarse a otra red. Para actualizarla:

1. Detén el servidor con `Ctrl + C` en la terminal donde está ejecutándose.
2. Ejecuta `ipconfig` y consulta nuevamente la IPv4 del adaptador activo.
3. Inicia la aplicación colocando la nueva IP después de `-IpLan`.
4. Abre en el teléfono la dirección HTTPS con esa misma IP.
5. Utiliza el certificado que indique el script para la nueva dirección.

La IP se indica al ejecutar `iniciar.ps1` y al escribir la dirección en el navegador.

Si el proyecto se ejecuta en la computadora del docente, se debe consultar la IPv4 de esa computadora y seguir estos mismos pasos. Los números de este README son ejemplos.

## Probar el ingreso y la salida con QR

1. En el teléfono, registra un usuario como miembro de la EPN o visitante. Envía una solicitud y anota su número.
2. En la computadora, abre la aplicación, inicia sesión como `ADMIN` y aprueba la solicitud por su número.
3. En el navegador de la computadora, agrega `/codigos.html` a la dirección del servidor. Con la IP del ejemplo sería:

   ```text
   https://10.130.8.50:8443/codigos.html
   ```

   Esta página muestra los QR de **Ingreso** y **Salida**.

4. En el teléfono, entra con la cuenta aprobada. Selecciona **Ingresar**, escribe la placa registrada y pulsa **Abrir cámara**. Apunta al QR de **Ingreso**. La aplicación debe mostrar el puesto asignado.
5. Vuelve al inicio, selecciona **Salir**, escribe la misma placa y escanea el QR de **Salida**. La aplicación debe confirmar la salida y liberar el puesto.

También puedes usar **Usar una foto** para leer un QR guardado como imagen.

## Archivos del proyecto

- `src/` contiene el código Java.
- `web/` contiene las pantallas y el lector QR.
- `Parqueadero-EPN.jar` incluye el servidor y las pantallas.
- `iniciar.ps1` ejecuta el JAR por HTTPS para acceder desde el teléfono.

Las cuentas, solicitudes, autorizaciones y registros de ingreso se mantienen en memoria durante la ejecución. Al reiniciar el servidor, se pierden esos datos y los puestos vuelven a su estado inicial.