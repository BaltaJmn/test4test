// Webhook de RevenueCat (issue #25). La fuente de verdad del estado premium es
// Supabase: el cliente solo lee profiles.is_premium, nunca lo escribe.
//
// Desplegar sin verificacion de JWT: RevenueCat no manda un token de Supabase,
// se autentica con la cabecera Authorization que se configura en su dashboard.
//   supabase functions deploy revenuecat-webhook --no-verify-jwt
//
// SUPABASE_URL y SUPABASE_SERVICE_ROLE_KEY los inyecta Supabase solo. El unico
// secreto que hay que dar de alta a mano es REVENUECAT_WEBHOOK_SECRET.
const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const WEBHOOK_SECRET = Deno.env.get("REVENUECAT_WEBHOOK_SECRET") ?? "";

// CANCELLATION no esta en REVOKE aunque la issue lo pidiera: en RevenueCat
// significa "ha desactivado la renovacion", no "ha perdido el acceso". El acceso
// dura hasta EXPIRATION, y quitarlo antes seria cobrar un mes y no darlo.
// TRANSFER tampoco entra: no trae un app_user_id, sino listas de origen y destino.
const GRANT = new Set([
  "INITIAL_PURCHASE",
  "RENEWAL",
  "UNCANCELLATION",
  "NON_RENEWING_PURCHASE",
  "PRODUCT_CHANGE",
  "SUBSCRIPTION_EXTENDED",
]);
const REVOKE = new Set(["EXPIRATION", "SUBSCRIPTION_PAUSED"]);

// app_user_id lo elige el cliente, asi que llega como dato no fiable y acaba
// dentro de una query de PostgREST: solo se acepta si es un uuid.
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const sha256 = async (value: string): Promise<Uint8Array> =>
  new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)));

// Comparacion en tiempo constante sobre los digest: comparar los secretos con ===
// filtra por tiempo de respuesta cuantos caracteres iniciales son correctos.
async function secretMatches(header: string): Promise<boolean> {
  if (!WEBHOOK_SECRET) return false;
  const [a, b] = await Promise.all([sha256(header), sha256(WEBHOOK_SECRET)]);
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
  return diff === 0;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("method not allowed", { status: 405 });
  if (!(await secretMatches(req.headers.get("Authorization") ?? ""))) {
    return new Response("unauthorized", { status: 401 });
  }

  const event = (await req.json().catch(() => null))?.event;
  const premium = GRANT.has(event?.type) ? true : REVOKE.has(event?.type) ? false : null;
  // 200 en los eventos que no cambian nada: un 4xx haria a RevenueCat reintentar
  // el mismo evento durante horas.
  if (premium === null) return new Response("ignored", { status: 200 });

  // app_user_id = profiles.id, que es lo que manda el cliente en Purchases.logIn.
  const userId = event.app_user_id;
  if (typeof userId !== "string" || !UUID.test(userId)) {
    return new Response("bad app_user_id", { status: 400 });
  }

  const res = await fetch(
    `${SUPABASE_URL}/rest/v1/profiles?id=eq.${encodeURIComponent(userId)}`,
    {
      method: "PATCH",
      headers: {
        apikey: SERVICE_ROLE_KEY,
        Authorization: `Bearer ${SERVICE_ROLE_KEY}`,
        "Content-Type": "application/json",
        Prefer: "return=minimal",
      },
      body: JSON.stringify({ is_premium: premium }),
    },
  );
  // Devolver 5xx hace que RevenueCat reintente, que es justo lo que se quiere si
  // el fallo fue de Postgres y no del evento.
  if (!res.ok) return new Response(await res.text(), { status: 500 });
  return new Response("ok", { status: 200 });
});
