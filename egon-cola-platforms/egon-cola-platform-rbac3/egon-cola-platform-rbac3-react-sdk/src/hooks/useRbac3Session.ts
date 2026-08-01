import { useContext } from 'react'
import { Rbac3SessionContext } from '../provider/Rbac3Provider'

export const useRbac3Session = () => {
  const value = useContext(Rbac3SessionContext)
  if (value === null) {
    throw new Error('useRbac3Session must be used within Rbac3Provider')
  }
  return value
}
