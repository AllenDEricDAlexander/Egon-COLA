package top.egon.cola.component.tianshu.admin.service.ai.impl;

import java.nio.charset.StandardCharsets;

/**
 * AI 提示组装共用的文本工具（仅 JDK）。
 * / Text helpers shared by AI prompt composition (JDK only).
 */
final class DdcAiText {

    private DdcAiText() {
    }

    /** 按 UTF-8 字节上限截断且不切断代码点。 / Truncates to a UTF-8 byte limit without splitting a code point. */
    static String truncateUtf8(String value, int maxBytes) {
        if (value == null) return null;
        if (value.getBytes(StandardCharsets.UTF_8).length <= maxBytes) return value;
        int bytes = 0;
        int index = 0;
        while (index < value.length()) {
            int codePoint = value.codePointAt(index);
            int width = codePoint < 0x80 ? 1 : codePoint < 0x800 ? 2 : codePoint < 0x10000 ? 3 : 4;
            if (bytes + width > maxBytes) break;
            bytes += width;
            index += Character.charCount(codePoint);
        }
        return value.substring(0, index);
    }

    /** 去掉模型可能包裹的 Markdown 代码块标记。 / Removes a Markdown code fence the model may have added. */
    static String stripCodeFence(String text) {
        if (text == null) return "";
        String trimmed = text.strip();
        if (!trimmed.startsWith("```")) return trimmed;
        int firstLineEnd = trimmed.indexOf('\n');
        int closing = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || closing <= firstLineEnd) return trimmed;
        return trimmed.substring(firstLineEnd + 1, closing).strip();
    }
}
