# Lista de pruebas para el martes 10 de noviembre de 2026

- [ ] Iniciar el servidor local siguiendo el README y abrir la ficha de Nube.
- [ ] Confirmar que la ficha se identifica como ficticia y muestra nombre, ilustración, raza, sexo, edad, características, alerta y estado.
- [ ] Confirmar que el botón de llamada queda oculto en la demo, que usa una mascota sin número de contacto.
- [ ] Revisar la ficha con una ventana de escritorio y con una vista móvil estrecha. Verificar que el contenido sea legible y que no haya desplazamiento horizontal.
- [ ] Probar los enlaces de ficha inexistente y desactivada: ambos deben mostrar «Este enlace es inválido o la ficha ya no está disponible». En el ejemplo de fallo de conexión o servidor, comprobar que se ofrece reintentar.
- [ ] Para simular la apertura en otro celular, usar un enlace o QR accesible desde ese equipo. `127.0.0.1` en el celular apunta al propio celular. Si la red del lugar permite la demo, servir solo `web/` en la red local, usar la dirección LAN del equipo anfitrión y comprobar el enlace en el celular antes de presentar. En caso contrario, usar la vista móvil del navegador en el equipo anfitrión.
- [ ] Abrir **Compartir esta ficha con un código QR**, descargar el SVG y comprobar su lectura desde el celular. El QR refleja el enlace del navegador. Antes de imprimirlo, abrir la web con la dirección LAN o HTTPS que se usará en la presentación.
- [ ] Ensayar la explicación de qué funciona hoy y qué queda pendiente de Firebase y del collar físico.

## Validaciones antes de una ficha real

- [ ] Revisar las reglas vigentes en Console, compararlas con la propuesta local probada y aprobar los cambios necesarios. Comprobar que ningún permiso más amplio exponga `pets` o `publicPetLinks`.
- [ ] Confirmar el proyecto de Firebase y el identificador configurado en `web/firebase-config.mjs`. Registrar MyPet Web si se incorporará el SDK web.
- [ ] Preparar Firebase Hosting y obtener una URL HTTPS. La configuración local ya apunta a `web/`. El despliegue requiere una autorización posterior.
- [ ] Confirmar manualmente en Console que el sitio usa `mypet-pip.web.app`. La consulta de metadatos con la sesión CLI disponible no pudo confirmar el dominio remoto.
- [ ] Verificar que Android muestre la base fija `https://mypet-pip.web.app/`. Las pruebas unitarias y de reglas deben rechazar hosts externos, subdominios, puertos, credenciales, rutas adicionales, parámetros y fragmentos.
- [ ] En Android, registrar una mascota de prueba, abrir su detalle y revisar la sección de ficha pública.
- [ ] Publicar sin teléfono, verificar en navegador sin sesión y confirmar que no aparece el botón de llamada.
- [ ] Publicar un teléfono de prueba autorizado y comprobar que el botón corresponde al número esperado, sin realizar llamadas innecesarias.
- [ ] Cambiar los datos privados, revisar y publicar la copia pública para que refleje la actualización.
- [ ] Desactivar el enlace, comprobar el aviso en la web y publicar de nuevo. El nuevo enlace debe ser distinto y el anterior debe seguir desactivado.
- [ ] Comprobar el recorrido con otra cuenta, sin permisos sobre la mascota.
- [ ] Repetir la suite del emulador: la transacción completa de revocación debe permitirse al dueño y denegarse a otra cuenta y al visitante. Los intentos denegados deben conservar la ficha activa. El marcador final debe tener solo `active: false`, y el ID anterior no debe poder reutilizarse.
- [ ] Revisar el diagnóstico de permisos en consola. Debe incluir solo la operación y el estado HTTP, sin identificadores públicos, datos personales ni contenido de documentos.

## Resultado local de esta etapa

Se verificaron la compilación Android y las pruebas unitarias, los estados y el QR web en Chromium a 320, 390 y 1280 píxeles, y las reglas propuestas en Firestore Emulator con datos ficticios. Las comprobaciones de Console, Hosting real, dispositivos y collar físico de esta lista siguen pendientes.
