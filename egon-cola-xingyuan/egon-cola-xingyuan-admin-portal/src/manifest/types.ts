export type PlatformKey = 'tianquan-shoubing' | 'tianquan-jianshen' | 'yuheng' | 'tianshu'

export interface ChildManifest {
  key: PlatformKey
  displayName: string
  url: string
  standaloneUrl: string
  version: string
  contractVersion: string
  compatibleHostRange: string
  requiredCapabilities: readonly string[]
  summaryUrl?: string
}

export type PlatformManifest = ChildManifest

export type ManifestErrorCode =
  | 'MANIFEST_FETCH_FAILED'
  | 'MANIFEST_INVALID'
  | 'MANIFEST_URL_NOT_ALLOWED'
  | 'MANIFEST_VERSION_UNSUPPORTED'
