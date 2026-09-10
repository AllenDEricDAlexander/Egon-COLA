# Egon-COLA RAG Starter

[English](README.md) | [中文](README.zh-CN.md)

## What This Is

`egon-cola-component-rag-starter` provides the **mechanics** of retrieval-augmented generation:
document extraction, chunking, embedding into a vector store, and similarity retrieval. It is the
engineering half of a RAG feature, not a knowledge base.

The component owns no provider configuration, no API key, no schema and no HTTP surface. It never
creates an `EmbeddingModel` or a `VectorStore`; the host application provides both as named beans and
the component resolves them by name.

Everything a knowledge base needs beyond the engine — knowledge base and document tables, CRUD APIs,
upload, asynchronous ingestion, permissions — belongs to the consuming application.

## Version Matrix

| Component | Version |
| --- | --- |
| Java | 21 |
| Spring Boot | 3.5.16 |
| Spring AI | 1.1.8 |

## Adding It

Import the Components BOM and depend on the starter without a version:

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

PDF and Office parsing are optional: add `spring-ai-pdf-document-reader` or
`spring-ai-tika-document-reader` to enable them. Without them the application still starts and a
request for those formats fails with a diagnostic listing the registered capabilities.

## What the Host Must Provide

Two named beans, referenced by configuration rather than by type:

```java
@Bean("knowledgeEmbeddingModel")
EmbeddingModel knowledgeEmbeddingModel() { /* OpenAI-compatible, or any other provider */ }

@Bean("knowledgeVectorStore")
VectorStore knowledgeVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel knowledgeEmbeddingModel) {
    return PgVectorStore.builder(jdbcTemplate, knowledgeEmbeddingModel)
            .dimensions(1536)
            .build();
}
```

Credentials, endpoints, model names and the vector store's own schema stay with the host. The
component never logs them and never needs them.

## Configuration

```yaml
egon:
  cola:
    component:
      rag:
        enabled: true                 # default false: nothing is created until this is true
        dimensions: 1536              # every registered model must report this dimension count
        vector-store-bean-name: knowledgeVectorStore
        embedding-models:
          openai-small:
            embedding-model-bean-name: knowledgeEmbeddingModel
        default-embedding-model: openai-small
        storage:
          type: LOCAL                 # the only built-in value
          local:
            root: ./data/rag-documents
        retrieval:
          default-top-k: 8
          max-top-k: 50
        validation:
          probe-on-startup: false     # see the cost note below
```

Unknown keys, missing required values and out-of-range values fail start-up rather than degrading.
Every logical embedding model must report the configured dimension count; a mismatch fails start-up
and names the model.

## Java API

```java
// 1. extract once, then persist the text yourself
ExtractedDocumentBO extracted = extractionService.extract(
        new RagExtractionCommand("report.pdf", "application/pdf", bytes));

// 2. chunk and embed from the text you stored - never re-reading the original
RagIngestionResult result = ingestionService.ingest(new RagIngestionCommand(
        "kb-1001", "doc-01J5K9", "openai-small", chunkingConfig, extracted, Map.of("source", "manual")));

// 3. retrieve within one collection and one model
List<RagRetrievedChunkBO> chunks = retrievalService.retrieve(new RagRetrievalQuery(
        "kb-1001", "openai-small", "how do I configure the timeout", 8, 0.0, Map.of()));
```

Extraction and ingestion are separate methods on purpose: a consumer normally persists the extracted
text before embedding, so a retried embedding never re-reads or re-parses the original document.
That is why `ingest` accepts an `ExtractedDocumentBO` and not a byte stream.

## Extension Points

| SPI | Purpose | Built-in |
| --- | --- | --- |
| `RagDocumentExtractor` | read a format into text | plain text family, Markdown, optional PDF and Tika |
| `RagChunkingStrategy` | split text into chunks | `TOKEN`, `MARKDOWN_HEADING`, `RECURSIVE` |
| `RagDocumentStorage` | keep the raw upload | local file system |
| `RagEmbeddingModelRegistry` | resolve logical model names | configuration driven, no implementation needed |

Extractors declare an `order()`; the lowest value wins and the choice is logged. Two extractors that
share an order and claim the same format fail start-up, so routing never depends on bean ordering.
Host implementations are collected automatically and can replace a built-in one.

## Execution and Failure Semantics

- **No transactions, no persistence.** The component writes only to the vector store and the storage
  SPI; vector store transaction semantics belong to the host's implementation.
- **No retry, no scheduling.** A failure propagates; the caller decides whether to retry.
- **Ingestion deletes first, then writes.** Combined with deterministic chunk identifiers
  (`documentId + ':' + chunkIndex`) that makes a re-run idempotent. A re-run re-embeds, which costs
  money again; it never re-reads or re-parses the document.
- **Retrieval never degrades.** A dependency failure raises rather than returning an empty list,
  because "nothing matched" and "the search failed" mean opposite things to a caller.
- **Collection and model filters are forced.** They are built by the component, so a caller cannot
  omit them. The vector table is shared, and a missing filter would silently return another
  collection's or another model's chunks.

## Boundaries and Logging

The component logs identifiers, counts, durations, outcomes and error types only. Document content,
chunk text, vectors, query text and credentials never reach the log. Metrics tags are limited to
outcome, model and strategy; they never include collection or document identifiers.

## Operational Notes

1. **The component does not create the vector table.** The host must migrate it before enabling the
   component. `PgVectorStore.initializeSchema(true)` or an equivalent migration both work.
2. **`dimensions` must match the vector table.** With the probe off, a mismatch is not detected at
   start-up; it surfaces when the first write fails.
3. **The probe is off by default and costs money when on.** Enabling `probe-on-startup` performs one
   real embedding call and one vector write per model at every start-up. It validates that the table
   exists, its dimension matches, the extension is installed and filtering and deletion work — none
   of which the offline dimension check can see. Leave it off in production.
4. **Local storage is single-instance only.** A multi-instance deployment must replace
   `RagDocumentStorage` with a shared backing.

## Upgrade and Verification

Enumerations only gain values; SPI interfaces only gain default methods; configuration keys only gain
optional keys. `mvn -pl egon-cola-components/egon-cola-component-rag-starter clean verify` runs the
whole offline suite, which needs no credentials and no external service.
