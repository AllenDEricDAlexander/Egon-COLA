export type ResultRecord<T> = {
  success: boolean
  code: number
  status: string
  message: string
  data: T
  traceId: string
  timestamp: number
}
