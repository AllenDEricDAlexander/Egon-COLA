package top.egon.cola.component.common.desensitize.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.desensitize.annotation.Sensitive;
import top.egon.cola.component.common.desensitize.annotation.SensitiveScene;
import top.egon.cola.component.common.desensitize.annotation.SensitiveType;
import top.egon.cola.component.common.desensitize.metadata.SensitiveMetadataResolver;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SensitiveJacksonModuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(
            new SensitiveJacksonModule(
                    SensitiveStrategyRegistry.defaults(),
                    new SensitiveMetadataResolver()
            )
    );

    @Test
    void masksResponseFieldsWithoutMutatingSourceObject() throws Exception {
        UserDto user = new UserDto(
                1L,
                "张三",
                "13812345678",
                "330106199901011234",
                "internal@example.com",
                null
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(user));

        assertEquals(1L, json.get("id").longValue());
        assertEquals("张*", json.get("name").textValue());
        assertEquals("138****5678", json.get("mobile").textValue());
        assertEquals("330106********1234", json.get("idCard").textValue());
        assertEquals("internal@example.com", json.get("logOnlyEmail").textValue());
        assertNull(json.get("nullableMobile").textValue());
        assertEquals("13812345678", user.getMobile());
        assertEquals("张三", user.getName());
    }

    @Test
    void supportsMethodAnnotation() throws Exception {
        MethodAnnotatedDto source = new MethodAnnotatedDto("6222021234567890");

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(source));

        assertEquals("6222********7890", json.get("bankCard").textValue());
        assertEquals("6222021234567890", source.getBankCard());
    }

    private static class UserDto {

        private final Long id;

        @Sensitive(type = SensitiveType.NAME)
        private final String name;

        @Sensitive(type = SensitiveType.MOBILE)
        private final String mobile;

        @Sensitive(type = SensitiveType.ID_CARD)
        private final String idCard;

        @Sensitive(type = SensitiveType.EMAIL, scenes = SensitiveScene.LOG)
        private final String logOnlyEmail;

        @Sensitive(type = SensitiveType.MOBILE)
        private final String nullableMobile;

        UserDto(Long id,
                String name,
                String mobile,
                String idCard,
                String logOnlyEmail,
                String nullableMobile) {
            this.id = id;
            this.name = name;
            this.mobile = mobile;
            this.idCard = idCard;
            this.logOnlyEmail = logOnlyEmail;
            this.nullableMobile = nullableMobile;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getMobile() {
            return mobile;
        }

        public String getIdCard() {
            return idCard;
        }

        public String getLogOnlyEmail() {
            return logOnlyEmail;
        }

        public String getNullableMobile() {
            return nullableMobile;
        }
    }

    private static class MethodAnnotatedDto {

        private final String bankCard;

        MethodAnnotatedDto(String bankCard) {
            this.bankCard = bankCard;
        }

        @Sensitive(type = SensitiveType.BANK_CARD)
        public String getBankCard() {
            return bankCard;
        }
    }
}
