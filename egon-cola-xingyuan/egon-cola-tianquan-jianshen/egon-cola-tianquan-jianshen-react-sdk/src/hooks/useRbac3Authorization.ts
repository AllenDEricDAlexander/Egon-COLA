import {useContext} from 'react'
import {Rbac3AuthorizationContext} from '../provider/Rbac3Provider'

export const useRbac3Authorization = () => {
    const value = useContext(Rbac3AuthorizationContext)
    if (value === null) {
        throw new Error('useRbac3Authorization must be used within Rbac3Provider')
    }
    return value
}
