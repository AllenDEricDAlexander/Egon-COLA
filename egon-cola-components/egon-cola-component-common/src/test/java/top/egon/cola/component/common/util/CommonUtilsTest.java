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
        assertTrue(Strings.isBlank(" "));
        assertEquals("fallback", Strings.defaultIfBlank(" ", "fallback"));
        assertEquals("abc", Strings.truncate("abcdef", 3));
        assertEquals("hello", Strings.normalize("  hello  "));
    }

    @Test
    void collectionUtilitiesHandleNullSafely() {
        assertTrue(Collections2.isEmpty(null));
        assertEquals(0, Collections2.size(null));
        assertEquals("a", Collections2.first(List.of("a", "b")));
        assertEquals(List.of("A", "B"), Collections2.map(List.of("a", "b"), String::toUpperCase));
    }

    @Test
    void dateUtilitiesFormatParseAndCalculateDayBounds() {
        LocalDateTime time = LocalDateTime.of(2026, 7, 7, 12, 30, 0);

        assertEquals("2026-07-07 12:30:00", Dates.format(time, "yyyy-MM-dd HH:mm:ss"));
        assertEquals(time, Dates.parseDateTime("2026-07-07 12:30:00", "yyyy-MM-dd HH:mm:ss"));
        assertEquals(LocalDate.of(2026, 7, 7).atStartOfDay(), Dates.startOfDay(LocalDate.of(2026, 7, 7)));
        assertEquals(LocalDateTime.of(2026, 7, 7, 23, 59, 59, 999_999_999), Dates.endOfDay(LocalDate.of(2026, 7, 7)));
    }

    @Test
    void idUtilitiesCreateUuidValues() {
        String uuid = Ids.uuid();
        String simpleUuid = Ids.simpleUuid();

        assertEquals(36, uuid.length());
        assertEquals(32, simpleUuid.length());
        assertNotEquals(uuid, simpleUuid);
    }

    @Test
    void enumUtilitiesFindByNameAndCode() {
        assertEquals(Status.ENABLED, Enums.getByName(Status.class, "ENABLED"));
        assertEquals(Status.DISABLED, Enums.getByCode(Status.class, "0"));
        assertTrue(Enums.containsName(Status.class, "ENABLED"));
        assertFalse(Enums.containsName(Status.class, "UNKNOWN"));
    }

    @Test
    void jsonUtilitiesSerializeAndDeserialize() {
        String json = Jsons.toJson(new JsonUser("egon", 18));

        JsonUser user = Jsons.fromJson(json, JsonUser.class);
        List<JsonUser> users = Jsons.fromJsonList("[{\"name\":\"egon\",\"age\":18}]", JsonUser.class);

        assertEquals("egon", user.getName());
        assertEquals(18, user.getAge());
        assertEquals("egon", users.get(0).getName());
    }

    @Test
    void maskingUtilitiesMaskSensitiveValues() {
        assertEquals("138****8000", Masking.mobile("13812348000"));
        assertEquals("e***@example.com", Masking.email("egon@example.com"));
        assertEquals("110101********1234", Masking.idCard("110101199001011234"));
        assertEquals("6222**********1234", Masking.bankCard("622202020202021234"));
        assertEquals("张*", Masking.name("张三"));
    }

    @Test
    void cryptoUtilitiesDigestEncodeAndHmac() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", Crypto.md5Hex("abc"));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Crypto.sha256Hex("abc"));
        assertEquals("YWJj", Crypto.base64Encode("abc"));
        assertEquals("abc", Crypto.base64DecodeToString("YWJj"));
        assertEquals("9c196e32dc0175f86f4b1cb89289d6619de6bee699e4c378e68309ed97a1a6ab", Crypto.hmacSha256Hex("abc", "key"));
    }

    @Test
    void treeUtilitiesBuildTreeFromFlatNodes() {
        List<UserNode> roots = Trees.build(
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
