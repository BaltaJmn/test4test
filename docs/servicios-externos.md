# Servicios externos: que hay montado y donde duele

Todo lo que vive fuera del repo: Google Cloud, Play Console, RevenueCat,
Supabase y Cloudflare. Cada seccion dice que esta configurado hoy y, al final,
los fallos concretos que ya nos han costado tiempo, para no volver a pagarlos.

Ningun valor secreto aparece aqui. Los identificadores publicos si, porque sin
ellos la pagina no sirve de nada.

## Identificadores

| Que | Valor |
|---|---|
| Package | `com.baltajmn.test4test` |
| Proyecto de Google Cloud | `test4test-baltajmn` |
| Proyecto de Supabase | `tneuaccyzausvszpplms` (eu-central-1), desde el 26-10-2026 |
| Callback de Supabase | `https://tneuaccyzausvszpplms.supabase.co/auth/v1/callback` |
| Deep links | `test4test://login` (vuelta del login), `test4test://app/<id>` (ficha compartida) |
| Web | `https://testers.baltajmn.dev` (Cloudflare Worker `test4test`): landing y privacidad, sin app |
| Producto de compra | Suscripcion `unlimited_apps` (por crear). `unlimited_slots`, pago unico, queda como legado |
| Grupo de la prueba cerrada | `https://groups.google.com/g/test4test-baltajmn` |

## Google Cloud

Proyecto `test4test-baltajmn`. Contiene el cliente OAuth del login y las dos
cuentas de servicio.

### OAuth para el login con Google

Hace falta **un solo cliente, de tipo Web**. Es el que se pega en Supabase, y en
sus URIs de redireccion autorizados tiene que estar el callback de Supabase de la
tabla de arriba. Al cambiar de proyecto de Supabase hay que anadir el nuevo: sin
el, Google devuelve `redirect_uri_mismatch`.

No hace falta cliente de tipo Android ni el SHA-1 del keystore. Lo dimos por
necesario al principio y es falso: la app no usa Google Sign-In nativo, usa
`signInWith(Google)` de supabase-kt, que abre el navegador y vuelve por
`test4test://login`. El SHA-1 solo lo pide el flujo nativo, que no usamos.

La pantalla de consentimiento esta en modo Testing con el usuario propio dado de
alta como test user.

### Cuentas de servicio

Hay dos, y confundirlas costo dos releases fallidas:

| Cuenta | Para que |
|---|---|
| `revenuecat@test4test-baltajmn.iam.gserviceaccount.com` | Que RevenueCat lea el catalogo de Play |
| `play-publisher@test4test-baltajmn.iam.gserviceaccount.com` | Que la CI suba el bundle a Play. Es la que va en el secreto `PLAY_SERVICE_ACCOUNT_JSON` |

En el proyecto de Cloud tiene que estar habilitada la **Google Play Android
Developer API**. Sin ella RevenueCat da un error de permisos que no menciona en
ningun momento que falte la API.

## Play Console

### Permisos de las cuentas de servicio

Se dan en Play Console, Usuarios y permisos, no en Google Cloud. Son dos cosas
distintas y las dos hacen falta:

1. **Acceso a la app.** Test4Test tiene que estar marcada en las apps de ese
   usuario. El permiso a nivel de cuenta por si solo no basta.
2. **El permiso concreto.**
   - Para `play-publisher`: "Publicar en canales de prueba" (Release to testing
     tracks). Con solo lectura la subida devuelve
     `The caller does not have permission`.
   - Para `revenuecat`, los tres: "Ver informacion de la app y descargar
     informes masivos (solo lectura)", "Ver datos financieros, pedidos y
     respuestas a encuestas de cancelacion" y "Gestionar pedidos y
     suscripciones". El primero es el que se olvida y es justo el que necesita
     el check 2 de RevenueCat.

Si la cuenta aparece como "Invitacion pendiente", el permiso no esta activo
aunque la casilla se vea marcada.

### El producto de compra

Desde la 1.6 es una suscripcion: plan gratuito con una app a la vez, y la
suscripcion para tener mas a la vez (decision del 26-10-2026, monetizacion mas
agresiva). Hasta la 1.5 fue `unlimited_slots`, pago unico; el webhook sigue
aceptando `NON_RENEWING_PURCHASE` para que quien lo compro lo recupere con
Restaurar. Riesgo conocido: una renovacion automatica sobre un problema que se
resuelve en tres semanas trae cancelaciones y alguna resena mala; el paywall y
el perfil dicen como cancelar para que no sea por sorpresa.

La app coge el primer paquete del offering actual, sea el que sea: cambiar de
producto es cosa de RevenueCat y no pide version nueva.

**El producto tiene que estar activo, no en borrador.** Un producto guardado
pero inactivo no aparece en RevenueCat, y es el fallo que mas tiempo hizo perder
de todo el montaje.

### Probar la compra sin pagar

1. Play Console, Configuracion, **Pruebas de licencia**: anadir ahi la cuenta de
   Google del dispositivo. Sin esto la compra sale como real y se cobra.
2. Instalar **desde Play**, nunca con `adb install`. Con Play App Signing el APK
   firmado con la clave de subida no coincide con el que firma Google, y la
   facturacion responde "item not available".
3. Antes de tocar el boton de compra, comprobar que la hoja de Play dice que es
   una tarjeta de prueba. Si no lo dice, parar: es un cargo real.

Ojo: con `is_premium` a `true` en la fila del perfil, `PurchaseSection` ni
siquiera se monta y no hay nada que probar.

### Seguridad de los datos

Desde la 1.6 la app comprueba si estan instaladas las apps a las que el usuario
se ha unido (`<queries>` de launcher, no `QUERY_ALL_PACKAGES`). En el formulario
de Seguridad de los datos eso es "Actividad en la app: apps instaladas",
recogido, no compartido con terceros, obligatorio para la funcion y no opcional.
Cambiarlo antes de subir la 1.6 a revision.

### Publicar

Solo el track de pruebas **cerradas** cuenta para los 12 testers y 14 dias
seguidos. El interno no cuenta, diga lo que diga el contador de esa pantalla.

El resto del procedimiento esta en la seccion "Releasing" del
[README](../README.md). Los dos que se olvidan: subir el `versionCode` antes de
cada subida, y escribir las notas de version a mano en Play Console porque el
workflow no pasa `whatsnew-dir`.

## RevenueCat

App de Android sobre el package `com.baltajmn.test4test`, con el JSON de la
cuenta de servicio `revenuecat@` subido.

- **Products**: importar `unlimited_slots`. Si el desplegable sale vacio, el
  identificador se puede escribir a mano: los checks 2 y 3 solo alimentan ese
  desplegable, no son funcionalidad.
- **Entitlements**: crear uno. El identificador da igual, el codigo mira
  `info.entitlements.active.isEmpty()`, no un id concreto. Enganchar el producto.
- **Offerings**: uno con identificador `default`, **marcado como Current**, con
  al menos un Package dentro que lleve el producto. El codigo coge el primer
  paquete del offering actual. Sin el Current no funciona nada y el paywall sale
  vacio sin dar ningun error.

La propagacion de permisos de Play tarda **hasta 36 horas** segun la propia
documentacion de RevenueCat, y su validador ademas cachea la respuesta un rato.
Si da error de credenciales el primer dia, es eso: esperar antes de tocar nada.
Para forzar una revalidacion, "Validate credentials" o volver a guardar la app.

### Webhook

Vive en [`supabase/functions/revenuecat-webhook`](../supabase/functions/revenuecat-webhook/index.ts)
y esta desplegado y activo. La fuente de verdad del estado premium es Supabase:
el cliente solo lee `profiles.is_premium`, nunca lo escribe.

En el panel de RevenueCat:

- URL: `https://tneuaccyzausvszpplms.supabase.co/functions/v1/revenuecat-webhook`
- Cabecera Authorization: el secreto **pelado, sin `Bearer ` delante**. La
  funcion compara la cabecera entera contra el valor guardado.
- Environment: **Sandbox y Production**. Si solo se manda Production, la compra
  de prueba no escribe nada y parece que el webhook esta roto.

El secreto se genera con `openssl rand -hex 32` y se pone en los dos lados. Del
lado de Supabase:

```bash
supabase secrets set REVENUECAT_WEBHOOK_SECRET=VALOR --project-ref tneuaccyzausvszpplms
```

Y el despliegue lleva `--no-verify-jwt` obligatoriamente, porque RevenueCat no
manda un token de Supabase, se autentica con esa cabecera:

```bash
supabase functions deploy revenuecat-webhook --project-ref tneuaccyzausvszpplms --no-verify-jwt
```

La funcion responde 200 a los eventos que no cambian nada. Un 4xx haria a
RevenueCat reintentar el mismo evento durante horas.

## Supabase

Proyecto `tneuaccyzausvszpplms`. Esquema y politicas en
[`supabase/migrations`](../supabase/migrations), y la bateria de RLS en
[`supabase/tests/rls_test.sql`](../supabase/tests/rls_test.sql).

**Como se perdio el primero.** El plan gratuito pausa un proyecto tras una
semana sin trafico. `fhsrjyuzkrbfuvuzplvy` se pauso el 26-09-2026 (aviso por
correo el 25), siguio pausado y el 26-10 ya no existia: con el se fueron todas
las cuentas, apps y testers, y la app estuvo un mes sin backend en plena prueba
cerrada. Ademas tenia aplicadas a mano migraciones que nunca entraron en el repo
(racha, denuncias, reciprocidad) y hubo que reconstruirlas. Desde entonces:
`keep-alive.yml` lo despierta cada dia, y nada se aplica sin su fichero en
`supabase/migrations`.

**Huecos del plan gratuito.** Dos proyectos activos por organizacion. Para crear
este se pauso Wachemon (`dsefqvklpngxahosioqq`) el 26-10-2026, no se borro: se
restaura desde el dashboard si hace falta, pausando otro.

- **Authentication, Providers, Google**: el Client ID y el Client secret del
  cliente **Web**. El de tipo Android no va aqui (y no existe).
- **Authentication, URL Configuration**: Site URL `https://testers.baltajmn.dev`
  y, en Redirect URLs, `test4test://login` para la vuelta en Android.
- **Authentication, Users**: la cuenta de correo y contrasena del revisor de
  Play, la misma que figura en Play Console, Acceso a la aplicacion.
- La clave `anon` es publica por diseno: esta protegida por RLS y va dentro de
  los clientes. La `service_role` no esta en el repo y no debe estarlo, vive
  solo en los secretos de Edge Functions.
- `is_premium` y `is_admin` solo los escribe `service_role` o el dashboard,
  por un GRANT a nivel de columna.

## Cloudflare

Worker `test4test` con Workers Static Assets, sin script, sirviendo la carpeta
[`web/`](../web) tal cual: landing, `/privacy` y `/privacy-es`. Configuracion en
[`wrangler.jsonc`](../wrangler.jsonc), despliegue en `deploy-web.yml`.

Tres cosas que rompieron el sitio en produccion:

- **`"workers_dev": true` tiene que estar explicito.** Declarar `routes` apaga
  la ruta `*.workers.dev` por defecto, y wrangler solo lo avisa con un WARNING.
  El sitio se quedo en 404.
- **`wranglerVersion: "4.127.1"` fijado en el workflow.** `wrangler-action@v3`
  trae la 3.90 por defecto y esa version no sabe desplegar un Worker que solo
  sirve assets: exige un `main` que aqui no existe a proposito.
- **Al mover el dominio, wrangler no limpia los registros del apex.** Quedaron
  apuntando a un origen inexistente y `baltajmn.dev` devolvia 522. Se limpia a
  mano en el DNS de Cloudflare. Ojo de paso con `quilt.baltajmn.dev`, que es
  GitHub Pages y va en DNS only.

Un certificado recien emitido tarda un rato: si `curl` da
`sslv3 alert handshake failure` contra una URL nueva, no esta roto, esta
provisionandose.

## Keystore

`keystore/upload.jks`, gitignored, nunca se commitea. Las passwords viven en
`local.properties` y en el gestor de contrasenas, en ningun sitio mas.

`keytool -v` revienta bajo locale espanol con
`java.util.MissingFormatArgumentException: Format specifier '%2$s'`. Es un bug
del JDK. Se esquiva forzando el idioma:

```bash
keytool -J-Duser.language=en -J-Duser.country=US -list -v -keystore keystore/upload.jks
```

## Secretos de GitHub Actions

Diez, todos por fichero o por pipe, nunca pegados en un chat:

```bash
gh secret set NOMBRE < ruta/al/fichero
```

| Secreto | De donde sale |
|---|---|
| `SUPABASE_URL`, `SUPABASE_ANON_KEY` | Supabase, Settings, API |
| `REVENUECAT_ANDROID_KEY` | RevenueCat, clave publica `goog_...` de Android |
| `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | El keystore de subida |
| `PLAY_SERVICE_ACCOUNT_JSON` | JSON de `play-publisher@` |
| `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID` | Cloudflare, My Profile, API Tokens |

De RevenueCat solo la clave publica `goog_...` es compartible. La `sk_...` no
sale del panel.

## Pendiente de rotar

Dos credenciales quedaron escritas en transcripts de sesiones anteriores y hay
que sustituirlas, independientemente de que sigan funcionando:

- **Token de API de Cloudflare.** Cloudflare, My Profile, API Tokens, borrar y
  crear otro. Luego `gh secret set CLOUDFLARE_API_TOKEN` por fichero.
- **Clave JSON de `play-publisher@`.** Google Cloud, IAM, Cuentas de servicio,
  esa cuenta, Claves: borrar la actual y crear otra. Luego
  `gh secret set PLAY_SERVICE_ACCOUNT_JSON < ruta.json`.
