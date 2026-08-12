package top.egon.cola.component.gateway.admin.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

/**
 * 中文说明：{@code GatewayAdminJwtAuthenticationConverter} 是类型，位于当前 Gateway 模块的相关包中，负责网关管理端JwtAuthenticationConverter相关的职责与边界。
 * English summary: {@code GatewayAdminJwtAuthenticationConverter} is a type in the current Gateway module; it owns the gateway admin jwt authentication converter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayAdminJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    /**
     * 中文说明：执行 convert 操作；该方法是 {@code GatewayAdminJwtAuthenticationConverter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the convert operation; this method is the invocation entry point on {@code GatewayAdminJwtAuthenticationConverter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminJwtAuthenticationConverter.convert(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param jwt 参数 jwt；parameter jwt。
     * @return 返回 convert 的处理结果；returns the result of the operation.
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(
                jwt,
                List.of(),
                jwt.getSubject()
        );
    }
}
