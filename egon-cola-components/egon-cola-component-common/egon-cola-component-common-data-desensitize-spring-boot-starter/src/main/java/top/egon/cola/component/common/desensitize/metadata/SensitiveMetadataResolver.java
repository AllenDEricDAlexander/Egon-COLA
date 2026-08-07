package top.egon.cola.component.common.desensitize.metadata;

import top.egon.cola.component.common.desensitize.annotation.Sensitive;
import top.egon.cola.component.common.desensitize.annotation.SensitiveScene;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Optional;

public class SensitiveMetadataResolver {

    public Optional<Sensitive> resolve(AnnotatedElement element, SensitiveScene scene) {
        if (element == null) {
            return Optional.empty();
        }
        return resolve(element.getAnnotation(Sensitive.class), scene);
    }

    public Optional<Sensitive> resolve(Sensitive sensitive, SensitiveScene scene) {
        if (sensitive == null || scene == null) {
            return Optional.empty();
        }
        return Arrays.stream(sensitive.scenes())
                .filter(candidate -> candidate == scene)
                .findFirst()
                .map(ignored -> sensitive);
    }
}
