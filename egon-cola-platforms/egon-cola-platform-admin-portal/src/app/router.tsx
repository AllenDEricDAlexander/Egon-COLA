import { Result } from 'antd'
import { Route, Routes, useParams } from 'react-router-dom'
import {
  EnterpriseLayout,
  PageHeader,
} from '@egon-cola/admin-web-shared'
import { PORTAL_HOST_VERSION } from '../manifest/loader'
import { ChildRoutePage } from '../pages/ChildRoutePage'
import { PORTAL_PLATFORM_ITEMS, PortalHomePage, navigationFor } from '../pages/PortalHomePage'

const StandaloneRedirectPage = () => {
  const { platformKey } = useParams()
  const item = PORTAL_PLATFORM_ITEMS.find((candidate) => candidate.key === platformKey)
  if (!item) {
    return <Result status="404" title="平台不存在" subTitle="请返回平台工作台选择有效的子应用。" />
  }

  return (
    <EnterpriseLayout
      config={{
        platformName: 'Egon COLA Platform',
        navigation: navigationFor(),
        footer: { version: PORTAL_HOST_VERSION },
      }}
    >
      <PageHeader title={`${item.label}独立入口`} subtitle="独立入口由子应用自行完成认证与业务加载" />
      <Result
        status="info"
        title="准备打开独立子应用"
        subTitle="如宿主挂载不可用，可使用该入口直接访问子应用。"
        extra={<a href={item.standaloneUrl}>独立打开 {item.label}</a>}
      />
    </EnterpriseLayout>
  )
}

export const PortalRouter = () => (
  <Routes>
    <Route path="/" element={<PortalHomePage />} />
    <Route path="/platform/:platformKey/*" element={<ChildRoutePage />} />
    <Route path="/standalone/:platformKey" element={<StandaloneRedirectPage />} />
    <Route path="*" element={<Result status="404" title="页面不存在" subTitle="请从左侧平台菜单进入页面。" />} />
  </Routes>
)
