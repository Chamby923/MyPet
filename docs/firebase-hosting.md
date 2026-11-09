# Preparación de Firebase Hosting

## Estado verificado

Revisión realizada el 8 de noviembre de 2026. Android, `web/firebase-config.mjs` y `.firebaserc` apuntan a `mypet-pip`. La CLI global no está disponible. El equipo tiene Node 26.13.0 y npm 11.20.0. Se usaron Firebase CLI 15.0.0 y Node 22.23.4 en la caché de npm, sin instalación global.

El servicio REST real respondió HTTP 403 `PERMISSION_DENIED` a dos peticiones anónimas de solo lectura, una a un identificador ficticio de `publicPets` y otra al listado de esa colección. Se inspeccionaron únicamente los estados y códigos de error. Esto verifica conectividad y denegación. No demuestra que las reglas hayan caducado en una fecha concreta. El responsable comunicó el vencimiento del 12 de octubre de 2026. La revisión de las reglas vigentes queda pendiente.

La consulta de sitios de Hosting mediante la CLI requiere iniciar sesión. Por ello, el sitio remoto y su URL definitiva todavía están pendientes de confirmación.

## Configuración local

- `.firebaserc` establece `mypet-pip` como proyecto predeterminado. Los comandos de esta guía también lo indican explícitamente.
- `firebase.json` conserva `hosting.public: "web"` y añade el emulador de Hosting en `127.0.0.1:5000`.
- Las cabeceras incluyen `Referrer-Policy: no-referrer`, `X-Content-Type-Options: nosniff` y `Cache-Control: no-cache`. El patrón regular `.*` aplica las cabeceras también en el emulador de Windows. La prueba detectó que su normalización de rutas impedía aplicar el patrón glob anterior.
- `firebase.json` configura Hosting y selecciona `firestore.rules.proposed` para Firestore. `firestore.publicPets.proposed.rules` es una alternativa histórica no activa. El emulador de Firestore usa una configuración temporal independiente. Para publicar solo Hosting, conservar siempre `--only hosting`; no usar un despliegue general.
- No hay redirecciones o reescrituras de rutas. El lector usa el parámetro `p` sobre la página estática.

El lector hace un GET a `/v1/projects/mypet-pip/databases/(default)/documents/publicPets/{publicId}` en la API de Firestore. No consulta `pets`, `publicPetLinks` ni el listado de `publicPets`. Las reglas del servidor deben impedir los listados y las lecturas privadas aunque alguien construya otra petición.

Las llamadas REST sin sesión están sujetas a las reglas de Firestore. El flujo actual usa únicamente el identificador del proyecto, sin trasladar la configuración Android completa al navegador y sin necesitar otra aplicación web o credenciales Admin. [API REST de Firestore](https://firebase.google.com/docs/firestore/use-rest-api)

## Contrato entre Android y web

Android publica `active`, `name`, `breed`, `sex`, `age`, `traits`, `medicalAlert`, `status`, `photoUrl` y `contactPhone`. Los estados son `home`, `lost` y `found`. El REST de Firestore devuelve esos valores con sus tipos, como `stringValue` y `booleanValue`, y el lector extrae solo la lista permitida.

Android muestra la base fija `https://mypet-pip.web.app/`. El modelo Android y las reglas solo aceptan ese origen, con o sin la barra final de raíz, y generan siempre la versión con barra. Confirmar manualmente que corresponda al sitio real antes de publicar. Para un ID de 32 caracteres, Android genera:

```text
https://mypet-pip.web.app/?p=IDENTIFICADOR_PUBLICO
```

La página obtiene `p` con `URLSearchParams`. El QR conserva ese parámetro. Esta ruta fue verificada mediante prueba unitaria de Android y en Hosting local. La dirección remota es la prevista para el sitio predeterminado y debe confirmarse en Console o con la CLI antes de utilizarla.

Revocar reemplaza el documento público por `active: false` y vacía el enlace activo en la mascota mediante una transacción. La nueva publicación genera otro ID. Las pruebas de reglas comprobaron que el identificador revocado no puede reactivarse por ese flujo. La web usa `cache: "no-store"` al consultar Firestore y vuelve a consultar al regresar a la pestaña o restaurarla desde el historial. Descarta respuestas anteriores si ya se inició otra consulta.

La revocación se comprueba en cada nueva consulta. Una página que permanezca abierta y visible puede seguir mostrando su última lectura hasta actualizarla. Las capturas o copias guardadas por visitantes quedan fuera de ese control.

La propuesta estricta posterior, [firestore.rules.proposed](../firestore.rules.proposed), limita las lecturas a fichas activas. Las fichas desactivadas e inexistentes quedan denegadas también para el dueño en esta colección pública. La web muestra el mismo aviso de enlace inválido o ficha no disponible para documentos inexistentes, revocados o denegados. Los diagnósticos de permisos en consola incluyen solo la operación y el estado HTTP. Las pruebas de autorización actualizadas se describen en [el modelo de datos](database.md).

## Pruebas locales

Desde la raíz del proyecto:

```powershell
npm exec --yes --package=node@22.23.4 --package=firebase-tools@15.0.0 -- firebase emulators:start --only hosting --project mypet-pip --config firebase.json
```

Abrir `http://127.0.0.1:5000/?p=demo`. Los parámetros `demo-missing`, `demo-disabled` y `demo-error` muestran estados ficticios sin consultar Firebase.

Para repetir la verificación automática y cerrar el emulador al terminar:

```powershell
npm exec --yes --package=node@22.23.4 --package=firebase-tools@15.0.0 -- firebase emulators:exec --only hosting --project mypet-pip --config firebase.json "node tests/hosting.mjs"
```

Las pruebas necesitan Chrome y permiso para abrir conexiones locales. `tests/hosting.mjs` comprueba rutas, MIME de los módulos, cabeceras y respuestas 404 para archivos ajenos a `web/`. Después ejecuta Chromium contra ese Hosting local con respuestas de Firestore simuladas.

La CLI puede avisar que no consigue la configuración de inicialización del SDK web por falta de sesión. Esta página usa REST y su propio identificador, de modo que las pruebas no dependen de esa inicialización. npm puede emitir una advertencia al preparar paquetes desde Node 26; la ejecución de la CLI se verificó con el Node 22 temporal.

Los parámetros reales en el Hosting local consultan el proyecto real. Las pruebas automáticas interceptan esas peticiones en Chromium para usar fichas ficticias. El emulador de Hosting por sí mismo no redirige la API REST hacia Firestore Emulator. [Pruebas locales de Hosting](https://firebase.google.com/docs/hosting/test-preview-deploy)

## Revisión manual antes de publicar

1. En Firebase Console, seleccionar `mypet-pip` y verificar las reglas activas de Firestore. Comparar con la propuesta local, incluidos los permisos de `pets`, la relación privada y las operaciones atómicas. Aprobar un conjunto seguro después de revisar otros permisos superpuestos.
2. Confirmar en Hosting que el sitio de destino pertenece al proyecto y anotar su dominio. Revisar el contenido existente del canal de producción antes de autorizar un reemplazo.
3. Iniciar sesión en la CLI con una cuenta autorizada. Confirmar el sitio con el comando de consulta de abajo.
4. Cuando las reglas aprobadas estén activas, crear una mascota ficticia desde Android, publicar una ficha, consultar sin sesión, actualizar y revocar. Verificar que el enlace anterior siga desactivado tras otra publicación.
5. Confirmar que no se pueden listar fichas o leer datos privados sin autorización. Hasta entonces, el funcionamiento real de publicación y consulta permanece pendiente.

El error 403 actual impide verificar en Firebase real los estados de ficha disponible, inexistente y desactivada. Un 403 tampoco permite deducir si el documento solicitado existe.

## Comandos para la futura publicación

Ejecutar desde la raíz de MyPet. Estos comandos quedan documentados para cuando se autorice el despliegue:

```powershell
npm exec --yes --package=node@22.23.4 --package=firebase-tools@15.0.0 -- firebase login
npm exec --yes --package=node@22.23.4 --package=firebase-tools@15.0.0 -- firebase hosting:sites:list --project mypet-pip
node --test tests/web.test.mjs
npm exec --yes --package=node@22.23.4 --package=firebase-tools@15.0.0 -- firebase emulators:exec --only hosting --project mypet-pip --config firebase.json "node tests/hosting.mjs"
npm exec --yes --package=node@22.23.4 --package=firebase-tools@15.0.0 -- firebase deploy --only hosting --project mypet-pip --config firebase.json
```

El último comando publica el contenido del sitio de Hosting configurado. No incluye el despliegue de reglas de Firestore. Al finalizar una publicación autorizada, verificar el dominio que devuelva la CLI y probar de nuevo el recorrido real sin sesión. [Referencia de la CLI](https://firebase.google.com/docs/cli) · [Sitios y destinos de Hosting](https://firebase.google.com/docs/hosting/multisites)
