package top.egon.cola.archetype.source.agent.adapter.knowledge.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.archetype.source.agent.application.knowledge.config.KnowledgeRuntimeProperties;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

import java.io.IOException;

/**
 * Opens the tenant scope of every request and closes it again (Spec B §9.2, `REQ-025`).
 *
 * <p>The tenant is the project's thread-scoped channel: the mybatis-plus tenant interceptor reads the
 * key this filter writes, and the ingest delivery thread restores the same key from its task payload.
 * Without it a knowledge read would fail on a missing tenant instead of answering, and a leftover
 * value would scope whatever the next request on that thread touches — so the removal happens in a
 * {@code finally}, on the same thread the value was set.
 *
 * <p>The default tenant is the only source in this version: the request carries no tenant of its own,
 * and authentication, which would supply one, is out of scope. The key name and the value both come
 * from configuration, so the adapter and the component that reads the key cannot drift apart.
 *
 * <p>It is registered for every request rather than only for knowledge paths, because a scoped
 * registration would make the tenant depend on the mapping a request eventually reaches; the only
 * tenant-scoped tables are the knowledge ones, so elsewhere the value is inert.
 */
@Component("knowledgeTenantMdcFilter")
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
@Slf4j
public class KnowledgeTenantMdcFilter extends OncePerRequestFilter {

    @Qualifier("knowledgeRuntimeProperties")
    private final KnowledgeRuntimeProperties properties;

    private final EgonColaMybatisPlusProperties mybatisPlusProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String tenantKey = mybatisPlusProperties.getTenantId().getMdcKey();
        MDC.put(tenantKey, String.valueOf(properties.tenant().defaultId()));
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(tenantKey);
        }
    }
}
