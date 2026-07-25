# Gateway Admin Web

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
