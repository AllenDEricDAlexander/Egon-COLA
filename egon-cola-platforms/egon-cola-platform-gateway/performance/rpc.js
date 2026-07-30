import http from "k6/http";
import { check } from "k6";
import { scenario, thresholds } from "./lib/profile.js";

const consumerBaseUrl =
  __ENV.GATEWAY_RPC_CONSUMER_BASE_URL || "http://127.0.0.1:18084";
const internalBaseUrl =
  __ENV.GATEWAY_INTERNAL_BASE_URL || "http://127.0.0.1:18082";

export const options = {
  scenarios: {
    rpc_to_rpc: scenario("rpc_to_rpc", "rpcToRpc"),
    http_to_rpc: scenario("http_to_rpc", "httpToRpc"),
  },
  thresholds: thresholds("rpc"),
};

export function rpcToRpc() {
  const id = `${__VU}-${__ITER}`;
  const response = http.get(
    `${consumerBaseUrl}/test/rpc/echo?message=perf-${id}`,
    {
      headers: { "X-Trace-ID": `perf-rpc-${id}` },
      tags: { protocol: "rpc", path: "rpc-to-rpc" },
    },
  );
  check(response, {
    "rpc request succeeds": (result) => result.status === 200,
  });
}

export function httpToRpc() {
  const id = `${__VU}-${__ITER}`;
  const response = http.post(
    `${internalBaseUrl}/rpc/echo`,
    JSON.stringify({ message: `perf-${id}` }),
    {
      headers: {
        Host: __ENV.GATEWAY_RPC_HOST || "rpc.gateway.test",
        "Content-Type": "application/json",
        "X-Trace-ID": `perf-http-rpc-${id}`,
      },
      tags: { protocol: "rpc", path: "http-to-rpc" },
    },
  );
  check(response, {
    "http to rpc request succeeds": (result) => result.status === 200,
  });
}
