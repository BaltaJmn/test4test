# Promocion de Test4Test (borradores del 8-11-2026)

Tres canales elegidos el 8-11 con el usuario. Nada de esto se ha enviado: cada pieza sale con su si
sobre el texto. Sin mencionar IA. Revisados por un agente aparte el 8-11 (afirmaciones contra la app,
tono, normas, limites). Datos que usan los textos (comprobados ese dia): suscripcion `unlimited_apps`
1,99 EUR / US$1.89 al mes; ficha https://play.google.com/store/apps/details?id=com.baltajmn.test4test;
web https://testers.baltajmn.dev. La primera solicitud de produccion de Test4Test se rechazo el 21-09
por "Testers were not engaged": es la historia real que cuentan los textos.

Ojo con el nombre: existe otra app "Test4Test - Get App Testers" (aimteralabs). Donde haga falta
distinguirse, "Test4Test: 12 Testers, 14 Days", que es el titulo de la ficha.

## 1. Reddit, r/AndroidClosedTesting (49.399 miembros)

Normas leidas el 8-11 con `reddit.mjs reglas`: solo enlaces de Google o de Google Groups (la web no
puede ir, la ficha de Play si), nada de Telegram, WhatsApp ni Discord en el titulo, dominio de
testerscommunity.com vetado, sin flair obligatorio. No prohibe hablar de herramientas, pero el veto a la
competencia dice que pueden quitarlo: primero se pregunta a los mods y el post solo sale si dicen que si.
r/TestersCommunity queda fuera: es de Testers Community y su norma 4 prohibe herramientas.

Envio: `reddit.mjs mensaje` en el GK35 (formulario de www con Chrome visible). Si sale el post, hay que
contestar los comentarios en las 2-3 primeras horas: o el usuario desde el movil, o se aprueban al momento.

### 1a. Mensaje a los mods

**ENVIADO el 8-11-2026** con el si del usuario, como u/BaltaJmn, por `reddit.mjs mensaje` en el GK35.
La respuesta llega a los mensajes de Reddit de esa cuenta; no reenviar.

Asunto: Can I post once about a test-for-test app?

    Hi mods,

    I'm an Android developer and I made Test4Test, an app for exactly what this sub does: you
    list your closed test, join other people's tests, and they join yours. It checks on the
    tester's phone that the app was installed from Google Play, and it has a daily round so
    testers keep opening the apps, which Google asks for. It's free, with ads and an optional
    subscription.

    Would you be OK with a single post presenting it, with only the Play Store link? If it's not
    a fit for the sub, no problem, I won't post it.

    Thanks,
    Balta

### 1b. Post (solo con el si de los mods)

Titulo: I made an app for test-for-test swaps that checks testers really installed your app

    I had to find 12 testers for 14 days too, and my first production request was rejected
    because testers "were not engaged". In the swaps I tried, people said "joined" and never
    installed, or uninstalled a few days in.

    So I built Test4Test:

    - List your app with its Google Group and its opt-in link.
    - The list is a line: apps go in order of arrival, developers who test others move up, and
      apps that already have 12 step aside. Everyone pushes the first apps to 12 instead of
      spreading thin.
    - When someone joins your test, their phone confirms your app is installed from Google Play,
      and you see it next to their name.
    - Daily round: testers open the apps they test once a day from Test4Test, and you see on how
      many of the last 14 days each one opened yours.
    - Comments under each app to ask for a test back.

    The free plan lists one app at a time and shows a small banner. There's an optional
    subscription (US$1.89/month): no ads, unlimited apps, and your apps go first in the line,
    labeled Featured, so paid apps are always marked. Joining tests, commenting and browsing
    are free.

    https://play.google.com/store/apps/details?id=com.baltajmn.test4test

    It's brand new, so there are only a few apps in it yet. Tell me what's missing and I'll
    fix it.

## 2. AlternativeTo

Receta en la memoria del kit de lanzamiento: el usuario inicia sesion en el navegador integrado y yo
relleno `alternativeto.net/manage-item/` con `form_input`; icono y capturas con "Upload by URL" desde la
ficha publicada de Play. Cola gratis (la prioritaria, 5 $, la decide el usuario).

- Nombre: Test4Test (si esta cogido: "Test4Test 12 Testers")
- Web: https://testers.baltajmn.dev
- Enlace de Android: la ficha de Play
- Plataformas: Android
- Licencia y precio: Freemium; subscription $2/month (US$1.89, AlternativeTo solo admite dolares enteros)
- Etiquetas: beta testing, app testing, android development, community
- Idiomas de la app: English, Spanish, Portuguese, Hindi, Indonesian
- Nota al revisor: I'm the developer.
- Alternativas que sugerir tras enviar (sin ellas no sale en busquedas): TestFi, Tester Bee

Descripcion corta:

    Swap closed testing with other Android developers to gather the 12 testers for 14 days that Google Play asks for.

Descripcion:

    Google Play asks new personal developer accounts to run a closed test with 12 testers for
    14 days before they can publish. Test4Test is an exchange for that: you list your app with
    its Google Group and opt-in link, join other developers' tests, and they join yours.

    The list is a line: apps go in order of arrival and developers who test others move up, so
    testers concentrate on the first apps until they reach 12. When someone joins your test,
    their phone checks that your app is installed from Google Play and you see it next to their
    name. A daily round has testers open the apps they test once a day from Test4Test, and
    owners see on how many of the last 14 days each tester opened them.

    Free with one app at a time and a small ad banner. An optional monthly subscription lists
    unlimited apps, removes ads and puts your apps first in the line, labeled Featured.

## 3. Product Hunt

Lanzamiento a mano con la cuenta del usuario (el plan de apps ya lo dejaba asi). Mejor de martes a
jueves a las 00:01 hora del Pacifico (09:01 en Espana). Competidor ya lanzado alli: BetaCircle.
Imagenes en `docs/producthunt/` (miniatura 240x240 y galeria 1270x760, hechas con las capturas de la
ficha de Play en ingles).

- Nombre: Test4Test
- Tagline (60 max): Swap testers for Google Play's 12-tester rule
- Enlaces: la ficha de Play y https://testers.baltajmn.dev
- Temas: Android, Developer Tools
- Precio: Free options available
- Descripcion (260 max):

      Google Play makes new developers run a closed test with 12 testers for 14 days. Test4Test is an exchange: list your app, join other tests and they join yours. Installs are checked on the tester's phone, and a daily round nudges testers to open the apps.

Primer comentario del autor:

    Hi Product Hunt! I'm Balta, an Android developer.

    If you've opened a personal Google Play developer account recently, you know the rule:
    12 testers opted in for 14 days in a row before you can publish. My first production
    request was rejected because testers "were not engaged": in the chat-group swaps I used,
    people said "I joined yours" and often never installed the app, or dropped it early.

    Test4Test turns the swap into an app:
    - The list is a line in order of arrival, and developers who test others move up, so
      everyone helps the first apps reach 12.
    - Installs are confirmed on the tester's phone, not taken on trust.
    - A daily round reminds testers to open the apps, and owners see on how many of the last
      14 days each tester did.

    It's free for one app at a time, with a small banner; an optional subscription gives
    unlimited apps, no ads and a Featured spot at the front of the line. It's brand new, so
    there are only a few apps in it yet. I'd love feedback from anyone who went through the
    14 days, especially on what made your testers drop off.
