package top.egon.cola.component.tianshu.admin.service.ai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationContextBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelRequestDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill 改写策略：输出完整文件树，解析时补齐根与中间目录节点。
 * / Skill rewrite strategy: outputs the whole file tree; parsing adds the root and intermediate directories.
 */
@Slf4j
@Component("ddcSkillOptimizerStrategy")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DdcSkillOptimizerStrategy implements DdcAiOptimizerStrategy {

    static final String ROOT_FILE = "SKILL.md";
    static final int MAX_FILES = 128;
    static final int MAX_FILE_BYTES = 128 * 1024;

    @Qualifier("ddcAiPromptTemplateLoader")
    private final DdcAiPromptTemplateLoader templates;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Override
    public DdcAiKindEnum kind() {
        return DdcAiKindEnum.SKILL;
    }

    @Override
    public DdcAiModelRequestDTO compose(DdcAiOptimizationContextBO context, String modelAlias, int maxOutputTokens,
            double temperature) {
        ArrayNode files = objectMapper.createArrayNode();
        context.getBaseline().getNodes().stream()
                .filter(node -> node.getNodeType() == DdcAiNodeTypeEnum.FILE)
                .sorted(Comparator.comparing(DdcAiNodeDTO::getPath))
                .forEach(node -> files.add(objectMapper.createObjectNode()
                        .put("path", node.getPath())
                        .put("content", node.getContent() == null ? "" : node.getContent())));
        String tree;
        try {
            tree = objectMapper.writeValueAsString(objectMapper.createObjectNode().set("files", files));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("AI_SKILL_TREE_NOT_SERIALIZABLE", failure);
        }
        String user = "## Current Skill Files\n" + tree
                + "\n\n## Operator Instruction\n" + (context.getInstruction().isBlank() ? "(none)" : context.getInstruction())
                + "\n\n## Low-score Samples\n"
                + DdcAiOptimizerSamples.render(context.getSamples(), context.getMaxContextBytes() / 2);
        return new DdcAiModelRequestDTO(modelAlias, templates.optimizeSkillSystem(), user, maxOutputTokens, temperature,
                "OPTIMIZE");
    }

    @Override
    public DdcAiContentDTO parse(String modelText) {
        JsonNode files;
        try {
            JsonNode root = objectMapper.readTree(DdcAiText.stripCodeFence(modelText));
            files = root == null ? null : root.get("files");
        } catch (JsonProcessingException malformed) {
            throw invalid();
        }
        if (files == null || !files.isArray() || files.isEmpty() || files.size() > MAX_FILES) throw invalid();
        Map<String, DdcAiNodeDTO> nodes = new LinkedHashMap<>();
        nodes.put("", DdcAiNodeDTO.builder().path("").nodeType(DdcAiNodeTypeEnum.DIRECTORY).build());
        List<DdcAiNodeDTO> fileNodes = new ArrayList<>();
        for (JsonNode file : files) {
            JsonNode path = file.get("path");
            JsonNode content = file.get("content");
            if (path == null || !path.isTextual() || content == null || !content.isTextual()) throw invalid();
            String relative = path.asText();
            if (!safePath(relative) || content.asText().getBytes(StandardCharsets.UTF_8).length > MAX_FILE_BYTES) {
                throw invalid();
            }
            fileNodes.add(DdcAiNodeDTO.builder().path(relative).nodeType(DdcAiNodeTypeEnum.FILE)
                    .content(content.asText()).build());
        }
        for (DdcAiNodeDTO file : fileNodes) {
            String path = file.getPath();
            for (int slash = path.indexOf('/'); slash > 0; slash = path.indexOf('/', slash + 1)) {
                String directory = path.substring(0, slash);
                DdcAiNodeDTO existing = nodes.putIfAbsent(directory,
                        DdcAiNodeDTO.builder().path(directory).nodeType(DdcAiNodeTypeEnum.DIRECTORY).build());
                if (existing != null && existing.getNodeType() != DdcAiNodeTypeEnum.DIRECTORY) throw invalid();
            }
            if (nodes.putIfAbsent(path, file) != null) throw invalid();
        }
        if (!nodes.containsKey(ROOT_FILE) || nodes.get(ROOT_FILE).getNodeType() != DdcAiNodeTypeEnum.FILE) {
            throw invalid();
        }
        return DdcAiContentDTO.builder().promptText(null).nodes(List.copyOf(nodes.values())).build();
    }

    private static boolean safePath(String path) {
        if (path.isBlank() || path.startsWith("/") || path.endsWith("/") || path.contains("\\")
                || path.length() > 1024) {
            return false;
        }
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) return false;
        }
        return path.chars().noneMatch(Character::isISOControl);
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("INVALID_OUTPUT");
    }
}
