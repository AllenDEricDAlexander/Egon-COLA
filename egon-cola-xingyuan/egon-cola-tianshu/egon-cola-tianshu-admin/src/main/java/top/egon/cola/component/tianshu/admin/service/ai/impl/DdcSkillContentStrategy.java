package top.egon.cola.component.tianshu.admin.service.ai.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiValidationGroups;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service("ddcSkillContentStrategy")
@RequiredArgsConstructor
@Validated
public class DdcSkillContentStrategy implements DdcAiContentStrategy {

    private static final byte[] DOMAIN = "tianshu-ai-content-v1".getBytes(StandardCharsets.UTF_8);

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    public DdcAiKindEnum kind() {
        return DdcAiKindEnum.SKILL;
    }

    @Override
    public PreparedContent prepare(DdcAiContentDTO content) {
        validationUtils.validate(content, DdcAiValidationGroups.Save.class);
        if (content.getPromptText() != null) throw new IllegalArgumentException("AI_SKILL_PROMPT_TEXT_MUST_BE_NULL");
        List<DdcAiNodeDTO> nodes = new ArrayList<>(Objects.requireNonNull(content.getNodes(), "AI_SKILL_NODES_REQUIRED"));
        nodes.sort((left, right) -> Arrays.compareUnsigned(utf8(left.getPath()), utf8(right.getPath())));

        Map<String, String> fileHashes = new HashMap<>();
        long totalBytes = 0;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.write(DOMAIN);
                output.writeByte(0);
                output.writeByte(2);
                output.writeInt(nodes.size());
                for (DdcAiNodeDTO node : nodes) {
                    byte[] path = utf8(node.getPath());
                    output.writeLong(path.length);
                    output.write(path);
                    if (node.getNodeType() == DdcAiNodeTypeEnum.DIRECTORY) {
                        output.writeByte(0);
                    } else {
                        output.writeByte(1);
                        byte[] contentBytes = utf8(node.getContent());
                        output.writeLong(contentBytes.length);
                        output.write(contentBytes);
                        totalBytes = Math.addExact(totalBytes, contentBytes.length);
                        fileHashes.put(node.getPath(), DdcAiContentStrategy.sha256Hex(contentBytes));
                    }
                }
            }
            return new PreparedContent(null, nodes, DdcAiContentStrategy.sha256Hex(bytes.toByteArray()),
                    fileHashes, totalBytes, nodes.size());
        } catch (IOException failure) {
            throw new IllegalStateException("Unable to encode Skill content hash", failure);
        }
    }

    private byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
