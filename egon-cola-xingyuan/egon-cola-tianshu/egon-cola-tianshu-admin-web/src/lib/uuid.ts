const hex = Array.from({ length: 256 }, (_, value) =>
  value.toString(16).padStart(2, '0'))

export const uuidV7 = (
  timestamp = Date.now(),
  randomBytes = crypto.getRandomValues(new Uint8Array(10)),
): string => {
  const bytes = new Uint8Array(16)
  let milliseconds = BigInt(timestamp)
  for (let index = 5; index >= 0; index -= 1) {
    bytes[index] = Number(milliseconds & 0xffn)
    milliseconds >>= 8n
  }
  bytes[6] = 0x70 | (randomBytes[0] & 0x0f)
  bytes[7] = randomBytes[1]
  bytes[8] = 0x80 | (randomBytes[2] & 0x3f)
  bytes.set(randomBytes.slice(3, 10), 9)

  return [
    [...bytes.slice(0, 4)].map((value) => hex[value]).join(''),
    [...bytes.slice(4, 6)].map((value) => hex[value]).join(''),
    [...bytes.slice(6, 8)].map((value) => hex[value]).join(''),
    [...bytes.slice(8, 10)].map((value) => hex[value]).join(''),
    [...bytes.slice(10, 16)].map((value) => hex[value]).join(''),
  ].join('-')
}
