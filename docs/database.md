# Modelo privado, ficha pública y autorización

## Esquema Android comprobado

El modelo es `app/src/main/java/com/example/mypet/data/model/Pet.kt`. Sus campos persistidos son:

| Grupo | Campos reales |
| --- | --- |
| Mascota | `name`, `photoUrl`, `breed`, `sex`, `birthDate`, `color`, `status` |
| Dueño y contacto privado | `ownerId`, `ownerName`, `phone`, `ownerEmail` |
| Información adicional privada | `vaccines`, `allergies`, `observations` |
| Enlace público actual | `publicId`, `publicBaseUrl` |

Todos son cadenas. `status` usa «En casa», «Perdida» o «Encontrada». La propiedad Kotlin `id` recibe el identificador del documento mediante `@set:DocumentId` y no se guarda como campo.

`PetViewModel` realiza estas operaciones:

| Operación | Implementación y compatibilidad |
| --- | --- |
| Crear | `addPet` usa `pets.add(pet.copy(ownerId = uid))`. El UID procede de Firebase Authentication. La propuesta exige que coincida con la sesión. |
| Consultar | `fetchPets` usa `whereEqualTo("ownerId", uid)` con una escucha. Ese filtro satisface la regla de propietario. Una consulta sin el filtro se deniega. |
| Leer para publicar o revocar | La transacción obtiene un documento de `pets` y comprueba su `ownerId`. La propuesta permite esa lectura únicamente al dueño. |
| Editar | `updatePet` actualiza nombre, raza, sexo, fecha o edad, color, nombre del dueño, teléfono, vacunas, alergias y observaciones. Conserva el propietario y los metadatos públicos. |
| Cambiar estado | `updatePetStatus` actualiza solo `status`. |
| Eliminar | No se encontró una llamada a `delete` ni una pantalla de eliminación. La propuesta deniega el borrado incluso al dueño. |

La lista permitida privada contiene exactamente esos 16 campos persistidos. La propuesta valida sus tipos y valores de estado. Exige `ownerId` y `name`, y admite que los documentos antiguos omitan otros campos, incluidos los metadatos públicos. Los valores opcionales se comprueban con los mismos valores vacíos predeterminados del modelo.

Un documento histórico con campos adicionales, tipos distintos, propietario ausente o un estado desconocido puede leerse por su dueño si tiene un `ownerId` válido, pero sus escrituras quedarán denegadas hasta una revisión individual. No se inspeccionaron ni modificaron documentos reales para verificar esos casos.

## Separación pública

`publicPets/{publicId}` contiene una copia revisada y autorizada por el dueño. `PublicPet.toDocument()` produce únicamente:

| Campo | Tipo y validación propuesta |
| --- | --- |
| `active` | Booleano. Debe ser verdadero en una ficha publicada. |
| `name` | Cadena no vacía ni compuesta solo por espacios, hasta 100 caracteres. |
| `breed`, `sex`, `age` | Cadenas opcionales de hasta 100 caracteres. |
| `traits` | Cadena opcional de hasta 1000 caracteres. |
| `medicalAlert` | Cadena opcional de hasta 500 caracteres, autorizada por el dueño. |
| `status` | `home`, `lost` o `found`. |
| `photoUrl` | Cadena vacía o URL HTTPS sin credenciales en la autoridad, hasta 2048 caracteres. |
| `contactPhone` | Vacío o teléfono internacional con `+` y de 8 a 15 dígitos. Se publica solo si el dueño lo autoriza en Android. |

Las reglas rechazan campos adicionales, incluidos `ownerId`, `ownerEmail`, `ownerName`, `petId`, `phone`, `birthDate`, `vaccines`, `allergies` y `observations`. El nombre de teléfono público real es `contactPhone`. Las estructuras anidadas que sustituyan las cadenas también se rechazan.

Las reglas impiden añadir campos privados a la estructura pública. El texto libre requiere revisión del dueño: una regla de tipos no puede reconocer todos los datos personales que una persona escriba dentro de `traits` o `medicalAlert`. Una URL de foto autorizada también debe revisarse antes de compartirla.

## Relación privada y revocación

Android genera un UUID v4 de 32 caracteres hexadecimales. `publicPetLinks/{publicId}` guarda únicamente `ownerId` y `petId`. El visitante no puede consultar esa relación. Las reglas permiten al dueño comprobar si un ID nuevo está libre, como hace la transacción Android.

Publicar escribe atómicamente la relación privada, el documento público y `publicId`/`publicBaseUrl` de la mascota. Las reglas comprueban la propiedad contra el documento privado y la coherencia final con `getAfter`. El propietario y el vínculo existente son inmutables.

Revocar sustituye el documento público por el marcador exacto `{active: false}` y vacía el enlace actual de la mascota en la misma transacción. La relación privada permanece para impedir reutilizar el identificador. Una publicación posterior necesita uno nuevo. Las escrituras y lecturas de subcolecciones no declaradas quedan denegadas.

## Propuesta vigente para revisión

El archivo configurado en `firebase.json` es [firestore.rules.proposed](../firestore.rules.proposed). La propuesta histórica `firestore.publicPets.proposed.rules` conserva el diseño previo, no está activa en esa configuración y no debe usarse para publicar esta política de lectura estricta.

| Actor | pets | publicPets activa | publicPets inactiva o inexistente | publicPetLinks |
| --- | --- | --- | --- | --- |
| Propietario autenticado | Crear, leer y editar sus documentos. Borrado denegado. | Lectura individual. Escrituras válidas y coherentes con la mascota. | Lectura denegada. Puede revocar y publicar otro ID mediante transacciones. | Lectura individual propia. Creación coherente y escritura idéntica del vínculo existente. |
| Otro usuario autenticado | Acceso a la mascota ajena denegado. Puede consultar sus propias mascotas con el filtro de UID. | Lectura individual. Escrituras ajenas denegadas. | Lectura denegada. | Acceso ajeno denegado. |
| Visitante anónimo | Denegado. | Lectura individual. | Denegado. | Denegado. |

Los listados de `publicPets` y `publicPetLinks` y las eliminaciones se deniegan para todos los clientes. No hay reglas universales ni permisos por fecha. `firebase.json` selecciona `firestore.rules.proposed`; cualquier futura publicación requiere revisión y autorización. Esta descripción de la configuración local no comprueba el estado remoto.

## Compatibilidad y efecto en la web

La publicación, la actualización, la consulta filtrada y la revocación actuales de Android son compatibles con las reglas probadas. Los campos `publicBaseUrl` y `publicId` deben estar ambos vacíos o ambos configurados con un ID válido y la base autorizada.

Android y `validBaseUrl()` admiten únicamente `https://mypet-pip.web.app` o `https://mypet-pip.web.app/` para una ficha publicada. Android normaliza siempre el enlace a `https://mypet-pip.web.app/?p=ID`. El formulario muestra la base fija, sin edición. Se rechazan otros hosts, subdominios, puertos explícitos, credenciales, rutas adicionales, parámetros y fragmentos. El valor vacío se conserva solo para una mascota sin enlace activo. La URL de la foto es un campo distinto y puede usar otro origen HTTPS autorizado por el dueño.

Si un documento histórico conserva una base externa, una edición que deje ese valor en el documento será rechazada por las nuevas reglas. La publicación revisada desde el formulario actual puede guardar la base autorizada junto con la ficha pública. Antes de aplicar reglas reales, revisar este caso sin migrar ni alterar automáticamente los documentos existentes.

El proyecto configurado es `mypet-pip`, pero la consulta de metadatos con la sesión CLI disponible no confirmó el sitio remoto. Confirmar manualmente en Firebase Console que Hosting tenga el dominio `mypet-pip.web.app` antes de publicar reglas, sitio o enlaces para el collar.

El lector web consulta un documento de `publicPets` por ID mediante REST y sin sesión. Tiene configurado `mypet-pip`. No consulta `pets` ni necesita otra aplicación web o claves de Admin para ese recorrido. Quedan pendientes autenticarse en la CLI, confirmar el sitio de Hosting y probar en el proyecto real tras aprobar las reglas.

Con esta política, una lectura anónima de una ficha inactiva o inexistente devuelve 403. La web muestra el mismo mensaje para denegaciones 401/403, documentos inexistentes 404, marcadores inactivos y los ejemplos ficticios correspondientes: «Este enlace es inválido o la ficha ya no está disponible». Los fallos de conexión o de servidor conservan un mensaje de reintento. La consola registra la operación `publicPets.get` y el estado HTTP cuando hay una denegación. El diagnóstico no incluye ID, URL de consulta, datos del documento ni el cuerpo de error recibido.

## Pruebas de autorización

`tests/firestore.mjs` carga exclusivamente `firestore.rules.proposed` en `demo-mypet`, con Firestore Emulator en `127.0.0.1:8088`. Usa cuentas y datos ficticios. Comprueba:

- Alta y consulta propia, filtro de `ownerId`, edición y cambio de estado.
- Denegación de acceso ajeno, cambio de propietario, tipos incorrectos, campos privados desconocidos y borrado.
- Compatibilidad de documentos previos sin los campos opcionales.
- Publicación atómica y protección del vínculo privado.
- Lectura pública activa, denegación de listados y escrituras anónimas o ajenas.
- Validación de tipos, longitudes, foto, teléfono y campos públicos permitidos.
- Revocación atómica, denegación de lectura del ID anterior y nueva publicación con ID diferente.
- Transacción completa de revocación del propietario permitida, y la misma transacción denegada para otra cuenta y para el visitante anónimo. Después de cada intento denegado se comprueba que la ficha siga activa y vinculada. Una lectura administrativa exclusiva del emulador verifica que el marcador final contenga exactamente `active: false`.
- Dominio autorizado con y sin barra final permitido, y variantes de host, puerto, ruta, credenciales, parámetros y fragmentos denegadas.

La suite se ejecutó correctamente el 8 de noviembre de 2026 con Firebase CLI 15.0.0, Node 22.23.4 y JDK 21. Las pruebas compilaron las reglas en el emulador y terminaron con código de salida 0. No se ejecutaron contra el proyecto real.

Para repetir las pruebas, crear una configuración temporal con una ruta absoluta a `firestore.rules.proposed`, puerto 8088 y UI desactivada. Mantener JDK 21 disponible. Desde esa carpeta temporal:

```powershell
npm exec --yes --package=node@22.23.4 --package=firebase-tools@15.0.0 -- firebase emulators:exec --only firestore --project demo-mypet --config firebase.json "node RUTA_ABSOLUTA_A_MYPET/tests/firestore.mjs"
```

Ejemplo de la configuración temporal:

```json
{
  "firestore": { "rules": "RUTA_ABSOLUTA_A_MYPET/firestore.rules.proposed" },
  "emulators": {
    "firestore": { "host": "127.0.0.1", "port": 8088 },
    "ui": { "enabled": false },
    "singleProjectMode": true
  }
}
```

En Windows, usar barras `/` en las rutas JSON. La configuración temporal sirve solo para el emulador y se mantiene separada de la configuración de Hosting.

[Reglas de campos](https://firebase.google.com/docs/firestore/security/rules-fields) · [Reglas y consultas](https://firebase.google.com/docs/firestore/security/rules-query) · [Validación de transacciones](https://firebase.google.com/docs/firestore/manage-data/transactions)
