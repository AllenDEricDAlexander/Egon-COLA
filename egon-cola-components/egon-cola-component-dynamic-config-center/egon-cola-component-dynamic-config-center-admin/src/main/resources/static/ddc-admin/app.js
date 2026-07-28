import { uuidV7 } from './uuid.mjs'

const tokenKey = 'egon.ddc.admin.token'

const state = {
  token: sessionStorage.getItem(tokenKey) ?? '',
  services: [],
  instances: [],
  configs: [],
  selectedService: null,
}

const element = (id) => document.getElementById(id)
const loginPanel = element('login-panel')
const workspace = element('workspace')
const message = element('message')
const configDialog = element('config-dialog')

const serviceQueries = [
  { serviceKind: 'HTTP_PROVIDER', protocol: 'http', label: 'HTTP Provider' },
  { serviceKind: 'HTTP_PROVIDER', protocol: 'https', label: 'HTTPS Provider' },
  { serviceKind: 'RPC_PROVIDER', protocol: 'grpc', label: 'RPC Provider' },
  { serviceKind: 'INTERNAL_GATEWAY', protocol: 'grpc', label: 'Internal Gateway' },
]

const query = (values) => {
  const params = new URLSearchParams()
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      params.set(key, String(value).trim())
    }
  })
  return params.toString()
}

const api = async (path, options = {}) => {
  const headers = new Headers(options.headers ?? {})
  headers.set('Authorization', `Bearer ${state.token}`)
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const response = await fetch(path, { ...options, headers })
  const payload = await response.json().catch(() => ({}))
  if (response.status === 401) {
    logout()
    throw new Error('登录已过期，请重新粘贴 Access Token')
  }
  if (!response.ok || payload.success === false) {
    throw new Error(payload.message || payload.code || `请求失败 (${response.status})`)
  }
  return payload.data
}

const showMessage = (text, success = false) => {
  message.textContent = text
  message.classList.remove('hidden', 'success')
  if (success) message.classList.add('success')
}

const clearMessage = () => {
  message.textContent = ''
  message.classList.add('hidden')
  message.classList.remove('success')
}

const setLoggedIn = (loggedIn) => {
  loginPanel.classList.toggle('hidden', loggedIn)
  workspace.classList.toggle('hidden', !loggedIn)
  element('logout-button').classList.toggle('hidden', !loggedIn)
  const status = element('connection-status')
  status.textContent = loggedIn ? 'DDC 已连接' : '未登录'
  status.classList.toggle('status-online', loggedIn)
  status.classList.toggle('status-offline', !loggedIn)
}

const logout = () => {
  state.token = ''
  sessionStorage.removeItem(tokenKey)
  setLoggedIn(false)
}

const textCell = (value, className) => {
  const cell = document.createElement('td')
  cell.textContent = value ?? '—'
  if (className) cell.className = className
  return cell
}

const formatTime = (value) => {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}

const serviceIdentity = (service) => [
  service.serviceKind,
  service.protocol,
  service.serviceName,
  service.group ?? '',
  service.version ?? '',
].join('|')

const loadRegistry = async () => {
  clearMessage()
  const env = element('registry-env').value.trim()
  const namespace = element('registry-namespace').value.trim()
  const snapshots = await Promise.all(serviceQueries.map(async (item) => {
    const data = await api(`/api/v1/ddc/registry/services?${query({
      env,
      namespace,
      serviceKind: item.serviceKind,
      protocol: item.protocol,
    })}`)
    return (data?.services ?? []).map((service) => ({ ...service, label: item.label }))
  }))
  const unique = new Map()
  snapshots.flat().forEach((service) => unique.set(serviceIdentity(service), service))
  state.services = [...unique.values()].sort((left, right) =>
    `${left.serviceKind}:${left.serviceName}`.localeCompare(`${right.serviceKind}:${right.serviceName}`))
  renderServices()
  renderRegistrySummary()
  if (state.selectedService) {
    const selected = state.services.find((item) =>
      serviceIdentity(item) === serviceIdentity(state.selectedService))
    if (selected) await loadInstances(selected)
    else clearInstances('选择左侧服务查看实例')
  }
}

const renderRegistrySummary = () => {
  const counts = {
    'HTTP Provider': state.services.filter((item) => item.serviceKind === 'HTTP_PROVIDER').length,
    'RPC Provider': state.services.filter((item) => item.serviceKind === 'RPC_PROVIDER').length,
    'Internal Gateway': state.services.filter((item) => item.serviceKind === 'INTERNAL_GATEWAY').length,
    '在线实例': state.instances.filter((item) => item.status === 'ONLINE').length,
  }
  const cards = Object.entries(counts).map(([label, value]) => {
    const card = document.createElement('div')
    card.className = 'metric'
    const caption = document.createElement('span')
    caption.textContent = label
    const number = document.createElement('strong')
    number.textContent = value
    card.append(caption, number)
    return card
  })
  element('registry-summary').replaceChildren(...cards)
}

const renderServices = () => {
  const rows = state.services.map((service) => {
    const row = document.createElement('tr')
    row.className = 'clickable'
    if (state.selectedService && serviceIdentity(state.selectedService) === serviceIdentity(service)) {
      row.classList.add('selected')
    }
    row.append(
      textCell(service.label ?? service.serviceKind),
      textCell(service.serviceName, 'mono'),
      textCell(service.protocol),
      textCell(`${service.group || '—'} / ${service.version || '—'}`),
    )
    row.addEventListener('click', () => loadInstances(service).catch(handleError))
    return row
  })
  element('service-table').replaceChildren(...rows)
  element('service-count').textContent = state.services.length
  element('service-empty').classList.toggle('hidden', state.services.length > 0)
}

const loadInstances = async (service) => {
  state.selectedService = service
  renderServices()
  const data = await api(`/api/v1/ddc/registry/instances?${query(service)}`)
  state.instances = data?.instances ?? []
  renderInstances()
  renderRegistrySummary()
}

const clearInstances = (caption) => {
  state.selectedService = null
  state.instances = []
  element('instance-table').replaceChildren()
  element('instance-count').textContent = '0'
  element('instance-empty').textContent = caption
  element('instance-empty').classList.remove('hidden')
  renderRegistrySummary()
}

const renderInstances = () => {
  const rows = state.instances.map((instance) => {
    const row = document.createElement('tr')
    const statusCell = document.createElement('td')
    const badge = document.createElement('span')
    badge.className = `badge ${instance.status === 'ONLINE' ? 'badge-online' : 'badge-neutral'}`
    badge.textContent = instance.status ?? 'UNKNOWN'
    statusCell.append(badge)
    const instanceCell = textCell(instance.instanceId, 'mono')
    if (instance.metadata?.buildId) {
      const build = document.createElement('span')
      build.className = 'subtle'
      build.textContent = instance.metadata.buildId
      instanceCell.append(build)
    }
    row.append(
      statusCell,
      instanceCell,
      textCell(`${instance.secure ? 'tls://' : ''}${instance.host}:${instance.port}`, 'mono'),
      textCell(formatTime(instance.lastHeartbeatAt)),
      textCell(formatTime(instance.expireAt)),
    )
    return row
  })
  element('instance-table').replaceChildren(...rows)
  element('instance-count').textContent = state.instances.length
  element('instance-empty').classList.toggle('hidden', state.instances.length > 0)
}

const scope = () => ({
  appCode: element('config-app').value.trim(),
  env: element('config-env').value.trim(),
  namespace: element('config-namespace').value.trim(),
  configKey: element('config-key-filter').value.trim(),
})

const loadConfigs = async () => {
  clearMessage()
  state.configs = await api(`/api/v1/ddc/configs?${query({ ...scope(), includeDeleted: false })}`) ?? []
  renderConfigs()
}

const renderConfigs = () => {
  const rows = state.configs.map((config) => {
    const row = document.createElement('tr')
    const value = config.configValue ?? ''
    const valueCell = textCell(value.length > 80 ? `${value.slice(0, 80)}…` : value, 'mono')
    if (config.description) {
      const description = document.createElement('span')
      description.className = 'subtle'
      description.textContent = config.description
      valueCell.append(description)
    }
    const actionCell = document.createElement('td')
    const actions = document.createElement('div')
    actions.className = 'actions'
    const edit = actionButton('编辑', 'button-secondary', () => openConfigDialog(config))
    const publish = actionButton('发布', 'button-primary', () => publishConfig(config).catch(handleError))
    actions.append(edit, publish)
    actionCell.append(actions)
    row.append(
      textCell(config.configKey, 'mono'),
      textCell(config.valueType),
      valueCell,
      textCell(config.currentVersion),
      textCell(formatTime(config.updatedAt)),
      actionCell,
    )
    return row
  })
  element('config-table').replaceChildren(...rows)
  element('config-count').textContent = state.configs.length
  element('config-empty').classList.toggle('hidden', state.configs.length > 0)
}

const actionButton = (caption, variant, listener) => {
  const button = document.createElement('button')
  button.type = 'button'
  button.className = `button button-small ${variant}`
  button.textContent = caption
  button.addEventListener('click', listener)
  return button
}

const ensureScope = async (requestedScope = scope()) => {
  clearMessage()
  const current = requestedScope
  const apps = await api('/api/v1/ddc/apps') ?? []
  if (!apps.some((app) => app.appCode === current.appCode)) {
    await api('/api/v1/ddc/apps', {
      method: 'POST',
      body: JSON.stringify({
        appCode: current.appCode,
        appName: current.appCode,
        owner: 'local-admin',
        description: 'Created by DDC Admin Web',
        enabled: true,
      }),
    })
  }
  const namespaces = await api(`/api/v1/ddc/namespaces?${query({
    appCode: current.appCode,
    env: current.env,
  })}`) ?? []
  if (!namespaces.some((item) => item.namespace === current.namespace)) {
    await api('/api/v1/ddc/namespaces', {
      method: 'POST',
      body: JSON.stringify({
        appCode: current.appCode,
        env: current.env,
        namespace: current.namespace,
        description: 'Created by DDC Admin Web',
        enabled: true,
      }),
    })
  }
  showMessage(`作用域 ${current.appCode}/${current.env}/${current.namespace} 已就绪`, true)
}

const openConfigDialog = (config = null) => {
  const current = config ?? scope()
  const editing = Boolean(config?.id)
  element('config-dialog-title').textContent = editing ? '编辑配置' : '新建配置'
  element('config-id').value = config?.id ?? ''
  element('editor-app').value = current.appCode ?? ''
  element('editor-env').value = current.env ?? ''
  element('editor-namespace').value = current.namespace ?? ''
  element('editor-key').value = config?.configKey ?? ''
  element('editor-type').value = config?.valueType ?? 'STRING'
  element('editor-version').value = config?.currentVersion ?? ''
  element('editor-value').value = config?.configValue ?? ''
  element('editor-default').value = config?.defaultValue ?? ''
  element('editor-description').value = config?.description ?? ''
  ;['editor-app', 'editor-env', 'editor-namespace', 'editor-key'].forEach((id) => {
    element(id).readOnly = editing
  })
  element('change-reason-field').classList.toggle('hidden', !editing)
  configDialog.showModal()
}

const saveConfig = async () => {
  const id = element('config-id').value
  if (id) {
    await api(`/api/v1/ddc/configs/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify({
        configValue: element('editor-value').value,
        changeReason: element('editor-change-reason').value || 'DDC Admin Web update',
        currentVersion: Number(element('editor-version').value),
      }),
    })
  } else {
    const editorScope = {
      appCode: element('editor-app').value.trim(),
      env: element('editor-env').value.trim(),
      namespace: element('editor-namespace').value.trim(),
    }
    await ensureScope(editorScope)
    await api('/api/v1/ddc/configs', {
      method: 'POST',
      body: JSON.stringify({
        ...editorScope,
        configKey: element('editor-key').value.trim(),
        configValue: element('editor-value').value,
        defaultValue: element('editor-default').value,
        valueType: element('editor-type').value,
        description: element('editor-description').value,
      }),
    })
  }
  configDialog.close()
  await loadConfigs()
  showMessage('配置已保存', true)
}

const publishConfig = async (config) => {
  if (!window.confirm(`确认发布 ${config.configKey} 当前版本？`)) return
  const result = await api(`/api/v1/ddc/configs/${encodeURIComponent(config.id)}/publish`, {
    method: 'POST',
    body: JSON.stringify({
      changeId: uuidV7(),
      configValue: config.configValue,
      expectedVersion: config.currentVersion,
      timeoutMs: 30000,
    }),
  })
  showMessage(`发布任务 ${result.changeId}：${result.status}`, result.status === 'SUCCESS')
  await loadConfigs()
}

const handleError = (failure) => {
  showMessage(failure instanceof Error ? failure.message : String(failure))
}

element('login-form').addEventListener('submit', async (event) => {
  event.preventDefault()
  state.token = element('access-token').value.trim()
  try {
    await api('/api/v1/ddc/apps')
    sessionStorage.setItem(tokenKey, state.token)
    setLoggedIn(true)
    await loadRegistry()
  } catch (failure) {
    handleError(failure)
  }
})

element('logout-button').addEventListener('click', logout)
element('refresh-registry').addEventListener('click', () => loadRegistry().catch(handleError))
element('ensure-scope').addEventListener('click', () => ensureScope().catch(handleError))
element('new-config').addEventListener('click', () => openConfigDialog())
element('close-config-dialog').addEventListener('click', () => configDialog.close())
element('cancel-config').addEventListener('click', () => configDialog.close())

element('config-filter').addEventListener('submit', (event) => {
  event.preventDefault()
  loadConfigs().catch(handleError)
})

element('config-form').addEventListener('submit', (event) => {
  event.preventDefault()
  saveConfig().catch(handleError)
})

document.querySelectorAll('.tab').forEach((tab) => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.tab').forEach((item) => item.classList.toggle('active', item === tab))
    document.querySelectorAll('.view').forEach((view) => view.classList.add('hidden'))
    element(`${tab.dataset.view}-view`).classList.remove('hidden')
    clearMessage()
    if (tab.dataset.view === 'configs') loadConfigs().catch(handleError)
  })
})

if (state.token) {
  api('/api/v1/ddc/apps')
    .then(() => {
      setLoggedIn(true)
      return loadRegistry()
    })
    .catch(handleError)
} else {
  setLoggedIn(false)
}
