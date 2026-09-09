#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: demo-token.sh <base64-hmac-secret>" >&2
  exit 2
fi

base64_url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

issued_at="$(date +%s)"
expires_at="$((issued_at + 43200))"
header="$(printf '%s' '{"alg":"HS256","typ":"JWT"}' | base64_url)"
payload="$(printf '{"sub":"gateway-demo","iat":%s,"exp":%s,"capabilities":["*"],"roles":["gateway-admin"]}' "${issued_at}" "${expires_at}" | base64_url)"
signing_input="${header}.${payload}"
key_hex="$(printf '%s' "$1" | openssl base64 -d -A | od -An -v -tx1 | tr -d ' \n')"
signature="$(printf '%s' "${signing_input}" | openssl dgst -sha256 -mac HMAC -macopt "hexkey:${key_hex}" -binary | base64_url)"
printf '%s\n' "${signing_input}.${signature}"
