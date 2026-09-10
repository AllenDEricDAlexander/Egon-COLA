package top.egon.cola.component.rag.chunk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;
import top.egon.cola.component.rag.model.RagChunkBO;
import top.egon.cola.component.rag.model.RagChunkingConfigDTO;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Two-level splitting: first by heading level, then token-wise for oversized sections.
 *
 * <p>Each section keeps its heading line so a chunk stays readable on its own, and records the
 * heading path in {@code headingPath} assembled from the enclosing headings. Text without headings
 * degrades to a single section, which the token splitter may still divide.
 */
@Slf4j
public class MarkdownHeadingRagChunkingStrategy implements RagChunkingStrategy {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*)$");

    @Override
    public RagChunkingStrategyEnum strategy() {
        return RagChunkingStrategyEnum.MARKDOWN_HEADING;
    }

    @Override
    public String name() {
        return "markdownHeadingRagChunkingStrategy";
    }

    @Override
    public List<RagChunkBO> split(ExtractedDocumentBO document, RagChunkingConfigDTO config) {
        if (document.text().isBlank()) {
            return List.of();
        }
        try {
            List<RagChunkBO> chunks = new ArrayList<>();
            for (Section section : sectionsOf(document.text(), config.headingLevels())) {
                List<String> pieces = applyOverlap(splitOversized(section.content(), config),
                        config.overlapTokens());
                for (String piece : pieces) {
                    if (piece.isBlank()) {
                        continue;
                    }
                    Map<String, String> attributes = section.headingPath().isEmpty()
                            ? Map.of()
                            : Map.of("headingPath", section.headingPath());
                    chunks.add(new RagChunkBO(chunks.size(), piece, attributes));
                }
            }
            return List.copyOf(chunks);
        } catch (RuntimeException exception) {
            throw failure("markdown-heading", exception);
        }
    }

    private List<String> splitOversized(String content, RagChunkingConfigDTO config) {
        if (content.length() <= charBudget(config.maxTokensPerChunk())) {
            return List.of(content);
        }
        return TokenTextSplitter.builder()
                .withChunkSize(config.maxTokensPerChunk())
                .withMinChunkSizeChars(config.minChunkChars())
                .withMinChunkLengthToEmbed(1)
                .withMaxNumChunks(10_000)
                .withKeepSeparator(true)
                .build()
                .apply(List.of(new Document(content))).stream()
                .map(Document::getText)
                .filter(piece -> !piece.isBlank())
                .toList();
    }

    private static List<Section> sectionsOf(String text, List<Integer> headingLevels) {
        List<Section> sections = new ArrayList<>();
        Deque<Heading> stack = new ArrayDeque<>();
        StringBuilder buffer = new StringBuilder();
        String currentPath = "";
        for (String line : text.split("\n", -1)) {
            Matcher matcher = HEADING.matcher(line);
            if (matcher.matches() && headingLevels.contains(matcher.group(1).length())) {
                addSection(sections, currentPath, buffer);
                int level = matcher.group(1).length();
                while (!stack.isEmpty() && stack.peek().level() >= level) {
                    stack.pop();
                }
                stack.push(new Heading(level, matcher.group(2).trim()));
                currentPath = pathOf(stack);
            }
            buffer.append(line).append('\n');
        }
        addSection(sections, currentPath, buffer);
        return sections;
    }

    private static void addSection(List<Section> sections, String headingPath, StringBuilder buffer) {
        String content = buffer.toString();
        buffer.setLength(0);
        if (!content.isBlank()) {
            sections.add(new Section(headingPath, content));
        }
    }

    private static String pathOf(Deque<Heading> stack) {
        List<String> titles = new ArrayList<>();
        for (Heading heading : stack) {
            titles.add(heading.title());
        }
        java.util.Collections.reverse(titles);
        return String.join(" > ", titles);
    }

    private record Heading(int level, String title) {
    }

    private record Section(String headingPath, String content) {
    }
}
