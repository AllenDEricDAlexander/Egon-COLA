# DDC 配置编辑格式优化设计

## 背景

DDC Admin 当前直接把 `configValue` 原文放进列表和文本框。普通 JSON 因为是单行文本而难读，
`gateway.rules.active` 更包含 Gateway 发布所需的 activation、snapshot、checksum 等运行元数据，
真正允许人编辑的规则内容位于 `inlineSnapshot.content`。

## 目标

- 配置列表单独展示自动识别的文件类型：JSON、YAML、TOML 或 TXT。
- JSON 在编辑器中自动缩进，YAML、TOML、TXT 保留原始文本格式。
- 对已识别的系统发布外壳，只向用户展示真正的配置内容。
- 保存 Gateway 内联规则时自动把内容合并回原外壳，并重算内容校验和、产物校验和和字节长度。
- 不删除普通业务 JSON 中恰好名为 `data`、`version` 或 `metadata` 的字段。

## 方案比较

### 方案一：前端配置展示适配器（采用）

新增纯 JavaScript 模块，把后端配置记录转换成编辑模型，并把编辑结果转换回持久化值。
文件类型通过配置键扩展名、`valueType` 和内容特征自动识别，不改变 DDC 运行时契约。

优点是改动集中、无需数据库迁移、旧配置立即生效，并能针对 Gateway 外壳安全地往返转换。
缺点是新增的系统外壳类型需要显式增加适配规则。

### 方案二：给 DDC 表增加 `file_type` 字段

文件类型可以持久化，但需要 PostgreSQL 和 SQLite 迁移、管理 API 与客户端契约扩展；仍不能解决
Gateway 发布外壳的内容提取和校验和重建，因此本次不采用。

### 方案三：仅把 JSON 进行格式化

实现最小，但 Gateway 元数据仍会占满编辑器，也无法区分 YAML、TOML 和 TXT，因此不满足需求。

## 展示适配器

`config-format.mjs` 提供三个边界：

- `detectConfigFormat(config)`：优先识别 `.yaml/.yml/.toml/.json/.txt` 等扩展名，随后识别
  Gateway 内联规则、`valueType=JSON` 和内容特征，最终回退到 TXT。
- `prepareConfigEditor(config)`：普通 JSON 返回缩进文本；识别到 Gateway INLINE activation 时，
  解析 `inlineSnapshot` 并只返回 `snapshot.content`。
- `serializeConfigEditor(editor)`：普通内容按编辑结果保存；Gateway 内容按现有 canonical JSON 规则
  重建 snapshot 和 activation，更新 `ruleContentSha256`、`artifactSha256`、`totalSize`。

提取规则必须满足完整结构特征：外层为 INLINE activation，`inlineSnapshot` 是可解析 JSON，内层同时
包含 `content`、`releaseId`、`generatedAt`、`ruleSchemaVersion`。不会按字段名递归删除任意内容。

## 页面交互

- 配置列表把原“类型”拆为“值类型”和“文件类型”，当前值预览使用适配后的配置内容。
- 编辑弹窗增加只读的“文件类型”和提示文本，说明类型是自动识别以及是否隐藏了系统元数据。
- 编辑配置时保留原始 `configValue` 的适配上下文；提交前进行 JSON 校验和反向转换。
- 新建或修改键名、值类型、内容时即时刷新文件类型提示。
- 无法解析声明为 JSON 的内容时阻止保存并显示明确错误；YAML、TOML、TXT 原样保存。

## 兼容性与安全

- `valueType` 继续表示 DDC 的运行时值转换类型，不与文件格式混用。
- 不新增依赖，不修改数据库，不修改 DDC Starter 或 Gateway 运行契约。
- 仅 INLINE Gateway activation 支持内容级编辑；其他 JSON 完整展示，避免猜测性丢字段。
- SHA-256 使用浏览器原生 Web Crypto；canonical JSON 对对象键排序、忽略 null，并保持数组顺序，
  同时兼容 Gateway 对空 `parameters` 的省略规则。

## 测试

- Node 内置测试验证 JSON/YAML/TOML/TXT 识别和普通业务字段不被误删。
- 以固定 Gateway activation fixture 验证只展示 `content`，保存后元数据保留、校验和与长度为固定值。
- Java classpath 资源测试验证新模块随 Admin JAR 一同打包。
- 运行 JavaScript 测试、DDC Admin Maven 测试、打包和 `git diff --check`。

## 设计模式判断

采用 Adapter：它隔离后端持久化值与编辑器展示值的差异，并把 Gateway 特有的往返规则封装在单一
模块中。Strategy 或 Factory 会为当前两个分支增加多余接口层；数据库类型字段也不能解决发布外壳，
因此保持纯函数 Adapter 更符合现有原生 JavaScript 页面风格。
