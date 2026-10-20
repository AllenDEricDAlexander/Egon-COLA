package top.egon.cola.component.yaoguang.admin.architecture;

import com.fasterxml.jackson.databind.DeserializationFeature;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import top.egon.cola.component.yaoguang.admin.config.GatewayAiJsonConfiguration;
import top.egon.cola.component.yaoguang.admin.knowledge.domain.dto.KnowledgeBaseCommandDTO;
import top.egon.cola.component.yaoguang.admin.shared.domain.validation.ExecuteGroup;
import top.egon.cola.component.yaoguang.admin.wiki.domain.dto.WikiTransitionCommandDTO;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeCarrierContractTest {
    @Test
    void transitionStillRequiresEventAndRevisionOnlyInExecuteGroup() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new WikiTransitionCommandDTO()).isEmpty());
            Set<String> paths = validator.validate(new WikiTransitionCommandDTO(), ExecuteGroup.class).stream()
                    .map(value -> value.getPropertyPath().toString()).collect(Collectors.toSet());
            assertTrue(paths.contains("event"));
            assertTrue(paths.contains("expectedPageRevision"));
        }
    }

    @Test
    void knowledgeJsonStillRejectsServerDerivedFieldsWithoutChangingGlobalMapper() {
        var converter = new MappingJackson2HttpMessageConverter();
        var converters = new ArrayList<org.springframework.http.converter.HttpMessageConverter<?>>();
        converters.add(converter);
        new GatewayAiJsonConfiguration().extendMessageConverters(converters);
        assertFalse(converter.getObjectMapper().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        var message = new MockHttpInputMessage("{\"name\":\"Knowledge\",\"actorType\":\"USER\"}".getBytes(StandardCharsets.UTF_8));
        message.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        assertThrows(org.springframework.http.converter.HttpMessageNotReadableException.class,
                () -> converter.read(KnowledgeBaseCommandDTO.class, message));
    }
}
