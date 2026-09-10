# Egon-COLA RAG Starter

[English](README.md) | [中文](README.zh-CN.md)

## 这是什么

`egon-cola-component-rag-starter` 提供检索增强生成的**机制**：文档读取、切分、嵌入写入与相似度检索。
它是 RAG 能力的工程化那一半，不是一个知识库。

组件不持有任何供应商配置、密钥或表结构，也不提供 HTTP 接口。它**不创建** `EmbeddingModel` 与
`VectorStore`——宿主应用以具名 Bean 提供两者，组件按名字解析。

知识库在引擎之外需要的一切——知识库与文档表、增删改查接口、上传、异步摄取、权限——都属于引入组件的业务应用。

## 版本矩阵

| 组件 | 版本 |
| --- | --- |
| Java | 21 |
| Spring Boot | 3.5.16 |
| Spring AI | 1.1.8 |

## 依赖方式

引入 Components BOM 后无版本引入本 Starter：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-components-bom</artifactId>
            <version>${egon-cola.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-rag-starter</artifactId>
</dependency>
```

PDF 与 Office 解析是可选能力：加入 `spring-ai-pdf-document-reader` 或 `spring-ai-tika-document-reader`
即可启用。不加也能正常启动，只是请求这些格式时会得到一份列出已注册能力的明确错误。

## 宿主需要提供什么

两个具名 Bean，通过配置引用而不是按类型注入：

```java
@Bean("knowledgeEmbeddingModel")
EmbeddingModel knowledgeEmbeddingModel() { /* OpenAI 兼容，或任何其它供应商 */ }

@Bean("knowledgeVectorStore")
VectorStore knowledgeVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel knowledgeEmbeddingModel) {
    return PgVectorStore.builder(jdbcTemplate, knowledgeEmbeddingModel)
            .dimensions(1536)
            .build();
}
```

凭据、地址、模型名与向量库自身的表结构都留在宿主侧。组件不记录这些信息，也不需要这些信息。

## 配置

```yaml
egon:
  cola:
    component:
      rag:
        enabled: true                 # 缺省 false：不设为 true 时不会创建任何 Bean
        dimensions: 1536              # 每个注册模型都必须报告该维度
        vector-store-bean-name: knowledgeVectorStore
        embedding-models:
          openai-small:
            embedding-model-bean-name: knowledgeEmbeddingModel
        default-embedding-model: openai-small
        storage:
          type: LOCAL                 # 目前唯一的内置取值
          local:
            root: ./data/rag-documents
        retrieval:
          default-top-k: 8
          max-top-k: 50
        validation:
          probe-on-startup: false     # 计费含义见下文
```

未知键、缺少必填项与越界取值都会让启动失败，而不是降级运行。每个逻辑嵌入模型都必须报告配置声明的维度；
不一致时启动失败并指明是哪个模型。

## Java API

```java
// 1. 只解析一次，文本由你自己落库
ExtractedDocumentBO extracted = extractionService.extract(
        new RagExtractionCommand("report.pdf", "application/pdf", bytes));

// 2. 从你已存的文本切分并嵌入——不会重新读取原文件
RagIngestionResult result = ingestionService.ingest(new RagIngestionCommand(
        "kb-1001", "doc-01J5K9", "openai-small", chunkingConfig, extracted, Map.of("source", "manual")));

// 3. 在单一集合与单一模型内检索
List<RagRetrievedChunkBO> chunks = retrievalService.retrieve(new RagRetrievalQuery(
        "kb-1001", "openai-small", "超时怎么配置", 8, 0.0, Map.of()));
```

抽取与摄取的接口是有意分开的：消费方通常先把抽取出的文本持久化，再触发嵌入，这样重跑嵌入时不会重新读取、
重新解析原文件。这也是 `ingest` 接受 `ExtractedDocumentBO` 而不是字节流的原因。

## 扩展点

| SPI | 作用 | 内置实现 |
| --- | --- | --- |
| `RagDocumentExtractor` | 把某种格式读成文本 | 纯文本族、Markdown、可选 PDF 与 Tika |
| `RagChunkingStrategy` | 把文本切成片段 | `TOKEN`、`MARKDOWN_HEADING`、`RECURSIVE` |
| `RagDocumentStorage` | 保存原始上传文件 | 本地文件系统 |
| `RagEmbeddingModelRegistry` | 解析逻辑模型名 | 配置驱动，无需实现 |

抽取器声明 `order()`，数值小者优先，选择结果会记入日志。两个抽取器若共享同一优先级且声明同一种格式，
启动即失败——路由永远不会取决于 Bean 注册顺序。宿主自定义实现会被自动收集，并能整体替换内置实现。

## 执行与失败语义

- **无事务、无持久化。** 组件只写向量库与存储 SPI；向量库的事务语义由其宿主的实现决定。
- **不重试、不调度。** 失败直接向上抛出，是否重试由调用方决定。
- **摄取先删后写。** 配合确定性的分块标识（`documentId + ':' + chunkIndex`），重跑是幂等的。
  重跑会重新计费嵌入，但不会重新读取或重新解析文档。
- **检索不降级。** 依赖失败会抛异常而不是返回空列表——对调用方而言，「没有匹配」与「检索失败」含义相反。
- **集合与模型过滤是强制的。** 过滤条件由组件构造，调用方无法省略。向量表是共享的，漏掉过滤会静默返回
  其它集合或其它模型的分块。

## 边界与日志

组件只记录标识、计数、耗时、结果与错误类型。文档内容、分块文本、向量、查询原文与凭据不会进入日志。
指标标签只包含结果、模型与策略，绝不含集合或文档标识。

## 运维须知

1. **组件不建向量表。** 宿主必须先完成迁移再启用组件；`PgVectorStore.initializeSchema(true)` 或等价迁移均可。
2. **`dimensions` 必须与向量表一致。** 探针关闭时启动期不会发现不一致，会在首次写入失败时暴露。
3. **探针默认关闭，开启后产生真实计费。** 开启 `probe-on-startup` 会在每次启动时对每个模型执行一次真实嵌入
   调用与一次向量写入。它能验证表存在、维度匹配、扩展可用以及过滤与删除可用——这些离线维度校验都看不到。
   生产环境建议保持关闭。
4. **本地存储仅支持单实例。** 多实例部署必须用共享存储替换 `RagDocumentStorage`。

## 升级与验证门禁

枚举只增不改；SPI 接口只增默认方法；配置键只增可选键。
`mvn -pl egon-cola-components/egon-cola-component-rag-starter clean verify` 运行全部离线测试，
不需要任何凭据或外部服务。
