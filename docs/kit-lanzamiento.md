# Kit de lanzamiento (servicio, desde el 9-11-2026)

Proyecto de dos semanas para cobrar ya: fichas y capturas de Play hechas para otros devs, con la misma
plantilla que las apps propias. Pagina: https://testers.baltajmn.dev/launch/ (en) y /es/lanzamiento/ (es),
enlazada desde el menu de todas las paginas y desde las guias de acceso rechazado.

- Kit de ficha 49 EUR: hasta 8 capturas con titular (1080x2160, App Store si lo pide), grafico 1024x500,
  titulo y descripciones, hasta 13 idiomas, zip en 3 dias laborables, una ronda de cambios.
- Kit de lanzamiento 99 EUR: lo anterior + subida a su Play Console + borrador de las respuestas de acceso a
  produccion + revision de su prueba cerrada.
- Flujo: pide por correo (baltax.75@gmail.com) con capturas; en 24 h se confirma que sirven y se manda el
  enlace de pago; se cobra antes de empezar. Traducciones: la pagina dice que no son de nativos.
- Nunca: testers, instalaciones, resenas ni promesa de aprobacion.

Corte: dia 7 (16-11). Con 0 ventas y menos de 3 interesados, se cierra y se vuelve a opobot.
Meta: 4 ventas al 23-11.

## Pendiente del usuario

1. Cuenta de cobro (Stripe, o PayPal si ya la tiene) y dos enlaces de pago fijos, 49 y 99 EUR. Crear la
   cuenta es suyo; con la cuenta hecha, los enlaces se pueden crear desde el navegador con su si.
2. Alta de autonomo / IVA: consultarlo con quien le lleve los impuestos antes de la primera factura.

## Receta por pedido (se afina con el primero)

1. Crudas en `~/AndroidStudioProjects/kit/<app>/store/screenshots/raw/android/<idioma>/`; la barra de estado
   se quita con `crop.from`.
2. Entrada en `gk/contenido/plantilla/tienda/apps.js` con `stores: ['play']` (y App Store si lo pide), estilo
   propio en `estilos.js` con los colores de su icono, titulares en `textos.js` con sonnet.
3. `./todas.sh <app>`; revision por un agente aparte (liston 9, como las propias); grafico con `graficos.sh`.
4. Ficha en cada idioma con sonnet; zip con una carpeta por idioma.
5. Kit de 99: el cliente invita a su Play Console, solo en esa app y solo presencia en la tienda; subida con
   `play.sh ficha`; al acabar, que quite el acceso.

## Canales

r/AndroidClosedTesting queda fuera: sus normas solo dejan enlaces de Google, y la pagina no lo es.

### 1. r/forhire (borrador, sin enviar)

Antes: `reddit.mjs reglas forhire` en el GK35 (formato de titulo y si exige tarifa). Sale con el si del
usuario sobre el texto.

Titulo: [For Hire] Google Play screenshots and store listing in 13 languages, 3 days, 49 EUR

    I'm an indie Android developer with my own apps on Google Play and the App Store. I design store
    galleries and write listings for apps that are about to launch.

    You send raw screenshots and a few lines about your app. In 3 working days you get:

    - up to 8 screenshots with a headline each, in your app's colors (1080x2160; App Store sizes on request)
    - a feature graphic, 1024x500
    - title, short and full description
    - all of it in up to 13 languages

    49 EUR. 99 EUR with the upload to your Play Console and a draft of your production access answers.

    Before and after, and live examples: https://testers.baltajmn.dev/launch/

    You pay only after I confirm your screenshots work. One round of changes included.

### 2. Fiverr (opcional, cuenta del usuario)

Hay busqueda real de "app store screenshots". La cuenta la crea el usuario; el texto del gig sale de la
pagina en ingles. Fiverr se queda un 20 %: subir el precio a 59 / 119 alli.
