#!/usr/bin/env python3
"""Protect the user's verbatim Java rules and Plan rule matrix."""

from __future__ import annotations

import re
import unittest
from pathlib import Path

VERBATIM_RULES = r'''1 类名规范，必须以 java的pojo规范命名。以dao po bo vo dto query command event等结尾
2 每层之间必须被 springboot-validation 校验，复用对象使用分组校验；基于原生和自定义约束注解、@Valid、@Validated 及 ConstraintValidator 扩展实现。ValidationUtils 只承担通用手工校验，不承载全部业务校验；电话号码复用 libphonenumber。
3 Java POJO 默认使用 class，注解为 @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true)，按构造目标选择 @Builder 或 @SuperBuilder；父类状态参与相等性时使用 @EqualsAndHashCode(callSuper = true)。只有不可变 value object 可以使用 record，并豁免 class 的 Lombok 规范；普通实体不能因简单而使用 record。使用 MapStruct、MapStructPlus 和 egon-cola-component-common-core 的 BaseConverter（双向）或 BaseForwardConverter（不可逆投影）进行转换。
4业务类必须使用@Slf4j注解注入log对象。如果业务类被spring管理，必须指定名称，如果是单例的情况下，参考@Service("userService")。如果需要依赖注入，必须@RequiredArgsConstructor进行修饰，不要代码中写。且属性必须被@qualify修饰。
5 工具类只允许使用jdk原生、Apache Commons(commons-lang3、commons-collections4、commons-io、commons-text、commons-codec、commons-beanutils)、Guava。针对Tika按需引入。
6 JSON 使用 Spring Boot Jackson；持久化枚举值使用 @EnumValue，向前端输出的枚举值使用 @JsonValue，禁止 ordinal 作为业务编码。
7 springboot 多环境配置文件，必须保持配置一致，但值不一定一致。
9 复杂业务必须引入设计模式，不允许硬编码
10 日期相关的必须使用java.time下的实体类，不允许使用java.util下的
11 只允许现有三层结构或当前 egon-cola-archetypes 的 DDD 结构，三层结构保持现状。必须尽量复用 egon-cola-components；持久化模块必须使用 Egon COLA MP Starter，DDL 统一由 egon-mp-sdj-ext-starter 分布式管理。业务唯一键必须组合业务列与 deletedAt（数据库 deleted_at），并验证 NULL、租户及重复软删除语义。采用 CQE（Command Query Event）；Event 必须经 egon-cola-component-transactional-outbox-starter 或 MQ 中间件投递。'''


class UserMandatedJavaRulesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.root = Path(__file__).resolve().parent.parent

    def test_verbatim_rules_are_present_once_in_entrypoints_and_references(self) -> None:
        for relative in (
            "SKILL.md",
            "SKILL.zh-CN.md",
            "references/user-mandated-java-rules.md",
            "references/user-mandated-java-rules.zh-CN.md",
        ):
            text = (self.root / relative).read_text(encoding="utf-8")
            self.assertEqual(1, text.count(VERBATIM_RULES), relative)

    def test_original_numbering_is_preserved(self) -> None:
        lines = VERBATIM_RULES.splitlines()
        actual = [line.split(maxsplit=1)[0] if not line.startswith("4业务") else "4" for line in lines]
        self.assertEqual(["1", "2", "3", "4", "5", "6", "7", "9", "10", "11"], actual)

    def test_current_template_has_exact_rule_matrix_and_markers(self) -> None:
        text = (self.root / "assets/plan-template.md").read_text(encoding="utf-8")
        self.assertIn("| Template Version | `4` |", text)
        start = text.index("### 4.8 User-mandated Java Rule Implementation Matrix")
        end = text.index("\n## 5. Change File Tree", start)
        rows = re.findall(r"(?m)^\| Rule (\d+) \|", text[start:end])
        self.assertEqual(["1", "2", "3", "4", "5", "6", "7", "9", "10", "11"], rows)
        self.assertIn("- Literal Rules:", text)
        self.assertIn("- Literal rule enforcement:", text)

    def test_known_weakening_language_is_absent(self) -> None:
        text = (self.root / "references/java-spring-egon-coding-standards.md").read_text(encoding="utf-8")
        for forbidden in (
            "selecting only necessary Lombok annotations",
            "When semantically compatible and available",
            "For direct logic, record why a pattern",
        ):
            self.assertNotIn(forbidden, text)


if __name__ == "__main__":
    unittest.main()
