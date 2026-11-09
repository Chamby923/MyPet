# Arquitectura

## Android actual

`app/` es el único módulo Android. `MainActivity` monta la interfaz en Jetpack Compose y define las rutas de navegación. Las pantallas viven en `ui/screens`, los modelos en `data/model` y el estado de la interfaz en `ui/viewmodel`.

`AuthViewModel` usa Firebase Authentication para registrar cuentas e iniciar sesión. `PetViewModel` consulta `pets` por `ownerId` y escribe las fichas. El filtro del cliente delimita la consulta. La autorización efectiva depende de las reglas de Firestore, que deben revisarse en la consola. La escucha se reemplaza al volver a la lista y se cierra al cambiar de cuenta o destruir el ViewModel.

`PublicProfileCard` permite revisar y autorizar los datos públicos. `PublicPet` valida y convierte la lista permitida de campos en un documento. El modelo `Pet` incorpora `publicId` y `publicBaseUrl` con valores vacíos por defecto para documentos anteriores. El ID de Firestore usa el destino de anotación Kotlin de lectura recomendado. La edición de la ficha privada actualiza solo sus campos de formulario, para conservar los metadatos del enlace.

## Experiencia pública

La página de `web/` lee el parámetro `p` de la URL. La raíz y los identificadores de demo usan datos locales. Para un identificador real y con el proyecto configurado, el navegador usa la API REST de Firestore para solicitar un único documento de `publicPets` sin iniciar sesión. La aplicación web toma solo los campos de su lista permitida y muestra estados de carga, ficha disponible, inexistente, desactivada, error y configuración pendiente.

La configuración local de Hosting apunta a `web/`. El formato con parámetro de consulta permite servir el mismo HTML desde un servidor estático local y desde Hosting. Ningún recurso remoto es necesario para la demo. `share.mjs` genera el QR con una copia local de qrcode-generator 2.0.4, incluida con su licencia MIT.

## Publicación y revocación

El flujo previsto es **collar NTAG213 → URL HTTPS → página web → documento público**. El dueño publica desde Android con su sesión de Firebase. Una transacción lee `pets`, comprueba el dueño y escribe `publicPetLinks`, `publicPets` y los metadatos privados. Usa un UUID aleatorio en hexadecimal para el primer enlace y conserva el identificador al actualizarlo. Al revocarlo, reemplaza la ficha pública por `active: false` y vacía el enlace actual en la mascota. Una publicación posterior crea un identificador distinto.

El lector REST tiene configurado solo el identificador del mismo proyecto Firebase que Android. La configuración Android completa queda fuera de la web. Registrar MyPet Web en Console será necesario si se introduce el SDK web y resulta conveniente para la gestión del proyecto. El flujo REST actual no requiere trasladar claves Android ni usar credenciales Admin. La publicación de Hosting y los cambios de reglas requieren revisión antes de probar el recorrido real.

[Referencia de la API REST de Firestore](https://firebase.google.com/docs/firestore/use-rest-api)
