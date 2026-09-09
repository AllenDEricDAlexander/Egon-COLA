package top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.controller.ResourcePermissionMappingController;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePermissionMappingControllerTest {

    @Test
    void exposesDedicatedMappingReadAndWriteEndpoints() {
        RequestMapping mapping = ResourcePermissionMappingController.class
                .getAnnotation(RequestMapping.class);
        assertEquals("/api/tianquan-jianshen/v1/iam/resources/{resourceId}/permission-mapping",
                mapping.value()[0]);

        Method read = Arrays.stream(ResourcePermissionMappingController.class
                        .getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(GetMapping.class))
                .findFirst().orElseThrow();
        Method write = Arrays.stream(ResourcePermissionMappingController.class
                        .getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PutMapping.class))
                .findFirst().orElseThrow();
        assertTrue(read.getGenericReturnType().getTypeName().contains("ResultRecord"));
        assertTrue(write.getGenericReturnType().getTypeName().contains("ResultRecord"));
    }
}
