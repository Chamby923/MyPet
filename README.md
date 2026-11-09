# MyPet

MyPet es un Proyecto de Investigación Personal para ayudar a identificar a perros perdidos. La idea es que una persona acerque su celular a un collar NFC y consulte una ficha con información de la mascota y un contacto autorizado.

## Estado del proyecto

- Android usa Kotlin, Jetpack Compose, Firebase Authentication y Cloud Firestore. El código incluye registro e inicio de sesión, alta de mascotas, edición y cambio de estado.
- La ficha web de demostración funciona localmente con datos ficticios y sin una cuenta. Usa HTML, CSS y JavaScript, sin dependencias de instalación.
- Android incluye un formulario de publicación con consentimiento, teléfono opcional, enlace compartible y revocación. Las escrituras se realizan en una transacción que mantiene la relación privada y la ficha pública.
- La web consulta un documento de `publicPets` por identificador mediante Firestore REST y genera un QR local descargable. El identificador del proyecto se obtuvo de la configuración Android. El lector REST usa únicamente ese identificador.
- Las reglas propuestas se verificaron con datos ficticios en un emulador local. Quedan pendientes revisar las reglas vigentes, integrar las aprobadas, publicar Hosting y probar el recorrido real en Firebase y Android.
- El chip NTAG213 aún no está disponible. La presentación del martes 10 de noviembre de 2026 puede simular la apertura con un enlace.

## Preparación de Firebase Hosting

El proyecto predeterminado de `.firebaserc` es `mypet-pip`. Coincide con Android y con el lector REST. Las consultas anónimas de prueba a un documento y al listado de `publicPets` devolvieron HTTP 403 `PERMISSION_DENIED` en el proyecto real. La fecha de vencimiento de las reglas fue comunicada por el responsable del proyecto; las reglas remotas quedan pendientes de revisión.

Hosting se verificó localmente con Firebase CLI 15.0.0 y Node 22.23.4 temporal. Sirve únicamente `web/`. La CLI necesita iniciar sesión para confirmar el sitio remoto. El despliegue y el recorrido real Android → Firestore → navegador siguen pendientes. El lector REST actual funciona con el identificador del proyecto y no requiere registrar otra aplicación web.

Consulta [la guía de Hosting](docs/firebase-hosting.md) para repetir las pruebas, revisar los permisos y publicar cuando se autorice.

La propuesta estricta vigente está en [firestore.rules.proposed](firestore.rules.proposed). Solo permite lectura pública de fichas activas. La web muestra el mismo aviso de enlace inválido o ficha no disponible para fichas inactivas, inexistentes o denegadas. Consulta [el modelo y la matriz de autorización](docs/database.md) antes de integrar las reglas.

## Abrir la demostración

Desde la raíz de MyPet, ejecuta:

```powershell
python -m http.server 8000 --bind 127.0.0.1 --directory web
```

| Prueba | Enlace local |
| --- | --- |
| Ficha ficticia de Nube | [Abrir demo](http://127.0.0.1:8000/) |
| Perfil inexistente | [Abrir ejemplo](http://127.0.0.1:8000/?p=demo-missing) |
| Perfil desactivado | [Abrir ejemplo](http://127.0.0.1:8000/?p=demo-disabled) |
| Error y reintento | [Abrir ejemplo](http://127.0.0.1:8000/?p=demo-error) |

Usa `Ctrl+C` para detener el servidor. Para un QR que se abra en otro celular, sigue la [lista de preparación](docs/demo-checklist.md).

Para probar en un celular conectado a la misma red, detener el servidor anterior y ejecutar:

```powershell
python -m http.server 8000 --bind 0.0.0.0 --directory web
```

Consultar la dirección IPv4 de la red del equipo con `ipconfig` y abrir `http://IP_LOCAL:8000/?p=demo` desde el celular. Si Windows solicita acceso de red, limitarlo a la red privada usada para la prueba. Abrir **Compartir esta ficha con un código QR** después de acceder por esa dirección. El QR también permite descargar un SVG para la presentación. Este servidor temporal sirve únicamente la carpeta `web/`.

## Publicar desde Android

1. Con las reglas y Hosting revisados y configurados, abrir una mascota y elegir **Revisar datos para publicar**.
2. Confirmar que el dominio de Hosting sea `mypet-pip.web.app`. Android muestra esa dirección fija. Revisar nombre, raza, sexo, características, estado, foto opcional y edad aproximada.
3. Escribir únicamente la alerta médica que se autoriza compartir. Marcar la autorización del teléfono solo si se desea publicarlo, con formato internacional.
4. Autorizar los campos revisados y pulsar **Publicar datos revisados**. Abrir el enlace y comprobarlo en un navegador sin sesión.
5. Usar **Desactivar enlace** para revocarlo. Una publicación posterior usa otro identificador.

La edición privada y el cambio de estado mantienen su funcionamiento. Para actualizar la copia pública, volver a revisar y publicar los campos. Cada publicación exige consentimiento. Los campos opcionales sin autorizar se omiten o quedan vacíos en la copia pública.

## Verificaciones locales

```powershell
node --test tests/web.test.mjs
node tests/browser.mjs
.\gradlew.bat assembleDebug testDebugUnitTest
```

La prueba de navegador necesita Google Chrome instalado y permiso para usar un servidor local. `CHROME_BINARY` permite indicar otro ejecutable compatible. La prueba usa respuestas ficticias de Firestore y guarda capturas en una carpeta temporal.

La [prueba de reglas](tests/firestore.mjs) debe ejecutarse con Firestore Emulator en `127.0.0.1:8088`, proyecto `demo-mypet`. Consulta [la preparación del emulador](docs/database.md). Las pruebas locales no sustituyen la comprobación final con las reglas aprobadas en el proyecto real.

## Documentación

- [Arquitectura](docs/architecture.md)
- [Modelo de datos y reglas propuestas](docs/database.md)
- [Integración NFC](docs/nfc-integration.md)
- [Lista de pruebas para la demo](docs/demo-checklist.md)
- [Guion de presentación](docs/presentation.md)
- [Configuración, pruebas y publicación de Hosting](docs/firebase-hosting.md)
