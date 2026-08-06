export const canonicalOAuthPageUrl = (
  currentHref: string,
  redirectUri?: string,
): string | undefined => {
  if (!redirectUri) return undefined
  const current = new URL(currentHref)
  const redirect = new URL(redirectUri)
  if (current.origin === redirect.origin) return undefined
  return new URL(`${current.pathname}${current.search}${current.hash}`, redirect.origin).toString()
}
