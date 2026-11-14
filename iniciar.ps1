[CmdletBinding()]
param(
    [string]$IpLan
)

$ErrorActionPreference = 'Stop'
$raiz = $PSScriptRoot
$jar = Join-Path $raiz 'Parqueadero-EPN.jar'

function Ejecutar($programa, [string[]]$argumentos) {
    & $programa @argumentos
    if ($LASTEXITCODE -ne 0) {
        throw "$programa termino con codigo $LASTEXITCODE."
    }
}

if (-not (Test-Path -LiteralPath $jar)) {
    throw 'Pon iniciar.ps1 en la misma carpeta que Parqueadero-EPN.jar.'
}

$configuracion = @('PARQUEADERO_HOST','PARQUEADERO_PUERTO','PARQUEADERO_TLS_ARCHIVO',
    'PARQUEADERO_TLS_CLAVE','PARQUEADERO_PROXY_HTTPS')
$anterior = @{}
foreach ($variable in $configuracion) {
    $anterior[$variable] = [Environment]::GetEnvironmentVariable($variable, 'Process')
}

try {
    if ($IpLan) {
        $ip = [Net.IPAddress]::Parse($IpLan)
        if ($ip.AddressFamily -ne [Net.Sockets.AddressFamily]::InterNetwork) {
            throw 'Usa una direccion IPv4 de esta computadora (consulta ipconfig).'
        }
        $IpLan = $ip.IPAddressToString
        # Los certificados se guardan en Windows, fuera de la carpeta sincronizada.
        $carpetaCertificados = Join-Path $env:LOCALAPPDATA 'Parqueadero-EPN\HTTPS'
        $local = Join-Path $carpetaCertificados $IpLan
        [IO.Directory]::CreateDirectory($local) | Out-Null
        $tls = Join-Path $local 'servidor.p12'
        $raizPublica = Join-Path $local 'parqueadero-epn-ca.cer'
        $archivoClave = Join-Path $local 'clave-tls.txt'

        if (-not (Test-Path -LiteralPath $tls) -or -not (Test-Path -LiteralPath $raizPublica) -or
                -not (Test-Path -LiteralPath $archivoClave)) {
            # Cada clave de demostracion se crea localmente. El ZIP no contiene certificados privados.
            $claveTls = [Guid]::NewGuid().ToString('N')
            $temporal = Join-Path $local ([Guid]::NewGuid().ToString('N'))
            [IO.Directory]::CreateDirectory($temporal) | Out-Null
            $ca = Join-Path $temporal 'ca.p12'
            $peticion = Join-Path $temporal 'servidor.csr'
            $respuesta = Join-Path $temporal 'servidor.cer'
            $servidorNuevo = Join-Path $temporal 'servidor.p12'
            $caPublica = Join-Path $temporal 'parqueadero-epn-ca.cer'
            $env:PARQUEADERO_TLS_CLAVE = $claveTls

            try {
                $almacen = @('-storetype','PKCS12','-storepass:env','PARQUEADERO_TLS_CLAVE')
                Ejecutar keytool (@('-genkeypair','-alias','epn-ca','-keyalg','RSA','-keysize','2048',
                    '-validity','90','-dname','CN=Parqueadero EPN (demostracion local)',
                    '-ext','bc:c=ca:true,pathlen:0','-ext','ku:c=keyCertSign,cRLSign','-keystore',$ca) + $almacen)
                Ejecutar keytool (@('-exportcert','-alias','epn-ca','-keystore',$ca,'-file',$caPublica) + $almacen)
                Ejecutar keytool (@('-genkeypair','-alias','epn-servidor','-keyalg','RSA','-keysize','2048',
                    '-validity','90','-dname',"CN=$IpLan",'-keystore',$servidorNuevo) + $almacen)
                Ejecutar keytool (@('-certreq','-alias','epn-servidor','-keystore',$servidorNuevo,
                    '-file',$peticion) + $almacen)
                Ejecutar keytool (@('-gencert','-alias','epn-ca','-keystore',$ca,'-infile',$peticion,
                    '-outfile',$respuesta,'-validity','90','-ext',"san=ip:$IpLan",'-ext','ku=digitalSignature,keyEncipherment',
                    '-ext','eku=serverAuth') + $almacen)
                Ejecutar keytool (@('-importcert','-noprompt','-alias','epn-ca','-keystore',$servidorNuevo,
                    '-file',$caPublica) + $almacen)
                Ejecutar keytool (@('-importcert','-noprompt','-alias','epn-servidor','-keystore',$servidorNuevo,
                    '-file',$respuesta) + $almacen)

                Copy-Item -LiteralPath $caPublica -Destination $raizPublica -Force
                Set-Content -LiteralPath $archivoClave -Value $claveTls -Encoding ascii
                Copy-Item -LiteralPath $servidorNuevo -Destination $tls -Force
            } finally {
                foreach ($archivo in @($ca,$peticion,$respuesta,$servidorNuevo,$caPublica)) {
                    if (Test-Path -LiteralPath $archivo -PathType Leaf) {
                        Remove-Item -LiteralPath $archivo -Force -ErrorAction SilentlyContinue
                    }
                }
                if (Test-Path -LiteralPath $temporal -PathType Container) {
                    Remove-Item -LiteralPath $temporal -ErrorAction SilentlyContinue
                }
            }
        }

        $env:PARQUEADERO_TLS_CLAVE = (Get-Content -Raw -LiteralPath $archivoClave).Trim()
        $env:PARQUEADERO_TLS_ARCHIVO = $tls
        $env:PARQUEADERO_HOST = $IpLan
        $env:PARQUEADERO_PUERTO = '8443'
        Write-Host "En el celular, agrega este certificado de demostracion: $raizPublica"
        Write-Host "Abre https://${IpLan}:8443 en la misma red Wi-Fi."
    } else {
        Write-Host 'Vista local en esta computadora: http://127.0.0.1:8080'
    }
    Ejecutar java @('-jar',$jar)
} finally {
    foreach ($variable in $configuracion) {
        [Environment]::SetEnvironmentVariable($variable, $anterior[$variable], 'Process')
    }
}
