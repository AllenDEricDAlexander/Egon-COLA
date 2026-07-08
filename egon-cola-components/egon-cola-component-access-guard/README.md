# Egon COLA Access Guard

`egon-cola-component-access-guard` provides a Spring Boot starter for method access governance.

Planned capabilities:

- White list short-circuiting
- Redisson global rate limiting
- Temporary blacklist after repeated limit hits
- Timeout protection with fallback and `returnJson`
- Compatibility annotations for existing `DoWhiteList`, `DoRateLimiter`, and `DoHystrix` style usage
- Dynamic configuration provider extension points

The runtime starter artifact exported by the BOM is:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-access-guard-starter</artifactId>
</dependency>
```
