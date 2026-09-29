package top.egon.cola.component.tianshu.admin.service.ai.impl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiValidationGroups;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Validated
public interface DdcAiContentStrategy {

    DdcAiKindEnum kind();

    @Validated(DdcAiValidationGroups.Save.class)
    PreparedContent prepare(
            @NotNull(groups = DdcAiValidationGroups.Save.class)
            @Valid DdcAiContentDTO content);

    static String sha256Hex(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is not available", failure);
        }
    }

    record PreparedContent(String promptText, List<DdcAiNodeDTO> nodes, String contentHash,
            Map<String, String> fileSha256ByPath, long contentBytes, int nodeCount) {

        public PreparedContent {
            nodes = copyNodes(nodes);
            fileSha256ByPath = Map.copyOf(fileSha256ByPath);
        }

        @Override
        public List<DdcAiNodeDTO> nodes() {
            return copyNodes(nodes);
        }

        private static List<DdcAiNodeDTO> copyNodes(List<DdcAiNodeDTO> source) {
            if (source == null || source.isEmpty()) return List.of();
            return source.stream().map(node -> DdcAiNodeDTO.builder().path(node.getPath())
                    .nodeType(node.getNodeType()).content(node.getContent()).build()).toList();
        }
    }
}
