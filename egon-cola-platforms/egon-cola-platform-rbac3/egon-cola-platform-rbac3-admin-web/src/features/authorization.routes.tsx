import { useParams } from 'react-router-dom'
import type { FeatureRouteDescriptor } from './shared/RouteDescriptor'
import { AssignmentListPage } from './assignment/AssignmentListPage'
import { ManagementPolicyPage } from './management-policy/ManagementPolicyPage'
import { RoleActivationPage } from './role-activation/RoleActivationPage'
import { SessionListPage } from './session/SessionListPage'

const AssignmentRoute = () => <AssignmentListPage userId={useParams().userId ?? ''} />

export const authorizationRouteDescriptors: readonly FeatureRouteDescriptor[] = [
  { key: 'assignments', path: '/authorization/assignments/:userId', title: '角色任职', permission: 'system:role-assignment:read', componentKey: 'rbac3-assignments', component: AssignmentRoute, navigationOrder: 70 },
  { key: 'management-policies', path: '/authorization/management-policies', title: '委托策略', permission: 'system:management-policy:read', componentKey: 'rbac3-management-policies', component: ManagementPolicyPage, navigationOrder: 71 },
  { key: 'role-activation', path: '/authorization/role-activation', title: '激活角色', permission: 'system:role-activation:read', componentKey: 'rbac3-role-activation', component: RoleActivationPage, navigationOrder: 72 },
  { key: 'sessions', path: '/authorization/sessions', title: '会话管理', permission: 'system:session:read', componentKey: 'rbac3-sessions', component: SessionListPage, navigationOrder: 73 },
] as const
