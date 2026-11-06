# Diseno de las capturas de tienda: Test4Test

Lo que hace unicas las capturas de Test4Test (6-11-2026). Se pintan con la plantilla de `gk/contenido/plantilla/tienda/`: el estilo
en `estilos.js` (clave `test4test`), los frames en `apps.js` y los demas idiomas en `textos.js`. Toda captura nueva sigue estas reglas.
Las comunes a la familia estan en el README de la plantilla. Solo Google Play (1080x2160): la app es solo de Android, asi que no hay
tamano de App Store (`stores: ['play']`).

## Idea

El icono es un doble check violeta. Cada captura lleva ese doble check enorme y tenue detras del titular y de la pantalla, cambiando de
lado de una captura a la siguiente: puestas una junto a otra en la ficha son una fila de checks. Las palabras destacadas llevan su propio
check, como un punto tachado de una lista.

## Color (del icono y del tema de la app)

| Nombre | Hex | Uso |
|---|---|---|
| Violeta | `#5330D9` | fondo, degradado en diagonal de `#6747EA` a `#3E22B4`, igual en todas |
| Blanco | `#FFFFFF` | titular, check de fondo (al 15 %), pastillas (al 16 %) |
| Violeta suave | `#EDE8FF` | resaltado del titular y pegatina |
| Tinta violeta | `#3A1FB0` | texto sobre el resaltado |

## Letra

DIN Alternate a 700 (la del sistema de macOS), interletra -0,015 em, alineada a la izquierda. Los idiomas con otro alfabeto (hindi) caen
a la fuente del sistema. Las pastillas, en Roboto 700 como en toda la plantilla.

## Composicion

- Titular y pastillas arriba, a la izquierda, con 170 px de aire por encima (a 1320 de ancho).
- La pantalla, sin inclinar, con margen de 96 px, esquinas de 56 px y sombra violeta oscura. Es la pantalla entera de la app: con su barra
  de arriba y sus pestanas, sin barra de estado ni marco de movil. Las que sangran por abajo corren hasta el borde; la del formulario acaba
  justo debajo del boton (`crop.to`).
- Doble check de fondo: los dos trazos del icono (viewBox del cuadrado de 108, recortado a ellos), 104 % del ancho, al 15 % de blanco,
  sangrando por el lado derecho en las capturas impares y por el izquierdo en las pares. Detras del titular y de la pantalla (z-index 0).
- Resaltado con check: caja `#EDE8FF` de 28 px de radio y, al final, un check hecho con dos bordes girados 45 grados.
- Titular de 6 palabras como mucho. Todo lo que se dice es verdad y esta en la ficha.

## Las capturas y su historia

| # | Captura cruda | Idea | Que se ve |
|---|---|---|---|
| 1 | `01-feed` | el gancho: 12 testers en 14 dias | el feed de quien llega, con el medidor de 12 marcas de cada app (recorte desde 440: sin el parrafo de la entrada) |
| 2 | `02-publish` | publicar una app en un minuto | el formulario con nombre y dos enlaces de Play Console, sin cursor y con el enlace de alta entero |
| 3 | `03-round` | la ronda de hoy | 2 de 4 abiertas hoy y el aviso de cada tarde (recorte desde 392) |
| 4 | `04-owner` | quien de verdad la abre | Trail Journal: 12 de 12, dia 9 de 14, instalada y dias de uso por tester |
| 5 | `05-comments` | comentar y que te prueben | comentarios de Bird Log con la app de cada autor enlazada |
| 6 | `06-profile` | gratis o ilimitado sin anuncios | perfil con plan ilimitado: tres apps publicadas, sin anuncios |

Las cifras son las mismas en todas (`DemoBackend.kt`): Trail Journal 12 de 12 y dia 9 de 14, Sky Tonight 12 de 12 y dia 4, Pantry Pal 3,
Bird Log 5, Daily Dumbbell 6, Moss Habits 7, Tidy Budget 8, Pocket Chess Clock 10. Todo es inventado: ni las apps, ni los paquetes
(`com.example.*`; el de Trail Journal es `ru.tj.trail`, ver abajo), ni las personas existen. `DemoDataTest` comprueba que las cuentas cuadran.

Sin zoom en ningun frame: con 1,06 o 1,08 la barra de arriba quedaba a 8 px del borde de la tarjeta. El enlace de alta del formulario
(`play.google.com/apps/testing/<paquete>`) se corta en el borde del campo, que no cabe entero con ningun paquete; con el prefijo `ru`
el corte cae justo entre letras, y con `com.example` partia una letra por la mitad. Por eso el paquete de la demo es `ru.tj.trail`.

## Crudas

Salen de las pantallas reales de la app, no de un dibujo: `StoreScreenshotsTest` (`shared/src/androidHostTest`) monta `App()` con Robolectric
y Roborazzi contra un motor HTTP de mentira que sirve `DemoBackend`. El cliente de Supabase lo usa por `TestOverrides` (en la app son todos
null), nada sale a la red y cualquier peticion que no sea una lectura prevista rompe la prueba. Sin cuenta, sin backend de produccion.

```
./gradlew :shared:recordRoborazziAndroidHostTest          # crudas a store/screenshots/raw/<idioma>/ (1080x2400)
cd ../gk/contenido/plantilla/tienda && ./todas.sh test4test   # enmarcadas a store/screenshots/play/<idioma>/
```

Cinco idiomas de crudas (en-US, es-ES, pt-BR, hi-IN, id); es-419 usa las de es-ES (`rawLang`). Cada idioma lleva su texto en los
comentarios y la app, la suya en las pantallas. Con `./gradlew test` las capturas no corren (solo con las tareas de Roborazzi).

## Lo que no se hace

- Fondos de otro color, pantallas inclinadas o titular centrado.
- Cuentas, datos o nombres reales, o una captura del backend de produccion.
- Ensenar el banner de anuncios: el perfil de la demo es de plan ilimitado, que no lo lleva, y en una prueba no carga ninguno.
- Ensenar una cifra que no cuadre con otra captura.

## Anadir una captura

1. Un `@Test` nuevo en `StoreScreenshots` que lleve la app a esa pantalla y llame a `shot("07-nombre")`; datos nuevos en `DemoBackend`
   y una comprobacion en `DemoDataTest`.
2. Frame en `apps.js` (`test4test`): titular de 6 palabras como mucho, pastillas, recorte. Los demas idiomas en `textos.js`.
3. `./todas.sh test4test` y revisor aparte con liston 9.
