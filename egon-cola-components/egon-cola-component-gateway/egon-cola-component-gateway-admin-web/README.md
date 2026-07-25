# Gateway Admin Web

## Authentication

The Admin Web never sends an actor identity header. It stores a verified IAM
Bearer Token in `sessionStorage` by default and loads the actor and capabilities
from `GET /api/v1/gateway/admin/session`. Selecting "persist login" moves the
token bundle to `localStorage`; logout removes both copies.

Optional automatic refresh uses:

```text
VITE_GATEWAY_ADMIN_TOKEN_URL=https://iam.example.com/oauth2/token
VITE_GATEWAY_ADMIN_CLIENT_ID=gateway-admin-web
```

The configured identity provider must allow the browser client and enforce its
own CORS/PKCE policy. No client secret is embedded in the web bundle.

Independent React management console for Egon COLA Gateway.

```bash
npm ci
npm test -- --run
npm run build
```

The browser calls only Gateway Admin. Configure a different API origin with
`VITE_GATEWAY_ADMIN_API_BASE_URL`; an empty value uses the current origin.
The placeholder management actor is set with
`VITE_GATEWAY_ADMIN_ACTOR_ID` until the host platform supplies IAM.
