package top.egon.cola.component.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonUtilsTest {

    enum Status implements CodeEnum<String> {
        ENABLED("1"),
        DISABLED("0");

        private final String code;

        Status(String code) {
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }
    }

    static class UserNode {
        private final Long id;
        private final Long parentId;
        private final String name;
        private List<UserNode> children = List.of();

        UserNode(Long id, Long parentId, String name) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
        }

        Long getId() {
            return id;
        }

        Long getParentId() {
            return parentId;
        }

        String getName() {
            return name;
        }

        List<UserNode> getChildren() {
            return children;
        }

        void setChildren(List<UserNode> children) {
            this.children = children;
        }
    }

    static class JsonUser {
        private String name;
        private int age;

        JsonUser() {
        }

        JsonUser(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    @Test
    void stringUtilitiesHandleBlankDefaultAndTruncate() {
        assertTrue(StringUtils.isBlank(" "));
        assertEquals("fallback", StringUtils.defaultIfBlank(" ", "fallback"));
        assertEquals("abc", StringUtils.truncate("abcdef", 3));
        assertEquals("hello", StringUtils.normalize("  hello  "));
    }

    @Test
    void collectionUtilitiesHandleNullSafely() {
        assertTrue(CollectionUtils.isEmpty(null));
        assertEquals(0, CollectionUtils.size(null));
        assertEquals("a", CollectionUtils.first(List.of("a", "b")));
        assertEquals("b", CollectionUtils.last(List.of("a", "b")));
        assertEquals(List.of("A", "B"), CollectionUtils.map(List.of("a", "b"), String::toUpperCase));
        assertEquals(List.of("b"), CollectionUtils.filter(List.of("a", "b"), value -> value.compareTo("b") >= 0));
    }

    @Test
    void dateUtilitiesFormatParseAndCalculateDayBounds() {
        LocalDateTime time = LocalDateTime.of(2026, 7, 7, 12, 30, 0);

        assertEquals("2026-07-07 12:30:00", DateTimeUtils.format(time, "yyyy-MM-dd HH:mm:ss"));
        assertEquals(time, DateTimeUtils.parseDateTime("2026-07-07 12:30:00", "yyyy-MM-dd HH:mm:ss"));
        assertEquals(LocalDate.of(2026, 7, 7).atStartOfDay(), DateTimeUtils.startOfDay(LocalDate.of(2026, 7, 7)));
        assertEquals(LocalDateTime.of(2026, 7, 7, 23, 59, 59, 999_999_999), DateTimeUtils.endOfDay(LocalDate.of(2026, 7, 7)));
        assertEquals(time, DateTimeUtils.fromEpochMillis(DateTimeUtils.toEpochMillis(time)));
    }

    @Test
    void idUtilitiesCreateUuidValues() {
        String uuid = IdUtils.uuid();
        String simpleUuid = IdUtils.simpleUuid();

        assertEquals(36, uuid.length());
        assertEquals(32, simpleUuid.length());
        assertEquals(16, IdUtils.shortUuid().length());
        assertNotEquals(uuid, simpleUuid);
    }

    @Test
    void enumUtilitiesFindByNameAndCode() {
        assertEquals(Status.ENABLED, EnumUtils.getByName(Status.class, "ENABLED"));
        assertEquals(Status.DISABLED, EnumUtils.getByCode(Status.class, "0"));
        assertTrue(EnumUtils.containsName(Status.class, "ENABLED"));
        assertFalse(EnumUtils.containsName(Status.class, "UNKNOWN"));
    }

    @Test
    void jsonUtilitiesSerializeAndDeserialize() {
        String json = JsonUtils.toJson(new JsonUser("egon", 18));

        JsonUser user = JsonUtils.fromJson(json, JsonUser.class);
        List<JsonUser> users = JsonUtils.fromJsonList("[{\"name\":\"egon\",\"age\":18}]", JsonUser.class);
        JsonUser converted = JsonUtils.convert(JsonUtils.toMap(json), JsonUser.class);

        assertEquals("egon", user.getName());
        assertEquals(18, user.getAge());
        assertEquals("egon", users.get(0).getName());
        assertEquals("egon", converted.getName());
    }

    @Test
    void maskingUtilitiesMaskSensitiveValues() {
        assertEquals("138****8000", MaskingUtils.mobile("13812348000"));
        assertEquals("e***@example.com", MaskingUtils.email("egon@example.com"));
        assertEquals("110101********1234", MaskingUtils.idCard("110101199001011234"));
        assertEquals("6222**********1234", MaskingUtils.bankCard("622202020202021234"));
        assertEquals("张*", MaskingUtils.name("张三"));
    }

    @Test
    void cryptoUtilitiesDigestEncodeAndHmac() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", CryptoUtils.md5Hex("abc"));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", CryptoUtils.sha256Hex("abc"));
        assertEquals("YWJj", CryptoUtils.base64Encode("abc"));
        assertEquals("abc", CryptoUtils.base64DecodeToString("YWJj"));
        assertEquals("616263", CryptoUtils.hexEncode("abc"));
        assertEquals("9c196e32dc0175f86f4b1cb89289d6619de6bee699e4c378e68309ed97a1a6ab", CryptoUtils.hmacSha256Hex("abc", "key"));
    }

    @Test
    void treeUtilitiesBuildTreeFromFlatNodes() {
        List<UserNode> roots = TreeUtils.build(
                List.of(new UserNode(1L, null, "root"), new UserNode(2L, 1L, "child")),
                UserNode::getId,
                UserNode::getParentId,
                UserNode::setChildren
        );

        assertEquals(1, roots.size());
        assertEquals("root", roots.get(0).getName());
        assertEquals("child", roots.get(0).getChildren().get(0).getName());
    }
}
