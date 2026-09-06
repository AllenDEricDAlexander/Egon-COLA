import {LoginOutlined, LogoutOutlined} from '@ant-design/icons'
import {EnterpriseLayout} from '@egon-cola/admin-web-shared'
import {useMutation, useQuery} from '@tanstack/react-query'
import {Alert, Form, Input, Modal} from 'antd'
import type {PropsWithChildren} from 'react'
import {useState} from 'react'
import {portalAuth, type PortalIdentity} from '../auth/portalAuth'
import {navigationFor} from '../pages/PortalHomePage'
import './portal.css'

interface LoginForm {
  readonly tenantId: string
  readonly username: string
  readonly password: string
}

/** Host-owned brand and sign-in controls; each child still owns its authorization. */
export const PortalLayout = ({children}: PropsWithChildren) => {
  const [loginOpen, setLoginOpen] = useState(false)
  const [form] = Form.useForm<LoginForm>()
  const identity = useQuery({
    queryKey: ['portal-identity'],
    queryFn: () => portalAuth.userInfo<PortalIdentity>(),
    retry: false,
    refetchOnWindowFocus: true,
  })
  const login = useMutation({
    mutationFn: (values: LoginForm) => portalAuth.login(values),
    // Reload prevents a mounted child from retaining another account's authorization cache.
    onSuccess: () => window.location.reload(),
  })
  const logout = useMutation({
    mutationFn: () => portalAuth.logout(),
    onSuccess: () => window.location.reload(),
  })
  const user = identity.data?.sub ? identity.data : undefined

  return (
    <div className="portal-layout">
      <EnterpriseLayout config={{
        platformName: 'Egon COLA Platform',
        logo: <img src="/favicon.png" alt="Egon COLA Platform" width={28} height={28}
          style={{display: 'block', objectFit: 'contain'}} />,
        navigation: navigationFor(),
        hideFooter: true,
        contentStyle: {padding: 0, minHeight: 0, flex: '1 1 0', display: 'flex', overflow: 'hidden'},
        user: user ? {
          name: user.sub,
          description: `租户 ${user.tid}`,
          menu: [{key: 'logout', label: logout.isPending ? '正在退出…' : '退出登录',
            icon: <LogoutOutlined />, disabled: logout.isPending, onClick: () => logout.mutate()}],
        } : {
          name: identity.isPending ? '检查登录…' : '登录',
          avatar: <LoginOutlined />,
          menu: [{key: 'login', label: '登录', icon: <LoginOutlined />,
            onClick: () => {login.reset(); form.resetFields(); setLoginOpen(true)}}],
        },
      }}>
        <div className="portal-content">
          {logout.error && <Alert type="error" showIcon title="退出登录失败，请重试" />}
          {children}
        </div>
      </EnterpriseLayout>
      <Modal title="登录 Egon COLA Platform" open={loginOpen} okText="登录"
        confirmLoading={login.isPending} onOk={() => form.submit()}
        onCancel={() => {setLoginOpen(false); form.resetFields(); login.reset()}} destroyOnHidden>
        {login.error && <Alert type="error" showIcon title="登录失败，请检查租户和账号信息后重试"
          style={{marginBottom: 16}} />}
        <Form<LoginForm> form={form} layout="vertical" preserve={false}
          initialValues={{tenantId: import.meta.env.VITE_DEFAULT_TENANT_ID ?? ''}}
          onFinish={(values: LoginForm) => login.mutate(values)}>
          <Form.Item name="tenantId" label="租户 ID" rules={[{required: true}]}>
            <Input autoComplete="organization" />
          </Form.Item>
          <Form.Item name="username" label="用户名" rules={[{required: true}]}>
            <Input autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{required: true}]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
