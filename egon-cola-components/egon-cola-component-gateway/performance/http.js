import http from "k6/http";
import { check } from "k6";
import { scenario, thresholds } from "./lib/profile.js";

const publicBaseUrl =
  __ENV.GATEWAY_PUBLIC_BASE_URL || "http://127.0.0.1:18081";
const internalBaseUrl =
  __ENV.GATEWAY_INTERNAL_BASE_URL || "http://127.0.0.1:18082";

export const options = {
  scenarios: {
    http_public: scenario("http_public", "publicHttp"),
    http_internal: scenario("http_internal", "internalHttp"),
  },
  thresholds: thresholds("http"),
};

export function publicHttp() {
  const id = `${__VU}-${__ITER}`;
  const response = http.get(`${publicBaseUrl}/api/orders/${id}`, {
    headers: {
      Host: __ENV.GATEWAY_PUBLIC_HOST || "api.gateway.test",
      "X-Trace-ID": `perf-http-${id}`,
    },
    tags: { protocol: "http", path: "orders" },
  });
  check(response, {
    "public request succeeds": (result) => result.status === 200,
  });
}

export function internalHttp() {
  const id = `${__VU}-${__ITER}`;
  const response = http.get(
    `${internalBaseUrl}/api/internal/inventory/${id}`,
    {
      headers: {
        Host: __ENV.GATEWAY_INTERNAL_HOST || "internal.gateway.test",
        "X-Trace-ID": `perf-internal-${id}`,
      },
      tags: { protocol: "http", path: "inventory" },
    },
  );
  check(response, {
    "internal request succeeds": (result) => result.status === 200,
  });
}
