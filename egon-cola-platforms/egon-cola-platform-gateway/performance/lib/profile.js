import capacity from "../capacity.json";

const profileName = __ENV.GATEWAY_PERF_PROFILE || "smoke";
const profile = capacity.profiles[profileName];

if (!profile) {
  throw new Error(`unknown GATEWAY_PERF_PROFILE: ${profileName}`);
}

export function scenario(name, exec) {
  return {
    executor: "constant-arrival-rate",
    exec,
    rate: numberEnv("GATEWAY_PERF_RATE", profile.ratePerSecond),
    timeUnit: "1s",
    duration: __ENV.GATEWAY_PERF_DURATION || profile.duration,
    preAllocatedVUs: numberEnv(
      "GATEWAY_PERF_PRE_ALLOCATED_VUS",
      profile.preAllocatedVus,
    ),
    maxVUs: numberEnv("GATEWAY_PERF_MAX_VUS", profile.maxVus),
    tags: { scenario: name },
  };
}

export function thresholds(protocol) {
  const threshold = capacity.thresholds[protocol];
  return {
    [`http_req_failed{protocol:${protocol}}`]: [
      `rate<${threshold.failureRate}`,
    ],
    [`http_req_duration{protocol:${protocol}}`]: [
      `p(95)<${threshold.p95Milliseconds}`,
      `p(99)<${threshold.p99Milliseconds}`,
    ],
    dropped_iterations: [
      `count<=${capacity.thresholds.droppedIterations}`,
    ],
  };
}

function numberEnv(name, fallback) {
  if (!__ENV[name]) {
    return fallback;
  }
  const value = Number(__ENV[name]);
  if (!Number.isFinite(value) || value <= 0) {
    throw new Error(`${name} must be a positive number`);
  }
  return value;
}
