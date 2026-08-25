#!/usr/bin/env python3
"""Protect the user's verbatim Java rules and Plan rule matrix."""

from __future__ import annotations

import re
import unittest
from pathlib import Path

VERBATIM_RULES = r'''1 类名规范，必须以 java的pojo规范命名。以dao po bo vo dto query command event等结尾
2 每层之间必须被 springboot-validation 校验，复用的对象 validation 要分组校验，ValidatorUtils使用 libphonenumber进行规范化校验或者validation原生注解，若非必要，不要自己写。
3 实体类规范：复杂对象使用java类并使用Lombok进行@Data\@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor\@RequiredArgsConstructor\@Builder\@Accessors(chain = true)修饰。简单对象使用Java Record。使用MapStruct、MapStructPlus 进行转换，egon-cola-component-common-core有通用的convertor，必须继承实现这个。如果是不可变对象，使用@Value注释修饰。record场景Record 构造器很适合做数据规范化，推荐使用紧凑构造器，Record 可以作为局部类，在方法内部定义临时数据结构。
4业务类必须使用@Slf4j注解注入log对象。如果业务类被spring管理，必须指定名称，如果是单例的情况下，参考@Service("userService")。如果需要依赖注入，必须@RequiredArgsConstructor进行修饰，不要代码中写。且属性必须被@qualify修饰。
5 工具类只允许使用jdk原生、Apache Commons(commons-lang3、commons-collections4、commons-io、commons-text、commons-codec、commons-beanutils)、Guava。针对Tika按需引入。
6 json 使用SpringBoot-JackSon 对外交互层的实体类必须按需被jackson注解修饰。
7 springboot 多环境配置文件，必须保持配置一致，但值不一定一致。
9 复杂业务必须引入设计模式，不允许硬编码
10 日期相关的必须使用java.time下的实体类，不允许使用java.util下的
11 plan中必须确认代码结构，分层结构或者egon-cola-archetype，只允许这两种代码结构规范。&#x20;'''


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
