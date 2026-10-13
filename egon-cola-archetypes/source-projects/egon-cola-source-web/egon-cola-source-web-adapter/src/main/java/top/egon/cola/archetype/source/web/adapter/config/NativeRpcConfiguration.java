package top.egon.cola.archetype.source.web.adapter.config;

import io.grpc.Status;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import top.egon.cola.archetype.source.web.adapter.pojo.convertor.OrganizationFacadeConverter;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.common.exception.EgonRpcRejectedException;
import top.egon.cola.component.rpc.provider.server.RpcProviderExceptionMapper;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcSecurityContext;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/** Wires typed conversion and Jakarta validation into native RPC adapters. */
@Configuration(value = "nativeRpcConfiguration", proxyBeanMethods = false)
@EnableMethodSecurity
@Slf4j
public class NativeRpcConfiguration {
    /** Enables the SDK's composed SERVICE-scope annotation on the actual provider proxies. */
    @Bean("annotationTemplateExpressionDefaults")
    static AnnotationTemplateExpressionDefaults annotationTemplateExpressionDefaults() {
        return new AnnotationTemplateExpressionDefaults();
    }

    @Bean("organizationFacadeConverter")
    OrganizationFacadeConverter organizationFacadeConverter() {
        return Mappers.getMapper(OrganizationFacadeConverter.class);
    }

    @Bean("nativeRpcValidation")
    @Primary
    ValidationUtils nativeRpcValidation(@Qualifier("egonColaValidationUtils") ValidationUtils validation) {
        return validation;
    }

    @Bean("nativeRpcValidationExceptionMapper")
    RpcProviderExceptionMapper nativeRpcValidationExceptionMapper(
            @Qualifier("idpObservedVerificationComponent") ObjectProvider<IdpObservedVerificationComponent> observers) {
        return failure -> {
            if (failure instanceof AuthenticationException) {
                return Optional.of(Status.UNAUTHENTICATED.withDescription("jwt_invalid").asRuntimeException());
            }
            if (failure instanceof AccessDeniedException || failure instanceof EgonRpcRejectedException) {
                var proof = IdpRpcSecurityContext.currentProofOrNull();
                var marker = IdpRpcSecurityContext.currentAuthorizationObservationContext();
                var observations = observers.getIfAvailable();
                boolean confirmed = observations != null && proof != null && marker != null && marker.isActive()
                        && marker.denyOnce(() -> observations.authorizationDenied(proof)) && marker.isConfirmed();
                return Optional.of(confirmed
                        ? Status.PERMISSION_DENIED.withDescription("service_scope_denied").asRuntimeException()
                        : Status.UNAVAILABLE.withDescription("oauth_audit_delivery_unavailable").asRuntimeException());
            }
            if (failure instanceof ConstraintViolationException || failure instanceof DateTimeParseException) {
                return Optional.of(Status.INVALID_ARGUMENT.withDescription("Invalid RPC request").asRuntimeException());
            }
            return Optional.empty();
        };
    }
}
