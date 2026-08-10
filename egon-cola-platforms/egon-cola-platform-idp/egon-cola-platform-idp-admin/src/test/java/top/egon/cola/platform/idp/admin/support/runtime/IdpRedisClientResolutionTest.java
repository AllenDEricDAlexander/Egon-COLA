package top.egon.cola.platform.idp.admin.support.runtime;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.MethodParameter;
import top.egon.cola.platform.idp.admin.support.runtime.IdpPlatformConfiguration;
import top.egon.cola.platform.idp.admin.oauth.config.OAuthConfig;
import top.egon.cola.platform.idp.admin.token.config.TokenConfig;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IdpRedisClientResolutionTest {

    @Test
    void resolvesIdentityStateDependenciesWhenDdcRedisClientsExist() {
        RedissonClient runtime = mock(RedissonClient.class);
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.registerBean(
                    "rbac3RuntimeRedissonClient",
                    RedissonClient.class,
                    () -> runtime
            );
            context.registerBean(
                    "ddcRedissonClient",
                    RedissonClient.class,
                    () -> mock(RedissonClient.class)
            );
            context.refresh();

            for (Method method : redisDependentBeanMethods()) {
                MethodParameter parameter = redisParameter(method);
                DependencyDescriptor dependency =
                        new DependencyDescriptor(parameter, true);

                assertThat(context.getDefaultListableBeanFactory()
                        .resolveDependency(dependency, method.getName()))
                        .as("Redis dependency for %s", method.getName())
                        .isSameAs(runtime);
            }
        }
    }

    private static List<Method> redisDependentBeanMethods() {
        return List.of(
                OAuthConfig.class,
                TokenConfig.class,
                IdpPlatformConfiguration.class
        ).stream()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(Bean.class))
                .filter(method -> Arrays.stream(method.getParameterTypes())
                        .anyMatch(RedissonClient.class::equals))
                .toList();
    }

    private static MethodParameter redisParameter(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int index = 0; index < parameterTypes.length; index++) {
            if (parameterTypes[index] == RedissonClient.class) {
                return new MethodParameter(method, index);
            }
        }
        throw new IllegalArgumentException(
                "Bean method has no RedissonClient parameter: " + method
        );
    }
}
