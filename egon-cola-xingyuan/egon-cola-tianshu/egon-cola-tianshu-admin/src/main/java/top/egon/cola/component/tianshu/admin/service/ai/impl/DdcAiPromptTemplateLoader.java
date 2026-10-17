package top.egon.cola.component.tianshu.admin.service.ai.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从 classpath 加载并缓存评分与改写的系统提示模板。
 * / Loads and caches the judge and rewrite system prompt templates from the classpath.
 */
@Slf4j
@Component("ddcAiPromptTemplateLoader")
public class DdcAiPromptTemplateLoader {

    static final String BASE = "tianshu/ai/prompts/";
    static final String JUDGE = "judge-system.txt";
    static final String OPTIMIZE_PROMPT = "optimize-prompt-system.txt";
    static final String OPTIMIZE_SKILL = "optimize-skill-system.txt";

    private final Map<String, String> templates = new ConcurrentHashMap<>();

    public String judgeSystem() {
        return template(JUDGE);
    }

    public String optimizePromptSystem() {
        return template(OPTIMIZE_PROMPT);
    }

    public String optimizeSkillSystem() {
        return template(OPTIMIZE_SKILL);
    }

    String template(String name) {
        return templates.computeIfAbsent(name, DdcAiPromptTemplateLoader::read);
    }

    private static String read(String name) {
        try (InputStream input = new ClassPathResource(BASE + name).getInputStream()) {
            String text = StreamUtils.copyToString(input, StandardCharsets.UTF_8).strip();
            if (text.isEmpty()) throw new IllegalStateException("AI_PROMPT_TEMPLATE_EMPTY:" + name);
            return text;
        } catch (IOException missing) {
            throw new IllegalStateException("AI_PROMPT_TEMPLATE_MISSING:" + name, missing);
        }
    }
}
