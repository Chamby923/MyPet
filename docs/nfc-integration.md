# Integración NFC

## Etiqueta y contenido

La etiqueta prevista es **NTAG213**, un NFC Forum Type 2 Tag con **144 bytes de memoria de usuario**. El mensaje NDEF y su encapsulado consumen parte de esa memoria. Comprobar la capacidad para la URL final al escribirla y verificarla en la etiqueta física. [Ficha oficial de NXP](https://www.nxp.com/products/NTAG213_215_216)

Grabar un solo registro de tipo URI con la URL `https://mypet-pip.web.app/?p=IDENTIFICADOR_PUBLICO`, después de confirmar el dominio del sitio en Firebase Console. Android tiene esa base fija y las reglas rechazan otros hosts, puertos, credenciales, rutas adicionales, parámetros y fragmentos en `publicBaseUrl`. La consulta `?p=...` se añade únicamente al generar el enlace final. La sesión CLI disponible no permitió confirmar el sitio remoto, por lo que la confirmación manual sigue pendiente. El collar guardará el enlace; el navegador obtendrá los datos de la ficha al abrirla.

## Enlace y QR implementados

La ficha pública genera un QR del enlace actual y permite descargarlo como SVG. En la demo local, la raíz se codifica con `?p=demo`. Para una ficha real, el QR conserva su identificador público. Android permite abrir y compartir el enlace después de que la transacción de publicación termine correctamente.

Para un celular sin NFC, usar la cámara o un lector QR y abrir la URL. El QR debe apuntar a una dirección accesible desde ese equipo. Una dirección LAN sirve durante una prueba en la misma red. El collar final requiere la URL HTTPS publicada.

La revocación desactiva futuras lecturas de la ficha asociada. Publicar de nuevo genera otro identificador y requiere actualizar el QR y, cuando esté disponible, la etiqueta del collar.

La prueba local de autorización comprueba la revocación completa por propietario, otra cuenta y visitante anónimo. Solo el propietario tiene éxito. Las reglas dejan el ID anterior sin acceso público, y la web presenta el mismo mensaje genérico para un enlace inexistente, revocado o denegado. Una lectura ficticia administrativa del emulador comprueba que el documento revocado contiene únicamente `active: false`. Esa comprobación no usa Firebase real.
