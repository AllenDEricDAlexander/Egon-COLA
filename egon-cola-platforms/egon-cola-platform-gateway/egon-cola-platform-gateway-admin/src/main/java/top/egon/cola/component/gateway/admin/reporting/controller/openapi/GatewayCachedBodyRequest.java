package top.egon.cola.component.gateway.admin.reporting.controller.openapi;


import top.egon.cola.component.gateway.admin.application.controller.*;
import top.egon.cola.component.gateway.admin.application.domain.dto.*;
import top.egon.cola.component.gateway.admin.application.domain.exception.*;
import top.egon.cola.component.gateway.admin.application.domain.po.*;
import top.egon.cola.component.gateway.admin.application.domain.vo.*;
import top.egon.cola.component.gateway.admin.application.repository.*;
import top.egon.cola.component.gateway.admin.application.service.*;
import top.egon.cola.component.gateway.admin.auth.controller.*;
import top.egon.cola.component.gateway.admin.auth.domain.vo.*;
import top.egon.cola.component.gateway.admin.auth.service.*;
import top.egon.cola.component.gateway.admin.bootstrap.*;
import top.egon.cola.component.gateway.admin.catalog.controller.*;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.*;
import top.egon.cola.component.gateway.admin.catalog.domain.enums.*;
import top.egon.cola.component.gateway.admin.catalog.domain.po.*;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.*;
import top.egon.cola.component.gateway.admin.catalog.repository.*;
import top.egon.cola.component.gateway.admin.catalog.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.catalog.service.*;
import top.egon.cola.component.gateway.admin.config.*;
import top.egon.cola.component.gateway.admin.config.properties.*;
import top.egon.cola.component.gateway.admin.credential.controller.*;
import top.egon.cola.component.gateway.admin.credential.domain.dto.*;
import top.egon.cola.component.gateway.admin.credential.domain.po.*;
import top.egon.cola.component.gateway.admin.credential.domain.vo.*;
import top.egon.cola.component.gateway.admin.credential.repository.*;
import top.egon.cola.component.gateway.admin.credential.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.credential.service.*;
import top.egon.cola.component.gateway.admin.group.controller.*;
import top.egon.cola.component.gateway.admin.group.domain.dto.*;
import top.egon.cola.component.gateway.admin.group.domain.po.*;
import top.egon.cola.component.gateway.admin.group.domain.vo.*;
import top.egon.cola.component.gateway.admin.group.repository.*;
import top.egon.cola.component.gateway.admin.group.service.*;
import top.egon.cola.component.gateway.admin.mcp.controller.*;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.*;
import top.egon.cola.component.gateway.admin.mcp.domain.enums.*;
import top.egon.cola.component.gateway.admin.mcp.domain.exception.*;
import top.egon.cola.component.gateway.admin.mcp.domain.po.*;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.*;
import top.egon.cola.component.gateway.admin.mcp.repository.*;
import top.egon.cola.component.gateway.admin.mcp.repository.filesystem.*;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.mcp.service.*;
import top.egon.cola.component.gateway.admin.observability.controller.*;
import top.egon.cola.component.gateway.admin.observability.controller.message.*;
import top.egon.cola.component.gateway.admin.observability.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.observability.domain.dto.*;
import top.egon.cola.component.gateway.admin.observability.domain.enums.*;
import top.egon.cola.component.gateway.admin.observability.domain.po.*;
import top.egon.cola.component.gateway.admin.observability.domain.vo.*;
import top.egon.cola.component.gateway.admin.observability.repository.*;
import top.egon.cola.component.gateway.admin.observability.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.observability.service.*;
import top.egon.cola.component.gateway.admin.release.controller.*;
import top.egon.cola.component.gateway.admin.release.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.release.domain.*;
import top.egon.cola.component.gateway.admin.release.domain.dto.*;
import top.egon.cola.component.gateway.admin.release.domain.enums.*;
import top.egon.cola.component.gateway.admin.release.domain.po.*;
import top.egon.cola.component.gateway.admin.release.domain.vo.*;
import top.egon.cola.component.gateway.admin.release.repository.*;
import top.egon.cola.component.gateway.admin.release.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.release.service.*;
import top.egon.cola.component.gateway.admin.reporting.controller.openapi.*;
import top.egon.cola.component.gateway.admin.reporting.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.reporting.domain.dto.*;
import top.egon.cola.component.gateway.admin.reporting.domain.po.*;
import top.egon.cola.component.gateway.admin.reporting.domain.vo.*;
import top.egon.cola.component.gateway.admin.reporting.repository.*;
import top.egon.cola.component.gateway.admin.reporting.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.reporting.service.*;
import top.egon.cola.component.gateway.admin.routing.controller.*;
import top.egon.cola.component.gateway.admin.routing.domain.*;
import top.egon.cola.component.gateway.admin.routing.domain.dto.*;
import top.egon.cola.component.gateway.admin.routing.domain.po.*;
import top.egon.cola.component.gateway.admin.routing.domain.vo.*;
import top.egon.cola.component.gateway.admin.routing.repository.*;
import top.egon.cola.component.gateway.admin.routing.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.routing.service.*;
import top.egon.cola.component.gateway.admin.rule.domain.dto.*;
import top.egon.cola.component.gateway.admin.rule.domain.vo.*;
import top.egon.cola.component.gateway.admin.rule.service.*;
import top.egon.cola.component.gateway.admin.runtime.controller.*;
import top.egon.cola.component.gateway.admin.runtime.domain.dto.*;
import top.egon.cola.component.gateway.admin.runtime.domain.vo.*;
import top.egon.cola.component.gateway.admin.runtime.service.*;
import top.egon.cola.component.gateway.admin.scope.controller.*;
import top.egon.cola.component.gateway.admin.scope.domain.*;
import top.egon.cola.component.gateway.admin.scope.domain.dto.*;
import top.egon.cola.component.gateway.admin.scope.domain.vo.*;
import top.egon.cola.component.gateway.admin.scope.service.*;
import top.egon.cola.component.gateway.admin.shared.controller.*;
import top.egon.cola.component.gateway.admin.shared.domain.*;
import top.egon.cola.component.gateway.admin.shared.domain.enums.*;
import top.egon.cola.component.gateway.admin.shared.domain.exception.*;
import top.egon.cola.component.gateway.admin.shared.domain.po.*;
import top.egon.cola.component.gateway.admin.shared.domain.vo.*;
import top.egon.cola.component.gateway.admin.shared.repository.*;
import top.egon.cola.component.gateway.admin.shared.repository.jdbc.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.component.gateway.admin.credential.repository.GatewayCredentialRepository;
import top.egon.cola.component.gateway.admin.credential.service.GatewaySecretProtector;
import top.egon.cola.component.gateway.admin.reporting.repository.GatewayHmacNonceRepository;
import top.egon.cola.component.gateway.admin.reporting.service.GatewayReportAuthentication;
import top.egon.cola.component.gateway.admin.application.domain.po.GatewayApplicationPO;
import top.egon.cola.component.gateway.admin.application.repository.GatewayApplicationRepository;
import top.egon.cola.component.gateway.contract.reporting.GatewayCanonicalRequest;
import top.egon.cola.component.gateway.contract.reporting.GatewayRequestSigner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * 中文说明：{@code GatewayCachedBodyRequest} 是类型，位于当前 Gateway 模块的相关包中，负责CachedBody请求相关的职责与边界。
 * English summary: {@code GatewayCachedBodyRequest} is a type in the current Gateway module; it owns the cached body request-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayCachedBodyRequest
        extends HttpServletRequestWrapper {

    /**
     * 中文说明：保存 body 对应的状态、依赖或配置值；字段类型为 {@code byte[]}，由 {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by body; its type is {@code byte[]}, and {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest}; do not couple callers to its representation when the owning type exposes an API.
     */
    final byte[] body;

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param request 参数 请求；parameter request。
     */
    GatewayCachedBodyRequest(HttpServletRequest request)
            throws IOException {
        super(request);
        body = request.getInputStream().readAllBytes();
    }

    /**
     * 中文说明：执行 getInputStream 操作；该方法是 {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get input stream operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest.getInputStream(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getInputStream 的处理结果；returns the result of the operation.
     */
    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream input = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            /**
             * 中文说明：执行 isFinished 操作；该方法是 {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the is finished operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest.isFinished(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @return 返回 isFinished 的处理结果；returns the result of the operation.
             */
            @Override
            public boolean isFinished() {
                return input.available() == 0;
            }

            /**
             * 中文说明：执行 isReady 操作；该方法是 {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the is ready operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest.isReady(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @return 返回 isReady 的处理结果；returns the result of the operation.
             */
            @Override
            public boolean isReady() {
                return true;
            }

            /**
             * 中文说明：执行 setRead监听器 操作；该方法是 {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the set read listener operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest.setReadListener(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @param readListener 参数 read监听器；parameter read listener。
             */
            @Override
            public void setReadListener(ReadListener readListener) {
            }

            /**
             * 中文说明：执行 read 操作；该方法是 {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the read operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @return 返回 read 的处理结果；returns the result of the operation.
             */
            @Override
            public int read() {
                return input.read();
            }
        };
    }

    /**
     * 中文说明：执行 getReader 操作；该方法是 {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get reader operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayCachedBodyRequest.getReader(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getReader 的处理结果；returns the result of the operation.
     */
    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(
                getInputStream(),
                StandardCharsets.UTF_8
        ));
    }
}
