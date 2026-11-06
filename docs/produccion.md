# Solicitar acceso a produccion

Formulario de *Panel de control > Solicitar acceso a produccion*: respuestas libres de 300
caracteres y desplegables, en cuatro pasos. Si Google rechaza hay que rellenarlo otra vez, asi que
el texto vive aqui. En ingles, como Quilt (`../HabitTracker/store/produccion.md`): el revisor no
tiene garantizado el espanol.

Es una declaracion, no marketing: Google lo contrasta con las estadisticas reales de la prueba. De
donde sale cada dato, al final.

## 1. Informacion sobre tu prueba cerrada

**Como reclutaste usuarios**

```
By email: 55 messages on 1-3 Sep to 29 Google Groups that developers use for their own closed tests and to 26 developers, and a second round on 27 and 30 Oct after 1.6. The offer was reciprocal: I join your test, you join mine. They are exactly the users the app is for.
```

**Como de facil:** Dificil.

**Interacciones de los testers**

```
They signed up, listed apps, commented and reported. After the backend was rebuilt in 1.6 (26 Oct), six accounts signed up but none has listed an app or joined a test yet. A real user joins more tests than they list; here even listing stalled, so the core loop is barely used.
```

**Comentarios y como se recogieron**

```
By email replies with screenshots and the in-app comments, no survey. One tester sent a screenshot of the opt-in link failing, a Japanese developer found non-Latin text shown as boxes, and testers doing the three steps found the last button disabled. All three were fixed.
```

## 2. Sobre tu aplicacion

**Audiencia**

```
Android developers with a personal Play Console account who need 12 testers for 14 days before production, aged 13 and up as declared; nothing in it targets children. Free with one app listed at a time; a monthly subscription lists more.
```

**Como aporta valor**

```
Tester swaps run on trust: you never know who really installed your app. Test4Test checks on the tester's phone that the app came from Google Play, shows the owner each tester's status and counts the 14-day streak, so a swap can be checked instead of trusted.
```

**Descargas esperadas el primer ano:** la franja mas baja. El formulario dice que no afecta a nada.

## 3. Preparacion para produccion

**Cambios a partir de la prueba**

```
Non-Latin scripts now render (bundled font removed), the last step no longer stays disabled and links are validated. Google sign-in now works for everyone, the install check arrived in 1.6, and the backend was rebuilt with tested access rules and a daily keep-alive.
```

**Como decidiste que estaba lista**

```
Every fix from the test is in 1.6. CI builds, lints and runs the tests on every push; the database access rules have their own test suite, run after any change; a daily ping keeps the backend from pausing again. Production ships through the same pipeline as the test.
```

## 4. Pruebas adicionales

Sale porque la primera solicitud se rechazo el 21-09-2026 ("Testers were not engaged with your
app during your closed test").

**Que has hecho de forma diferente esta vez**

```
First run: Google sign-in only admitted test users, the backend was paused and then lost for a month, and the app was briefly set as unpublished, so testers could not engage. Now sign-in is open, the backend is rebuilt and kept awake, 1.6 verifies installs, and I recruited again.
```

## De donde sale cada dato

| Afirmacion | Prueba |
|---|---|
| 55 correos, 29 grupos, 26 personas, 1 a 3 de septiembre | Borrador anterior del formulario en Play Console, contado entonces de Gmail |
| Segunda ronda 27 y 30 de octubre | Gmail, enviados "A tester exchange for your closed test" y su seguimiento |
| Opt-in con error | Hilo con Alexandre Machado, 2-9-2026 |
| Texto no latino en cuadros | `aeef54e` |
| Boton del ultimo paso apagado | `1b3f431` |
| Enlaces validados | `2d9d82c` |
| Login de Google para todos | Pantalla de consentimiento en produccion el 26-10 (`servicios-externos.md`) |
| Comprobacion de instalacion, backend nuevo, keep-alive | `bc4cede`, `4aecead`, `keep-alive.yml` |
| Seis cuentas, cero apps y cero uniones desde la 1.6 | Consulta a Supabase del 4-11-2026 |
| Tests en cada push y de RLS | `android-ci.yml`, `supabase/tests/rls_test.sql` |
