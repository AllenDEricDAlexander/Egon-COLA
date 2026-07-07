# egon-cola-component-common

`egon-cola-component-common` is the aggregator for Egon COLA enterprise common modules.

It is not a business dependency Jar. Business applications should depend on the concrete modules they need.

## Modules

| Module | Responsibility |
|---|---|
| `egon-cola-component-common-core` | Error status, exceptions, enum contracts |
| `egon-cola-component-common-model` | Request, query, and pagination models |
| `egon-cola-component-common-trace` | Trace context based on MDC key `traceId` |
| `egon-cola-component-common-result` | External result DTOs and internal result models |
| `egon-cola-component-common-id` | UUIDv7 and ID generator contracts |
| `egon-cola-component-common-crypto` | SHA-256, HMAC-SHA256, Base64, Hex helpers |
| `egon-cola-component-common-mask` | Stable data masking helpers |
| `egon-cola-component-common-structure` | Tree structure helpers |
| `egon-cola-component-common-test` | Internal common boundary test support |

## Dependency Examples

Use the components BOM:

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
```

Then import only the module you need:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-result</artifactId>
</dependency>
```

## Migration Notes

| Old API | New API |
|---|---|
| `top.egon.cola.component.common.result.Result` | `top.egon.cola.component.common.result.dto.ResultDto` or `top.egon.cola.component.common.result.model.ResultModel` |
| `top.egon.cola.component.common.result.PageResult` | `PageResultDto` or `PageResultModel` |
| `top.egon.cola.component.common.exception.BusinessException` | `EgonBusinessException` |
| `top.egon.cola.component.common.exception.SystemException` | `EgonSystemException` |
| `top.egon.cola.component.common.exception.ErrorCodes` | `CommonStatus` |
| `top.egon.cola.component.common.util.IdUtils` | `UuidV7` or `UuidV7Generator` |
| `top.egon.cola.component.common.util.CryptoUtils` | `Digests`, `Hmacs`, `Base64s`, `Hexes` |
| `top.egon.cola.component.common.util.MaskingUtils` | `Masking` |
| `top.egon.cola.component.common.util.TreeUtils` | `TreeBuilder` |

The old `util` package, `BaseEntity`, and `AuditableModel` are intentionally removed.
