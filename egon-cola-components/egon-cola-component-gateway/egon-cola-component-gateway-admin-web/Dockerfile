FROM node:22-alpine AS build

WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:22-alpine

ENV PORT=8080
ENV GATEWAY_ADMIN_API_BASE_URL=http://gateway-admin:18080
ENV GATEWAY_ADMIN_API_DEVELOPMENT_PLAINTEXT=true

WORKDIR /app
COPY --from=build --chown=node:node /workspace/dist /app/dist
COPY --chown=node:node static-server.mjs /app/static-server.mjs

USER node
EXPOSE 8080

ENTRYPOINT ["node", "/app/static-server.mjs"]
