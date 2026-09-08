package top.egon.cola.archetype.source.light.start.config;

import io.grpc.Status;
import jakarta.validation.ConstraintViolationException;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import top.egon.cola.archetype.source.light.facade.rpc.LightRpcConverter;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.provider.server.RpcProviderExceptionMapper;

import java.time.format.DateTimeParseException;
import java.util.Optional;

/** Wires typed conversion and Jakarta validation into native RPC adapters. */
@Configuration(value = "nativeRpcConfiguration", proxyBeanMethods = false)
public class NativeRpcConfiguration {
    @Bean("lightRpcConverter")
    LightRpcConverter lightRpcConverter() {
        return Mappers.getMapper(LightRpcConverter.class);
    }

    @Bean("nativeRpcValidation")
    @Primary
    ValidationUtils nativeRpcValidation(@Qualifier("egonColaValidationUtils") ValidationUtils validation) {
        return validation;
    }

    @Bean("nativeRpcValidationExceptionMapper")
    RpcProviderExceptionMapper nativeRpcValidationExceptionMapper() {
        return failure -> failure instanceof ConstraintViolationException || failure instanceof DateTimeParseException
                ? Optional.of(Status.INVALID_ARGUMENT.withDescription("Invalid RPC request").asRuntimeException())
                : Optional.empty();
    }
}
