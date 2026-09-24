package top.egon.cola.component.yuheng.llm.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Signal;
import reactor.core.publisher.SignalType;
import top.egon.cola.component.yuheng.llm.config.LlmGatewayProperties;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.dto.LlmInvocationCommandDTO;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmCapabilityEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.exception.LlmInvocationException;
import top.egon.cola.component.yuheng.llm.proxy.service.AnthropicMessagesProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.OpenAiChatProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.OpenAiEmbeddingProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.OpenAiResponsesProtocolStrategy;

/**
 * 中文说明：{@code LlmProtocolContractTest} 是 Step 11「四协议保真 + 有界流式屏障」的测试先行闸门：它只驱动
 * {@link LlmProtocolStrategy} 的四个公开编解码操作（{@code encodeRequest}、{@code decodeUnaryResponse}、
 * {@code encodeStream}、{@code encodeError}），所以固定的是 SPI 自己写明的合同，而不是任何实现的临时行为：
 * 原生 JSON 与 SSE 帧只允许改 {@code model}（含逐帧回写 alias），工具调用 ID、{@code arguments} 片段、
 * {@code call_id}/{@code tool_use_id} 配对、{@code thinking} 签名与未知但安全的字段必须逐字保真；数值表示不得被
 * 改写（{@code 0.30} 不能变成 {@code 0.3}）；未声明能力的请求扩展与任何自证出网/凭据字段一律 400
 * {@code unsupported_parameter}；向量条数/index/宽度不合与 EOF 缺终态都是 502 {@code upstream_protocol_error} 或
 * ABORTED，绝不返回空成功、绝不伪造 {@code [DONE]}/{@code message_stop}/{@code response.completed}；OpenAI 三面的
 * 错误对象只有顶层 {@code error} 且内层四键，Messages 的顶层 {@code type} 恒为 {@code error} 且不套 OpenAI 形状；
 * 下游取消必须传播为上游取消，且上游交出的每个 {@link DataBuffer} 恰好释放一次。本类不启动 Spring 容器、不碰
 * 数据库、不发起任何真实网络调用：帧字节直接喂给 {@link Flux}，上游缓冲由计数代理 {@link PooledDataBuffer} 供给。
 * English summary: {@code LlmProtocolContractTest} is the test-first gate of Step 11 ("four-protocol fidelity plus the
 * bounded streaming barrier"): it drives only the four public codec operations of {@link LlmProtocolStrategy}, so what it
 * pins is the contract the SPI itself states rather than whatever an implementation happens to do. Only {@code model} may
 * change (per frame on the way back), while tool-call ids, {@code arguments} fragments, {@code call_id}/
 * {@code tool_use_id} pairing, {@code thinking} signatures and unknown-but-safe fields travel verbatim; a numeric
 * representation may not be re-cast ({@code 0.30} never becomes {@code 0.3}); an undeclared capability extension or any
 * self-asserted egress/credential field is a 400 {@code unsupported_parameter}; an embedding count/index/width mismatch
 * and an EOF without this protocol's terminal marker collapse into a 502 {@code upstream_protocol_error} or an ABORTED
 * stream, never an empty success and never a fabricated {@code [DONE]}/{@code message_stop}/
 * {@code response.completed}; the three OpenAI faces expose a single top-level {@code error} key with exactly four inner
 * keys while Messages keeps a top-level {@code type} of {@code error} and never borrows the OpenAI shape; a downstream
 * cancellation has to reach upstream and every {@link DataBuffer} the upstream issued must be released exactly once. No
 * Spring context, no database and no live network call: frame bytes go straight into a {@link Flux} and the upstream
 * buffers come from a counting {@link PooledDataBuffer} proxy.
 *
 * 用法 / Usage: {@code ./mvnw -o -pl …/yuheng-llm-gateway -am -Dtest=LlmProtocolContractTest …} 聚焦执行。
 */
class LlmProtocolContractTest {

    private static final String SUBJECT = "svc:wiki-indexer";
    private static final String CHANNEL_KEY = "local-a";
    private static final String CHAT_ALIAS = "company-chat";
    private static final String EMBEDDING_ALIAS = "company-embed-v1";
    private static final String RESPONSES_ALIAS = "company-responses";
    private static final String MESSAGES_ALIAS = "company-messages";
    private static final String UPSTREAM_MODEL = "vendor-real-model-x";
    private static final String BASE_URL = "https://local-llm.internal:8000/v1";
    private static final String SECRET_REF = "env:LLM_LOCAL_A_SECRET";
    private static final String CREDENTIAL_TEXT = "sk-enterprise-upstream-credential-4f4a";
    private static final String PROMPT_TEXT = "请解释发布流程。";
    private static final String UNSUPPORTED_PARAMETER = "unsupported_parameter";
    private static final String UPSTREAM_PROTOCOL_ERROR = "upstream_protocol_error";
    private static final String DONE_MARKER = "[DONE]";

    /** 中文说明：容器唯一 Jackson 栈的等价物：测试用它造文档，也用它序列化后再断言保真。 English summary: the test's equivalent of the container's single Jackson stack, used both to build documents and to serialise them for fidelity assertions. */
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** 中文说明：任何错误文档都不得出现的敏感片段，逐项对应 SPI 的禁止回显清单。 English summary: the sensitive fragments no error document may ever carry, one entry per SPI prohibition. */
    private static final List<String> FORBIDDEN_FRAGMENTS = List.of(BASE_URL, "local-llm.internal", SECRET_REF,
            "LLM_LOCAL_A_SECRET", CREDENTIAL_TEXT, "sk-", "Bearer", UPSTREAM_MODEL, PROMPT_TEXT, "Authorization");

    @Test
    @DisplayName("一个适配器一个协议：protocol() 让注册表能建无重复无缺项的 EnumMap")
    void eachAdapterOwnsExactlyOneProtocolAndNeverFallsBackToChat() {
        List<LlmProtocolEnum> declared = List.of(chatFace().protocol(), embeddingsFace().protocol(),
                responsesFace().protocol(), messagesFace().protocol());

        assertThat(declared).containsExactlyInAnyOrder(LlmProtocolEnum.OPENAI_CHAT,
                LlmProtocolEnum.OPENAI_EMBEDDING, LlmProtocolEnum.OPENAI_RESPONSES,
                LlmProtocolEnum.ANTHROPIC_MESSAGES);
        assertThat(declared).doesNotHaveDuplicates();
    }

    @Nested
    @DisplayName("OPENAI_CHAT：choices/delta.tool_calls/[DONE] 保真")
    class ChatFace {

        @Test
        @DisplayName("encodeRequest 只把 model 改成 upstreamModel，tools/tool_choice 与数值表示原样保留")
        void encodeRequestRewritesOnlyTheModelAndKeepsToolAndNumericRepresentation() {
            ObjectNode payload = document("""
                    {
                      "model": "company-chat",
                      "messages": [
                        {"role": "system", "content": "仅根据提供的资料回答。"},
                        {"role": "user", "content": "查一下上海明天的天气。"}
                      ],
                      "tools": [
                        {"type": "function", "function": {"name": "lookup_weather", "description": "按城市查天气",
                          "parameters": {"type": "object",
                            "properties": {"city": {"type": "string"}, "days": {"type": "integer"}},
                            "required": ["city", "days"]},
                          "strict": true}}
                      ],
                      "tool_choice": {"type": "function", "function": {"name": "lookup_weather"}},
                      "parallel_tool_calls": true,
                      "max_tokens": 1024,
                      "presence_penalty": -0.5
                    }
                    """);
            // 数值表示与「已声明能力的扩展字段」只能按节点构造，才谈得上要求编解码两侧都不改写它。
            payload.put("temperature", new BigDecimal("0.30"));
            payload.putObject("response_format").put("type", "json_object");
            payload.put("reasoning_effort", "high");
            // 注意：这里必须追加到字面量里已有的 messages 数组，`putArray` 会先把该名字下的数组清空重建。
            ((com.fasterxml.jackson.databind.node.ArrayNode) payload.path("messages")).addObject()
                    .put("role", "assistant").putNull("content")
                    .putArray("tool_calls").addObject()
                    .put("id", "call_7f3a").put("type", "function").put("index", 0)
                    .putObject("function").put("name", "lookup_weather")
                    .put("arguments", "{\"city\":\"上海\",\"days\":3}");
            LlmInvocationCommandDTO command = command(LlmProtocolEnum.OPENAI_CHAT, CHAT_ALIAS, false, payload,
                    LlmCapabilityEnum.FUNCTION_TOOLS, LlmCapabilityEnum.STRUCTURED_OUTPUT,
                    LlmCapabilityEnum.REASONING);

            ObjectNode encoded = chatFace().encodeRequest(command,
                    route(LlmProtocolEnum.OPENAI_CHAT, LlmCapabilityEnum.FUNCTION_TOOLS,
                            LlmCapabilityEnum.STRUCTURED_OUTPUT, LlmCapabilityEnum.REASONING));

            ObjectNode untouched = payload.deepCopy();
            untouched.put("model", UPSTREAM_MODEL);
            assertThat((Object) encoded).isEqualTo(untouched);
            assertThat(encoded.path("model").asText()).isEqualTo(UPSTREAM_MODEL);
            assertThat(write(encoded)).contains("\"temperature\":0.30");
            BigDecimal temperature = encoded.path("temperature").decimalValue();
            assertThat(temperature).as("the temperature node must still be a decimal").isNotNull();
            assertThat(temperature).isEqualByComparingTo("0.30");
            assertThat(temperature.scale()).as("0.30 must never be re-cast as 0.3").isEqualTo(2);
            assertThat(textValues(encoded.path("messages"), "role")).containsExactly("system", "user", "assistant");
            assertThat(encoded.path("tool_choice").path("function").path("name").asText())
                    .isEqualTo("lookup_weather");
            assertThat(valuesOf(encoded.path("tools").get(0).path("function").path("parameters")
                    .path("required"))).containsExactly("city", "days");
            JsonNode call = encoded.path("messages").get(2).path("tool_calls").get(0);
            assertThat(call.path("id").asText()).isEqualTo("call_7f3a");
            assertThat(call.path("function").path("arguments").asText())
                    .isEqualTo("{\"city\":\"上海\",\"days\":3}");
        }

        @Test
        @DisplayName("自证出网/凭据字段（upstream_url、api_key、host…）一律 400 unsupported_parameter 且不回显其值")
        void encodeRequestRejectsEverySelfAssertedEgressOrCredentialFieldWithNativeFourHundred() {
            LlmModelSnapshotBO.RouteBO route = route(LlmProtocolEnum.OPENAI_CHAT, LlmCapabilityEnum.FUNCTION_TOOLS);
            for (String field : List.of("upstream_url", "upstreamUrl", "base_url", "api_key", "host", "credential",
                    "secretRef", "authorization")) {
                ObjectNode payload = minimalChatPayload();
                payload.put(field, BASE_URL + "?key=" + CREDENTIAL_TEXT);

                LlmInvocationException error = catchThrowableOfType(
                        () -> chatFace().encodeRequest(
                                command(LlmProtocolEnum.OPENAI_CHAT, CHAT_ALIAS, false, payload,
                                        LlmCapabilityEnum.FUNCTION_TOOLS), route),
                        LlmInvocationException.class);

                assertThat(error.getStatus()).as("native status for %s", field).isEqualTo(400);
                assertThat(error.getCode()).as("machine code for %s", field).isEqualTo(UNSUPPORTED_PARAMETER);
                assertThat(error.getMessage()).as("no echo of the submitted value of %s", field)
                        .doesNotContain(BASE_URL, "local-llm.internal", CREDENTIAL_TEXT, "sk-");
            }
        }

        @Test
        @DisplayName("未声明能力的请求扩展（如 response_format）只能 400 拒绝，不得静默透传或降级")
        void encodeRequestRefusesAnExtensionWhoseCapabilityWasNeverDeclared() {
            ObjectNode payload = minimalChatPayload();
            payload.putObject("response_format").put("type", "json_schema");

            assertThatExceptionOfType(LlmInvocationException.class)
                    .isThrownBy(() -> chatFace().encodeRequest(
                            command(LlmProtocolEnum.OPENAI_CHAT, CHAT_ALIAS, false, payload),
                            route(LlmProtocolEnum.OPENAI_CHAT)))
                    .satisfies(error -> {
                        assertThat(error.getStatus()).isEqualTo(400);
                        assertThat(error.getCode()).isEqualTo(UNSUPPORTED_PARAMETER);
                    });
        }

        @Test
        @DisplayName("decodeUnaryResponse 保真 choices[].message.tool_calls 的 id 与 arguments，只把 model 换回 alias")
        void decodeUnaryResponseKeepsToolCallIdentityArgumentsAndUnknownSafeFields() {
            ObjectNode upstream = document("""
                    {
                      "id": "chatcmpl-78001",
                      "object": "chat.completion",
                      "created": 1790000000,
                      "model": "vendor-real-model-x",
                      "choices": [
                        {"index": 0,
                         "message": {"role": "assistant", "content": null, "refusal": null,
                           "tool_calls": [
                             {"id": "call_7f3a", "type": "function", "index": 0,
                              "function": {"name": "lookup_weather", "arguments": "{\\"city\\":\\"上海\\"}"}},
                             {"id": "call_8c1b", "type": "function", "index": 1,
                              "function": {"name": "book_ticket", "arguments": "{}"}}
                           ]},
                         "logprobs": null,
                         "finish_reason": "tool_calls"}
                      ],
                      "usage": {"prompt_tokens": 21, "completion_tokens": 9, "total_tokens": 30,
                        "prompt_tokens_details": {"cached_tokens": 8}},
                      "system_fingerprint": "fp_vendor_42",
                      "vendor_route_debug": {"replica": "shard-7", "queue_ms": 3}
                    }
                    """);

            ObjectNode decoded = chatFace().decodeUnaryResponse(
                    command(LlmProtocolEnum.OPENAI_CHAT, CHAT_ALIAS, false, minimalChatPayload(),
                            LlmCapabilityEnum.FUNCTION_TOOLS), upstream);

            JsonNode calls = decoded.path("choices").get(0).path("message").path("tool_calls");
            assertThat(textValues(calls, "id")).containsExactly("call_7f3a", "call_8c1b");
            assertThat(calls.get(0).path("function").path("arguments").asText()).isEqualTo("{\"city\":\"上海\"}");
            assertThat(calls.get(1).path("function").path("arguments").asText()).isEqualTo("{}");
            assertThat(calls.get(0).path("function").path("name").asText()).isEqualTo("lookup_weather");
            assertThat(decoded.path("choices").get(0).path("finish_reason").asText()).isEqualTo("tool_calls");
            assertThat(decoded.path("id").asText()).isEqualTo("chatcmpl-78001");
            assertThat(decoded.path("created").asLong()).isEqualTo(1_790_000_000L);
            assertThat(decoded.path("system_fingerprint").asText()).isEqualTo("fp_vendor_42");
            assertThat(decoded.path("vendor_route_debug").path("replica").asText()).isEqualTo("shard-7");
            assertThat(decoded.path("usage").path("prompt_tokens_details").path("cached_tokens").asInt()).isEqualTo(8);
            assertThat(decoded.path("choices").get(0).path("message").path("refusal").isNull()).isTrue();
            assertThat(decoded.path("model").asText()).isEqualTo(CHAT_ALIAS);
            assertThat(write(decoded)).doesNotContain(UPSTREAM_MODEL);
        }

        @Test
        @DisplayName("decodeUnaryResponse 绝不把缺失的 usage 伪造成 0，也不重排数组")
        void decodeUnaryResponseNeverFabricatesAMissingUsage() {
            ObjectNode upstream = document("""
                    {
                      "id": "chatcmpl-78002",
                      "object": "chat.completion",
                      "created": 1790000001,
                      "model": "vendor-real-model-x",
                      "choices": [
                        {"index": 0, "message": {"role": "assistant", "content": "第一段"}, "finish_reason": "stop"},
                        {"index": 1, "message": {"role": "assistant", "content": "第二段"}, "finish_reason": "stop"}
                      ]
                    }
                    """);

            ObjectNode decoded = chatFace().decodeUnaryResponse(
                    command(LlmProtocolEnum.OPENAI_CHAT, CHAT_ALIAS, false, minimalChatPayload()), upstream);

            assertThat(decoded.has("usage")).isFalse();
            assertThat(decoded.path("choices").size()).isEqualTo(2);
            assertThat(decoded.path("choices").get(0).path("message").path("content").asText()).isEqualTo("第一段");
            assertThat(decoded.path("choices").get(1).path("message").path("content").asText()).isEqualTo("第二段");
            assertThat(decoded.path("model").asText()).isEqualTo(CHAT_ALIAS);
        }

        @Test
        @DisplayName("SSE：delta.tool_calls[].index 顺序与 arguments 片段保真，终态 finish_reason 之后恰好一个 [DONE]")
        void encodeStreamKeepsToolCallDeltaOrderingTerminalReasonAndExactlyOneDoneMarker() {
            LlmInvocationCommandDTO command = command(LlmProtocolEnum.OPENAI_CHAT, CHAT_ALIAS, true,
                    minimalChatPayload(), LlmCapabilityEnum.FUNCTION_TOOLS);
            StreamOutcome outcome = drive(chatFace(), command, new BufferLedger(),
                    chatFrame("""
                            {"id":"chatcmpl-78001","object":"chat.completion.chunk","created":1790000000,
                             "model":"vendor-real-model-x",
                             "choices":[{"index":0,"delta":{"role":"assistant","content":null},"finish_reason":null}]}
                            """),
                    chatFrame("""
                            {"id":"chatcmpl-78001","object":"chat.completion.chunk","created":1790000000,
                             "model":"vendor-real-model-x",
                             "choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"call_7f3a","type":"function",
                               "function":{"name":"lookup_weather","arguments":""}}]},"finish_reason":null}]}
                            """),
                    chatFrame("""
                            {"id":"chatcmpl-78001","object":"chat.completion.chunk","created":1790000000,
                             "model":"vendor-real-model-x",
                             "choices":[{"index":0,"delta":{"tool_calls":[{"index":0,
                               "function":{"arguments":"{\\"city\\":"}}]},"finish_reason":null}]}
                            """),
                    chatFrame("""
                            {"id":"chatcmpl-78001","object":"chat.completion.chunk","created":1790000000,
                             "model":"vendor-real-model-x",
                             "choices":[{"index":0,"delta":{"tool_calls":[{"index":1,"id":"call_8c1b","type":"function",
                               "function":{"name":"book_ticket","arguments":"{}"}}]},"finish_reason":null}]}
                            """),
                    chatFrame("""
                            {"id":"chatcmpl-78001","object":"chat.completion.chunk","created":1790000000,
                             "model":"vendor-real-model-x",
                             "choices":[{"index":0,"delta":{},"finish_reason":"tool_calls"}]}
                            """),
                    "data: " + DONE_MARKER + "\n\n");

            assertThat(outcome.failure()).isNull();
            assertThat(outcome.completed()).isTrue();
            assertThat(toolCallDeltaIndexesOf(outcome.text())).containsExactly(0, 0, 1);
            assertThat(toolCallArgumentsOf(outcome.text())).containsExactly("", "{\"city\":", "{}");
            assertThat(toolCallIdsOf(outcome.text())).containsExactly("call_7f3a", "call_8c1b");
            assertThat(fieldValuesOf(outcome.dataNodes(), "object")).containsOnly("chat.completion.chunk");
            assertThat(fieldValuesOf(outcome.dataNodes(), "id")).containsOnly("chatcmpl-78001");
            assertThat(fieldValuesOf(outcome.dataNodes(), "model")).containsOnly(CHAT_ALIAS);
            assertThat(outcome.dataNodes().get(outcome.dataNodes().size() - 1)
                    .path("choices").get(0).path("finish_reason").asText()).isEqualTo("tool_calls");
            assertThat(outcome.doneMarkers()).isEqualTo(1);
            assertThat(outcome.text().trim()).endsWith(DONE_MARKER);
            assertThat(outcome.text()).doesNotContain(UPSTREAM_MODEL, DONE_MARKER + DONE_MARKER);
        }

        @Test
        @DisplayName("取消第一帧之后：取消传播到上游，且上游交出的每个 DataBuffer 恰好释放一次")
        void cancellingAfterTheFirstFramePropagatesUpstreamAndReleasesEveryIssuedBufferOnce() {
            BufferLedger ledger = new BufferLedger();
            AtomicBoolean upstreamCancelled = new AtomicBoolean();
            AtomicBoolean upstreamCompleted = new AtomicBoolean();
            AtomicInteger delivered = new AtomicInteger();
            LlmInvocationCommandDTO command = command(LlmProtocolEnum.OPENAI_CHAT, CHAT_ALIAS, true,
                    minimalChatPayload(), LlmCapabilityEnum.FUNCTION_TOOLS);
            Flux<DataBuffer> upstream = ledger.upstream(
                    chatFrame("""
                            {"id":"chatcmpl-78001","object":"chat.completion.chunk","created":1790000000,
                             "model":"vendor-real-model-x",
                             "choices":[{"index":0,"delta":{"content":"你"},"finish_reason":null}]}
                            """),
                    chatFrame("""
                            {"id":"chatcmpl-78001","object":"chat.completion.chunk","created":1790000000,
                             "model":"vendor-real-model-x",
                             "choices":[{"index":0,"delta":{"content":"好"},"finish_reason":null}]}
                            """),
                    chatFrame("""
                            {"id":"chatcmpl-78001","object":"chat.completion.chunk","created":1790000000,
                             "model":"vendor-real-model-x",
                             "choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}
                            """),
                    "data: " + DONE_MARKER + "\n\n")
                    .doOnCancel(() -> upstreamCancelled.set(true))
                    .doOnComplete(() -> upstreamCompleted.set(true));
            // 只请求一帧：这既是「预取不超过 1 帧」的可观察面，也让取消真的发生在第一帧之后而不是流末。
            BaseSubscriber<DataBuffer> subscriber = new BaseSubscriber<DataBuffer>() {

                @Override
                protected void hookOnSubscribe(Subscription subscription) {
                    subscription.request(1);
                }

                @Override
                protected void hookOnNext(DataBuffer frame) {
                    DataBufferUtils.release(frame);
                    if (delivered.incrementAndGet() == 1) {
                        cancel();
                    }
                }
            };

            chatFace().encodeStream(command, upstream).subscribe(subscriber);

            assertThat(delivered.get())
                    .as("the first frame must leave on its own, without buffering the whole stream").isPositive();
            assertThat(upstreamCancelled).as("a downstream cancel has to reach the upstream").isTrue();
            assertThat(upstreamCompleted).as("a cancelled upstream is never drained to completion").isFalse();
            assertThat(ledger.issuedCount()).as("the first frame has to have been pulled").isPositive();
            ledger.assertReleasedExactlyOnce();
        }
    }

    @Nested
    @DisplayName("OPENAI_EMBEDDING：有序 input/float 与向量合同")
    class EmbeddingsFace {

        @Test
        @DisplayName("encodeRequest 产出有序 input 与 encoding_format=float，只改 model")
        void encodeRequestEmitsTheOrderedInputPlusTheFloatEncodingFormat() {
            ObjectNode payload = document("""
                    {
                      "model": "company-embed-v1",
                      "input": ["第三段资料", "第一段资料", "第二段资料"],
                      "encoding_format": "float",
                      "dimensions": 4
                    }
                    """);

            ObjectNode encoded = embeddingsFace().encodeRequest(
                    command(LlmProtocolEnum.OPENAI_EMBEDDING, EMBEDDING_ALIAS, false, payload),
                    route(LlmProtocolEnum.OPENAI_EMBEDDING));

            assertThat(encoded.path("model").asText()).isEqualTo(UPSTREAM_MODEL);
            assertThat(encoded.path("encoding_format").asText()).isEqualTo("float");
            assertThat(valuesOf(encoded.path("input"))).containsExactly("第三段资料", "第一段资料", "第二段资料");
            assertThat(encoded.path("dimensions").asInt()).isEqualTo(4);
            assertThat(write(encoded)).doesNotContain("base64");
        }

        @Test
        @DisplayName("encoding_format 不是 float 即 400，不得被无声改写成 float")
        void encodeRequestRefusesAnyEncodingFormatOtherThanFloat() {
            ObjectNode payload = document("""
                    {"model": "company-embed-v1", "input": ["资料"], "encoding_format": "base64"}
                    """);

            assertThatExceptionOfType(LlmInvocationException.class)
                    .isThrownBy(() -> embeddingsFace().encodeRequest(
                            command(LlmProtocolEnum.OPENAI_EMBEDDING, EMBEDDING_ALIAS, false, payload),
                            route(LlmProtocolEnum.OPENAI_EMBEDDING)))
                    .satisfies(error -> {
                        assertThat(error.getStatus()).isEqualTo(400);
                        assertThat(error.getCode()).isEqualTo(UNSUPPORTED_PARAMETER);
                    });
        }

        @Test
        @DisplayName("stream 请求在出网前被拒；帧一旦交出就必须全部释放且零帧下发")
        void aStreamEmbeddingRequestIsRefusedFailClosedBeforeAnyFrame() {
            ObjectNode payload = document("""
                    {"model": "company-embed-v1", "input": ["资料"], "stream": true}
                    """);
            LlmInvocationCommandDTO streamCommand =
                    command(LlmProtocolEnum.OPENAI_EMBEDDING, EMBEDDING_ALIAS, true, payload);
            LlmModelSnapshotBO.RouteBO route = route(LlmProtocolEnum.OPENAI_EMBEDDING);
            LlmProtocolStrategy strategy = embeddingsFace();

            assertThatExceptionOfType(LlmInvocationException.class)
                    .isThrownBy(() -> strategy.encodeRequest(streamCommand, route))
                    .satisfies(error -> {
                        assertThat(error.getStatus()).isEqualTo(400);
                        assertThat(error.getCode()).isEqualTo(UNSUPPORTED_PARAMETER);
                    });

            BufferLedger ledger = new BufferLedger();
            StreamOutcome outcome = drive(strategy, streamCommand, ledger,
                    "data: {\"object\":\"list\",\"data\":[]}\n\n", "data: " + DONE_MARKER + "\n\n");

            assertThat(outcome.text()).isEmpty();
            assertThat(outcome.dataNodes()).isEmpty();
            assertThat(outcome.doneMarkers()).isZero();
            assertThat(outcome.failure()).isInstanceOfSatisfying(LlmInvocationException.class, error -> {
                assertThat(error.getStatus()).isEqualTo(502);
                assertThat(error.getCode()).isEqualTo(UPSTREAM_PROTOCOL_ERROR);
            });
            assertThat(ledger.unreleasedCount()).as("no handed-over buffer may leak").isZero();
            ledger.assertReleasedExactlyOnce();
        }

        @Test
        @DisplayName("条数/index/宽度任一不合都 502 upstream_protocol_error，而不是返回空成功")
        void vectorCountIndexAndWidthMismatchCollapseToFiveHundredTwo() {
            LlmProtocolStrategy strategy = embeddingsFace();
            ObjectNode payload = document("""
                    {
                      "model": "company-embed-v1",
                      "input": ["第一段资料", "第二段资料"],
                      "encoding_format": "float",
                      "dimensions": 4
                    }
                    """);
            LlmInvocationCommandDTO command =
                    command(LlmProtocolEnum.OPENAI_EMBEDDING, EMBEDDING_ALIAS, false, payload);

            ObjectNode wellFormed = document("""
                    {
                      "object": "list",
                      "model": "vendor-real-model-x",
                      "data": [
                        {"object": "embedding", "index": 0, "embedding": [0.5, 0.25, -1.5, 2.0]},
                        {"object": "embedding", "index": 1, "embedding": [0.125, 0.5, -0.5, 1.0]}
                      ],
                      "usage": {"prompt_tokens": 7, "total_tokens": 7}
                    }
                    """);
            ObjectNode decoded = strategy.decodeUnaryResponse(command, wellFormed);
            assertThat(decoded.path("model").asText()).isEqualTo(EMBEDDING_ALIAS);
            assertThat(decoded.path("data").size()).isEqualTo(2);
            assertThat(decoded.path("data").get(0).path("embedding").size()).isEqualTo(4);
            assertThat(decoded.path("data").get(1).path("embedding").get(0).asDouble()).isEqualTo(0.125);
            assertThat(decoded.path("usage").path("total_tokens").asInt()).isEqualTo(7);

            List<ObjectNode> broken = List.of(
                    // 条数少于 input 条数
                    document("""
                            {"object":"list","model":"vendor-real-model-x",
                             "data":[{"object":"embedding","index":0,"embedding":[0.5,0.25,-1.5,2.0]}]}
                            """),
                    // index 重复，即有一项缺失
                    document("""
                            {"object":"list","model":"vendor-real-model-x",
                             "data":[{"object":"embedding","index":0,"embedding":[0.5,0.25,-1.5,2.0]},
                                     {"object":"embedding","index":0,"embedding":[0.1,0.2,0.3,0.4]}]}
                            """),
                    // 宽度不等于声明维度
                    document("""
                            {"object":"list","model":"vendor-real-model-x",
                             "data":[{"object":"embedding","index":0,"embedding":[0.5,0.25,-1.5]},
                                     {"object":"embedding","index":1,"embedding":[0.1,0.2,0.3,0.4]}]}
                            """),
                    // 空 data 绝不允许当成成功
                    document("""
                            {"object":"list","model":"vendor-real-model-x","data":[]}
                            """));

            for (ObjectNode body : broken) {
                LlmInvocationException error = catchThrowableOfType(
                        () -> strategy.decodeUnaryResponse(command, body.deepCopy()), LlmInvocationException.class);
                assertThat(error.getStatus()).as("status for %s", write(body)).isEqualTo(502);
                assertThat(error.getCode()).as("code for %s", write(body)).isEqualTo(UPSTREAM_PROTOCOL_ERROR);
            }
        }
    }

    @Nested
    @DisplayName("OPENAI_RESPONSES：类型化事件、终态与无托管会话")
    class ResponsesFace {

        @Test
        @DisplayName("encodeRequest 缺 store 补 false；显式 true 与托管会话字段一律 400")
        void encodeRequestForcesStoreFalseAndRefusesManagedSessionFields() {
            ObjectNode payload = document("""
                    {
                      "model": "company-responses",
                      "input": [{"role": "user", "content": "请解释发布流程。"}],
                      "instructions": "仅根据提供的资料回答。",
                      "max_output_tokens": 1024
                    }
                    """);
            LlmProtocolStrategy strategy = responsesFace();
            LlmModelSnapshotBO.RouteBO route = route(LlmProtocolEnum.OPENAI_RESPONSES);

            ObjectNode encoded = strategy.encodeRequest(
                    command(LlmProtocolEnum.OPENAI_RESPONSES, RESPONSES_ALIAS, false, payload), route);
            assertThat(encoded.path("store").isBoolean()).isTrue();
            assertThat(encoded.path("store").asBoolean()).isFalse();
            assertThat(encoded.path("model").asText()).isEqualTo(UPSTREAM_MODEL);
            assertThat(encoded.path("instructions").asText()).isEqualTo("仅根据提供的资料回答。");
            assertThat(encoded.path("input").get(0).path("content").asText()).isEqualTo(PROMPT_TEXT);
            assertThat(encoded.path("max_output_tokens").asInt()).isEqualTo(1024);

            for (String forbidden : List.of("{\"store\": true}", "{\"background\": true}",
                    "{\"conversation\": \"conv_1\"}", "{\"previous_response_id\": \"resp_old\"}")) {
                ObjectNode hostile = document("""
                        {"model": "company-responses", "input": "请解释发布流程。"}
                        """);
                hostile.setAll(document(forbidden));

                assertThatExceptionOfType(LlmInvocationException.class)
                        .isThrownBy(() -> strategy.encodeRequest(
                                command(LlmProtocolEnum.OPENAI_RESPONSES, RESPONSES_ALIAS, false, hostile), route))
                        .satisfies(error -> {
                            assertThat(error.getStatus()).isEqualTo(400);
                            assertThat(error.getCode()).isEqualTo(UNSUPPORTED_PARAMETER);
                        });
            }
        }

        @Test
        @DisplayName("SSE：type 与单调 sequence_number 保真，终态之后不再出帧且绝不发 [DONE]")
        void encodeStreamKeepsTypedEventsMonotonicSequenceNumbersAndStopsAtTheTerminalEvent() {
            BufferLedger ledger = new BufferLedger();
            LlmInvocationCommandDTO command = command(LlmProtocolEnum.OPENAI_RESPONSES, RESPONSES_ALIAS, true,
                    document("""
                            {"model": "company-responses", "input": "请解释发布流程。"}
                            """));

            StreamOutcome outcome = drive(responsesFace(), command, ledger,
                    sseFrame("response.created", """
                            {"type":"response.created","sequence_number":0,
                             "response":{"id":"resp_synthetic_01","object":"response","status":"in_progress",
                               "model":"vendor-real-model-x","output":[],"store":false}}
                            """),
                    sseFrame("response.in_progress", """
                            {"type":"response.in_progress","sequence_number":1,
                             "response":{"id":"resp_synthetic_01","object":"response","status":"in_progress",
                               "model":"vendor-real-model-x","output":[],"store":false}}
                            """),
                    sseFrame("response.output_item.added", """
                            {"type":"response.output_item.added","sequence_number":2,"output_index":0,
                             "item":{"type":"function_call","id":"fc_1","call_id":"call_9","name":"lookup",
                               "arguments":"","status":"in_progress"}}
                            """),
                    sseFrame("response.function_call_arguments.delta", """
                            {"type":"response.function_call_arguments.delta","sequence_number":3,
                             "item_id":"fc_1","output_index":0,"call_id":"call_9","delta":"{\\"city\\":"}
                            """),
                    sseFrame("response.function_call_arguments.done", """
                            {"type":"response.function_call_arguments.done","sequence_number":4,
                             "item_id":"fc_1","output_index":0,"call_id":"call_9",
                             "arguments":"{\\"city\\":\\"上海\\"}"}
                            """),
                    sseFrame("vendor.stream_note", """
                            {"type":"vendor.stream_note","sequence_number":5,"note":"opaque but safe"}
                            """),
                    sseFrame("response.completed", """
                            {"type":"response.completed","sequence_number":6,
                             "response":{"id":"resp_synthetic_01","object":"response","status":"completed",
                               "model":"vendor-real-model-x","store":false,
                               "output":[{"type":"function_call","id":"fc_1","call_id":"call_9","name":"lookup",
                                 "arguments":"{\\"city\\":\\"上海\\"}","status":"completed"}],
                               "usage":{"input_tokens":12,"output_tokens":10,"total_tokens":22}}}
                            """),
                    // 终态之后的帧一律不得再下发。
                    sseFrame("response.output_item.added", """
                            {"type":"response.output_item.added","sequence_number":7,"output_index":1,
                             "item":{"type":"message","id":"msg_after_terminal"}}
                            """));

            assertThat(outcome.failure()).isNull();
            assertThat(outcome.completed()).isTrue();
            assertThat(outcome.eventTypes()).containsExactly("response.created", "response.in_progress",
                    "response.output_item.added", "response.function_call_arguments.delta",
                    "response.function_call_arguments.done", "vendor.stream_note", "response.completed");
            assertThat(outcome.sequenceNumbers()).containsExactly(0, 1, 2, 3, 4, 5, 6);
            assertThat(isStrictlyIncreasing(outcome.sequenceNumbers())).isTrue();
            assertThat(outcome.text()).doesNotContain("msg_after_terminal");
            assertThat(outcome.doneMarkers()).isZero();
            assertThat(outcome.text()).doesNotContain(DONE_MARKER, UPSTREAM_MODEL);
            assertThat(outcome.text()).contains("call_9");
            assertThat(ledger.issuedCount()).isPositive();
            ledger.assertReleasedExactlyOnce();
        }

        @Test
        @DisplayName("incomplete 与 failed 是自己的终态：流到此为止，且不得被改写成 completed")
        void incompleteIsItsOwnTerminalStateAndIsNeverRewrittenAsCompleted() {
            LlmInvocationCommandDTO command = command(LlmProtocolEnum.OPENAI_RESPONSES, RESPONSES_ALIAS, true,
                    document("""
                            {"model": "company-responses", "input": "请解释发布流程。"}
                            """));

            StreamOutcome truncated = drive(responsesFace(), command, new BufferLedger(),
                    sseFrame("response.created", """
                            {"type":"response.created","sequence_number":0,
                             "response":{"id":"resp_synthetic_02","object":"response","status":"in_progress",
                               "model":"vendor-real-model-x","output":[]}}
                            """),
                    sseFrame("response.incomplete", """
                            {"type":"response.incomplete","sequence_number":1,
                             "response":{"id":"resp_synthetic_02","object":"response","status":"incomplete",
                               "incomplete_details":{"reason":"max_output_tokens"},
                               "model":"vendor-real-model-x","output":[]}}
                            """),
                    sseFrame("response.completed", """
                            {"type":"response.completed","sequence_number":2,
                             "response":{"id":"resp_synthetic_02","status":"completed"}}
                            """));

            assertThat(truncated.failure()).isNull();
            assertThat(truncated.completed()).isTrue();
            assertThat(truncated.eventTypes()).containsExactly("response.created", "response.incomplete");
            assertThat(truncated.text()).doesNotContain("response.completed");
            assertThat(fieldValuesOf(truncated.dataNodes(), "type")).doesNotContain("response.completed");

            StreamOutcome failed = drive(responsesFace(), command, new BufferLedger(),
                    sseFrame("response.created", """
                            {"type":"response.created","sequence_number":0,
                             "response":{"id":"resp_synthetic_03","object":"response","status":"in_progress",
                               "model":"vendor-real-model-x","output":[]}}
                            """),
                    sseFrame("response.failed", """
                            {"type":"response.failed","sequence_number":1,
                             "response":{"id":"resp_synthetic_03","object":"response","status":"failed",
                               "error":{"code":"server_error","message":"vendor side failure"},
                               "model":"vendor-real-model-x","output":[]}}
                            """));

            assertThat(failed.completed()).isTrue();
            assertThat(failed.eventTypes()).containsExactly("response.created", "response.failed");
            assertThat(failed.doneMarkers()).isZero();
        }

        @Test
        @DisplayName("EOF 缺少终态即 ABORTED，且绝不伪造终态帧")
        void anEofWithoutTerminalMarkerIsAbortedAndNoTerminalFrameIsFabricated() {
            BufferLedger ledger = new BufferLedger();
            StreamOutcome outcome = drive(responsesFace(),
                    command(LlmProtocolEnum.OPENAI_RESPONSES, RESPONSES_ALIAS, true,
                            document("""
                                    {"model": "company-responses", "input": "请解释发布流程。"}
                                    """)), ledger,
                    sseFrame("response.created", """
                            {"type":"response.created","sequence_number":0,
                             "response":{"id":"resp_synthetic_04","object":"response","status":"in_progress",
                               "model":"vendor-real-model-x","output":[]}}
                            """),
                    sseFrame("response.output_text.delta", """
                            {"type":"response.output_text.delta","sequence_number":1,"item_id":"msg_1",
                             "output_index":0,"content_index":0,"delta":"校验后"}
                            """));

            assertThat(outcome.failure()).isInstanceOfSatisfying(LlmInvocationException.class, error -> {
                assertThat(error.getStatus()).isEqualTo(502);
                assertThat(error.getCode()).isEqualTo(UPSTREAM_PROTOCOL_ERROR);
            });
            assertThat(outcome.completed()).isFalse();
            assertThat(fieldValuesOf(outcome.dataNodes(), "type"))
                    .doesNotContain("response.completed", "response.incomplete");
            assertThat(outcome.text()).doesNotContain(DONE_MARKER);
            ledger.assertReleasedExactlyOnce();
        }

        @Test
        @DisplayName("decodeUnaryResponse 保真 function_call/function_call_output 的 call_id 配对与 opaque reasoning")
        void decodeUnaryResponseKeepsCallIdPairingAndOpaqueReasoning() {
            ObjectNode upstream = document("""
                    {
                      "id": "resp_synthetic_01",
                      "object": "response",
                      "created_at": 1790000000,
                      "status": "completed",
                      "error": null,
                      "incomplete_details": null,
                      "model": "vendor-real-model-x",
                      "output": [
                        {"type": "reasoning", "id": "rs_1", "summary": [],
                         "encrypted_content": "gAAAAABopaqueNotDecoded=="},
                        {"type": "function_call", "id": "fc_1", "call_id": "call_42", "name": "lookup_release",
                         "arguments": "{\\"city\\":\\"上海\\"}", "status": "completed"},
                        {"type": "function_call_output", "call_id": "call_42",
                         "output": "校验后直接发布，并保留版本状态。"},
                        {"type": "message", "id": "msg_1", "role": "assistant", "status": "completed",
                         "content": [{"type": "output_text", "text": "校验后直接发布。", "annotations": []}]}
                      ],
                      "usage": {"input_tokens": 12, "output_tokens": 10, "total_tokens": 22},
                      "store": false
                    }
                    """);

            ObjectNode decoded = responsesFace().decodeUnaryResponse(
                    command(LlmProtocolEnum.OPENAI_RESPONSES, RESPONSES_ALIAS, false,
                            document("""
                                    {"model": "company-responses", "input": "请解释发布流程。"}
                                    """)), upstream);

            JsonNode output = decoded.path("output");
            assertThat(textValues(output, "type")).containsExactly("reasoning", "function_call",
                    "function_call_output", "message");
            assertThat(output.get(1).path("call_id").asText()).isEqualTo("call_42");
            assertThat(output.get(2).path("call_id").asText()).isEqualTo("call_42");
            assertThat(output.get(1).path("arguments").asText()).isEqualTo("{\"city\":\"上海\"}");
            assertThat(output.get(2).path("output").asText()).isEqualTo("校验后直接发布，并保留版本状态。");
            assertThat(output.get(0).path("encrypted_content").asText()).isEqualTo("gAAAAABopaqueNotDecoded==");
            assertThat(decoded.path("id").asText()).isEqualTo("resp_synthetic_01");
            assertThat(decoded.path("status").asText()).isEqualTo("completed");
            assertThat(decoded.path("store").asBoolean()).isFalse();
            assertThat(decoded.path("usage").path("total_tokens").asInt()).isEqualTo(22);
            assertThat(decoded.path("error").isNull()).isTrue();
            assertThat(decoded.path("model").asText()).isEqualTo(RESPONSES_ALIAS);
            assertThat(write(decoded)).doesNotContain(UPSTREAM_MODEL, "\"choices\"", "chat.completion");
        }
    }

    @Nested
    @DisplayName("ANTHROPIC_MESSAGES：顶层 system、content blocks 与原生错误面")
    class MessagesFace {

        @Test
        @DisplayName("encodeRequest 保持 system 在顶层，绝不把它折进 messages")
        void encodeRequestKeepsSystemTopLevelAndNeverFoldsItIntoMessages() {
            ObjectNode payload = document("""
                    {
                      "model": "company-messages",
                      "system": "仅根据提供的资料回答，不得编造。",
                      "messages": [
                        {"role": "user", "content": [{"type": "text", "text": "请解释发布流程。"}]},
                        {"role": "assistant", "content": [
                          {"type": "tool_use", "id": "toolu_01", "name": "lookup_release",
                           "input": {"kb": "wiki"}}]},
                        {"role": "user", "content": [
                          {"type": "tool_result", "tool_use_id": "toolu_01", "content": "校验后直接发布。",
                           "is_error": false}]}
                      ],
                      "tools": [{"name": "lookup_release", "description": "查发布流程",
                                 "input_schema": {"type": "object", "properties": {"kb": {"type": "string"}}}}],
                      "tool_choice": {"type": "auto"},
                      "max_tokens": 1024,
                      "thinking": {"type": "enabled", "budget_tokens": 512}
                    }
                    """);

            ObjectNode encoded = messagesFace().encodeRequest(
                    command(LlmProtocolEnum.ANTHROPIC_MESSAGES, MESSAGES_ALIAS, false, payload,
                            LlmCapabilityEnum.FUNCTION_TOOLS, LlmCapabilityEnum.REASONING),
                    route(LlmProtocolEnum.ANTHROPIC_MESSAGES, LlmCapabilityEnum.FUNCTION_TOOLS,
                            LlmCapabilityEnum.REASONING));

            assertThat(encoded.path("system").asText()).isEqualTo("仅根据提供的资料回答，不得编造。");
            assertThat(textValues(encoded.path("messages"), "role")).containsExactly("user", "assistant", "user");
            assertThat(textValues(encoded.path("messages"), "role")).doesNotContain("system");
            assertThat(encoded.path("messages").get(1).path("content").get(0).path("id").asText())
                    .isEqualTo("toolu_01");
            assertThat(encoded.path("messages").get(2).path("content").get(0).path("tool_use_id").asText())
                    .isEqualTo("toolu_01");
            assertThat(encoded.path("tools").get(0).path("input_schema").path("type").asText())
                    .isEqualTo("object");
            assertThat(encoded.path("thinking").path("budget_tokens").asInt()).isEqualTo(512);
            assertThat(encoded.path("max_tokens").asInt()).isEqualTo(1024);
            assertThat(encoded.path("model").asText()).isEqualTo(UPSTREAM_MODEL);
        }

        @Test
        @DisplayName("decodeUnaryResponse 保真 content block 顺序、tool_use.id 与 thinking.signature")
        void decodeUnaryResponseKeepsBlockOrderToolUseIdAndThinkingSignature() {
            ObjectNode upstream = document("""
                    {
                      "id": "msg_synthetic_02",
                      "type": "message",
                      "role": "assistant",
                      "model": "vendor-real-model-x",
                      "content": [
                        {"type": "thinking", "thinking": "先核对版本再发布。",
                         "signature": "Eq8CCnJ3Dtx+opaqueSignature/abc123=="},
                        {"type": "text", "text": "校验后直接发布。"},
                        {"type": "tool_use", "id": "toolu_7f3a", "name": "lookup_release",
                         "input": {"kb": "wiki", "revision": 7}}
                      ],
                      "stop_reason": "tool_use",
                      "stop_sequence": null,
                      "usage": {"input_tokens": 12, "output_tokens": 8, "cache_read_input_tokens": 4},
                      "vendor_trace": {"replica": "shard-7"}
                    }
                    """);

            ObjectNode decoded = messagesFace().decodeUnaryResponse(
                    command(LlmProtocolEnum.ANTHROPIC_MESSAGES, MESSAGES_ALIAS, false,
                            minimalMessagesPayload()), upstream);

            JsonNode content = decoded.path("content");
            assertThat(textValues(content, "type")).containsExactly("thinking", "text", "tool_use");
            assertThat(content.get(0).path("signature").asText()).isEqualTo("Eq8CCnJ3Dtx+opaqueSignature/abc123==");
            assertThat(content.get(0).path("thinking").asText()).isEqualTo("先核对版本再发布。");
            assertThat(content.get(2).path("id").asText()).isEqualTo("toolu_7f3a");
            assertThat(content.get(2).path("input").path("revision").asInt()).isEqualTo(7);
            assertThat(decoded.path("stop_reason").asText()).isEqualTo("tool_use");
            assertThat(decoded.path("stop_sequence").isNull()).isTrue();
            assertThat(decoded.path("usage").path("cache_read_input_tokens").asInt()).isEqualTo(4);
            assertThat(decoded.path("vendor_trace").path("replica").asText()).isEqualTo("shard-7");
            assertThat(decoded.path("id").asText()).isEqualTo("msg_synthetic_02");
            assertThat(decoded.path("model").asText()).isEqualTo(MESSAGES_ALIAS);
            assertThat(write(decoded)).doesNotContain(UPSTREAM_MODEL, "\"choices\"", "\"tool_calls\"");
        }

        @Test
        @DisplayName("SSE：message_start→content_block_*→message_delta→message_stop 顺序保真，ping 与未知事件原样转发")
        void encodeStreamPreservesTheMessagesEventOrderingAndForwardsPingAndUnknownEvents() {
            BufferLedger ledger = new BufferLedger();
            StreamOutcome outcome = drive(messagesFace(),
                    command(LlmProtocolEnum.ANTHROPIC_MESSAGES, MESSAGES_ALIAS, true, minimalMessagesPayload()),
                    ledger,
                    sseFrame("message_start", """
                            {"type":"message_start","message":{"id":"msg_synthetic_03","type":"message",
                              "role":"assistant","model":"vendor-real-model-x","content":[],
                              "usage":{"input_tokens":12,"output_tokens":1}}}
                            """),
                    sseFrame("ping", "{\"type\":\"ping\"}"),
                    sseFrame("content_block_start", """
                            {"type":"content_block_start","index":0,
                             "content_block":{"type":"text","text":""}}
                            """),
                    sseFrame("content_block_delta", """
                            {"type":"content_block_delta","index":0,
                             "delta":{"type":"text_delta","text":"校验后"}}
                            """),
                    sseFrame("vendor.stream_note", """
                            {"type":"vendor.stream_note","note":"opaque but safe"}
                            """),
                    sseFrame("content_block_stop", "{\"type\":\"content_block_stop\",\"index\":0}"),
                    sseFrame("content_block_start", """
                            {"type":"content_block_start","index":1,
                             "content_block":{"type":"text","text":""}}
                            """),
                    sseFrame("content_block_delta", """
                            {"type":"content_block_delta","index":1,
                             "delta":{"type":"text_delta","text":"直接发布。"}}
                            """),
                    sseFrame("content_block_stop", "{\"type\":\"content_block_stop\",\"index\":1}"),
                    sseFrame("message_delta", """
                            {"type":"message_delta","delta":{"stop_reason":"end_turn","stop_sequence":null},
                             "usage":{"output_tokens":8,"input_tokens":12}}
                            """),
                    sseFrame("message_stop", "{\"type\":\"message_stop\"}"),
                    // 终态之后的第二份 message_stop 不得再下发。
                    sseFrame("message_stop", "{\"type\":\"message_stop\"}"));

            assertThat(outcome.failure()).isNull();
            assertThat(outcome.completed()).isTrue();
            assertThat(outcome.eventTypes()).containsExactly("message_start", "ping", "content_block_start",
                    "content_block_delta", "vendor.stream_note", "content_block_stop", "content_block_start",
                    "content_block_delta", "content_block_stop", "message_delta", "message_stop");
            assertOrdered(outcome.text(), "message_start", "content_block_start", "content_block_delta",
                    "content_block_stop", "message_delta", "message_stop");
            assertThat(outcome.dataNodes()).allSatisfy(node -> assertThat(node.has("model")).isFalse());
            assertThat(outcome.dataNodes().get(0).path("message").path("model").asText()).isEqualTo(MESSAGES_ALIAS);
            assertThat(outcome.text()).doesNotContain(UPSTREAM_MODEL, DONE_MARKER, "\"choices\"");
            ledger.assertReleasedExactlyOnce();
        }

        @Test
        @DisplayName("input_json_delta.partial_json 半段原样转发，只在块结束处完成解析")
        void inputJsonDeltaFragmentsTravelVerbatimAndAreParsedOnlyAtBlockCompletion() {
            StreamOutcome outcome = drive(messagesFace(),
                    command(LlmProtocolEnum.ANTHROPIC_MESSAGES, MESSAGES_ALIAS, true, minimalMessagesPayload(),
                            LlmCapabilityEnum.FUNCTION_TOOLS), new BufferLedger(),
                    sseFrame("message_start", """
                            {"type":"message_start","message":{"id":"msg_synthetic_04","type":"message",
                              "role":"assistant","model":"vendor-real-model-x","content":[],
                              "usage":{"input_tokens":9,"output_tokens":1}}}
                            """),
                    sseFrame("content_block_start", """
                            {"type":"content_block_start","index":0,
                             "content_block":{"type":"tool_use","id":"toolu_5a","name":"lookup_city","input":{}}}
                            """),
                    sseFrame("content_block_delta", """
                            {"type":"content_block_delta","index":0,
                             "delta":{"type":"input_json_delta","partial_json":"{\\"ci"}}
                            """),
                    sseFrame("content_block_delta", """
                            {"type":"content_block_delta","index":0,
                             "delta":{"type":"input_json_delta","partial_json":"ty\\":\\"上海\\"}"}}
                            """),
                    sseFrame("content_block_stop", "{\"type\":\"content_block_stop\",\"index\":0}"),
                    sseFrame("message_delta", """
                            {"type":"message_delta","delta":{"stop_reason":"tool_use","stop_sequence":null},
                             "usage":{"output_tokens":6}}
                            """),
                    sseFrame("message_stop", "{\"type\":\"message_stop\"}"));

            assertThat(outcome.failure()).isNull();
            assertThat(outcome.completed()).isTrue();
            assertThat(outcome.eventTypes()).containsExactly("message_start", "content_block_start",
                    "content_block_delta", "content_block_delta", "content_block_stop", "message_delta",
                    "message_stop");
            // 两个片段各自成帧原样出网：半段 JSON 既没被合并、也没被强行 parse 成对象再重出。
            assertThat(partialJsonFragmentsOf(outcome.text())).containsExactly("{\"ci", "ty\":\"上海\"}");
            assertThat(outcome.text()).contains("{\\\"ci");
        }

        @Test
        @DisplayName("EOF 缺 message_stop 即 ABORTED，绝不伪造 message_stop 成功帧")
        void anEofWithoutMessageStopIsAbortedWithoutFabricatingTheTerminalFrame() {
            BufferLedger ledger = new BufferLedger();
            StreamOutcome outcome = drive(messagesFace(),
                    command(LlmProtocolEnum.ANTHROPIC_MESSAGES, MESSAGES_ALIAS, true, minimalMessagesPayload()),
                    ledger,
                    sseFrame("message_start", """
                            {"type":"message_start","message":{"id":"msg_synthetic_05","type":"message",
                              "role":"assistant","model":"vendor-real-model-x","content":[],
                              "usage":{"input_tokens":9,"output_tokens":1}}}
                            """),
                    sseFrame("content_block_start", """
                            {"type":"content_block_start","index":0,
                             "content_block":{"type":"text","text":""}}
                            """),
                    sseFrame("content_block_delta", """
                            {"type":"content_block_delta","index":0,
                             "delta":{"type":"text_delta","text":"半句"}}
                            """));

            assertThat(outcome.failure()).isInstanceOfSatisfying(LlmInvocationException.class, error -> {
                assertThat(error.getStatus()).isEqualTo(502);
                assertThat(error.getCode()).isEqualTo(UPSTREAM_PROTOCOL_ERROR);
            });
            assertThat(outcome.completed()).isFalse();
            assertThat(outcome.eventTypes()).doesNotContain("message_stop", "message_delta");
            assertThat(outcome.text()).doesNotContain(DONE_MARKER);
            ledger.assertReleasedExactlyOnce();
        }

        @Test
        @DisplayName("Messages 错误面：顶层 type=error，其 error 只有 type 与 message，绝不套 OpenAI 形状")
        void encodeErrorUsesTheNativeMessagesFaceAndNeverTheOpenAiShape() {
            ObjectNode error = messagesFace().encodeError(new LlmInvocationException(400, UNSUPPORTED_PARAMETER,
                    "thinking", "The requested capability is not offered by this alias", false));

            assertThat(error.path("type").asText()).isEqualTo("error");
            JsonNode inner = error.path("error");
            assertThat(inner.isObject()).isTrue();
            assertThat(fieldNames(inner)).containsExactlyInAnyOrder("type", "message");
            assertThat(inner.path("type").asText()).isNotBlank();
            assertThat(inner.path("message").asText()).isNotBlank();
            assertThat(error.has("choices")).isFalse();
            assertThat(error.has("usage")).isFalse();
            assertThat(write(error)).doesNotContain("\"param\"", "\"code\"", "choices");
            assertNoSensitiveFragment(write(error));
        }
    }

    @Nested
    @DisplayName("原生错误面：OpenAI 三面只有 error{message,type,param,code}")
    class NativeErrorFaces {

        @Test
        @DisplayName("三个 OpenAI 面：唯一顶层键 error，其内层恰好四键，原生状态由载体携带")
        void everyOpenAiFaceEncodesExactlyTheFourSafeErrorFields() {
            for (LlmProtocolStrategy face : List.of(chatFace(), embeddingsFace(), responsesFace())) {
                LlmInvocationException carrier = new LlmInvocationException(429, "rate_limit_exceeded", "model",
                        "The engine is saturated; retry this request later", true);

                ObjectNode error = face.encodeError(carrier);

                assertThat(fieldNames(error)).as("top level of %s", face.protocol()).containsExactly("error");
                JsonNode inner = error.path("error");
                assertThat(fieldNames(inner)).as("error keys of %s", face.protocol())
                        .containsExactlyInAnyOrder("message", "type", "param", "code");
                assertThat(inner.path("code").asText()).isEqualTo("rate_limit_exceeded");
                assertThat(inner.path("param").asText()).isEqualTo("model");
                assertThat(inner.path("type").asText()).isNotBlank();
                assertThat(inner.path("message").asText()).isNotBlank();
                assertThat(carrier.getStatus()).as("the native status stays with the carrier of %s", face.protocol())
                        .isEqualTo(429);

                ObjectNode withoutParam = face.encodeError(new LlmInvocationException(502,
                        UPSTREAM_PROTOCOL_ERROR, null, "The upstream stream disagreed with this protocol", false));
                assertThat(withoutParam.path("error").path("param").isNull()).isTrue();
            }
        }

        @Test
        @DisplayName("任何一面的错误文档都不含 baseUrl、secretRef、凭据、upstreamModel 或请求正文")
        void noEncodedErrorEverEchoesABaseUrlSecretUpstreamModelOrRequestPayload() {
            List<LlmProtocolStrategy> faces =
                    List.of(chatFace(), embeddingsFace(), responsesFace(), messagesFace());
            List<LlmInvocationException> carriers = List.of(
                    new LlmInvocationException(400, UNSUPPORTED_PARAMETER, "upstream_url",
                            "The request asserted a field this gateway does not accept", false),
                    new LlmInvocationException(413, "request_too_large", "input",
                            "The submitted document exceeds the configured ceiling", false),
                    new LlmInvocationException(502, UPSTREAM_PROTOCOL_ERROR, "data",
                            "The upstream response disagreed with this protocol", false),
                    new LlmInvocationException(504, "upstream_timeout", null,
                            "The upstream call exceeded its timeout budget", false));

            for (LlmProtocolStrategy face : faces) {
                for (LlmInvocationException carrier : carriers) {
                    String serialized = write(face.encodeError(carrier));

                    assertNoSensitiveFragment(serialized);
                    assertThat(serialized).as("no admin envelope on %s / %s", face.protocol(), carrier.getCode())
                            .doesNotContain("\"traceId\"", "\"timestamp\"", "\"success\"");
                }
            }
        }
    }

    private static LlmProtocolStrategy chatFace() {
        return new OpenAiChatProtocolStrategy(gatewayProperties(), JSON);
    }

    private static LlmProtocolStrategy embeddingsFace() {
        return new OpenAiEmbeddingProtocolStrategy(gatewayProperties(), JSON);
    }

    private static LlmProtocolStrategy responsesFace() {
        return new OpenAiResponsesProtocolStrategy(gatewayProperties(), JSON);
    }

    private static LlmProtocolStrategy messagesFace() {
        return new AnthropicMessagesProtocolStrategy(gatewayProperties(), JSON);
    }

    private static LlmGatewayProperties gatewayProperties() {
        return new LlmGatewayProperties()
                .setEnabled(true)
                .setAllowedLocalCidrs(List.of("local-llm.internal"))
                .setAllowedCloudHosts(List.of("api.cloud-llm.example"))
                .setSecretsRoot("/run/secrets/yuheng-llm")
                .setMaxRequestBytes(1_048_576L)
                .setMaxFrameBytes(65_536)
                .setMaximumAttempts(2)
                .setStreamingThreads(8)
                .setIdentity(new LlmGatewayProperties.Identity()
                        .setResourceUri("https://llm.yuheng.internal")
                        .setServiceTokenAudience("yuheng-llm"));
    }

    private static LlmInvocationCommandDTO command(LlmProtocolEnum protocol, String alias, boolean stream,
            ObjectNode payload, LlmCapabilityEnum... capabilities) {
        return new LlmInvocationCommandDTO()
                .setProtocol(protocol)
                .setModel(alias)
                .setStream(stream)
                .setPayload(payload)
                .setCallerSubject(SUBJECT)
                .setAllowedDeployments(EnumSet.of(LlmDeploymentEnum.LOCAL))
                .setRequiredCapabilities(EnumSet.of(LlmCapabilityEnum.TEXT, capabilities));
    }

    private static LlmModelSnapshotBO.RouteBO route(LlmProtocolEnum protocol, LlmCapabilityEnum... capabilities) {
        LlmModelSnapshotBO.ChannelBO channel = new LlmModelSnapshotBO.ChannelBO()
                .setChannelKey(CHANNEL_KEY)
                .setDeployment(LlmDeploymentEnum.LOCAL)
                .setProtocol(protocol)
                .setBaseUrl(BASE_URL)
                .setSecretRef(SECRET_REF)
                .setEnabled(Boolean.TRUE)
                .setConnectTimeoutMs(2_000)
                .setHeaderTimeoutMs(30_000)
                .setIdleTimeoutMs(60_000)
                .setTotalTimeoutMs(120_000)
                .setMaxConcurrent(4);
        return new LlmModelSnapshotBO.RouteBO()
                .setChannelKey(CHANNEL_KEY)
                .setUpstreamModel(UPSTREAM_MODEL)
                .setPriority(1)
                .setWeight(100)
                .setCapabilities(EnumSet.of(LlmCapabilityEnum.TEXT, capabilities))
                .setChannel(channel);
    }

    private static ObjectNode minimalChatPayload() {
        return document("""
                {"model": "company-chat", "messages": [{"role": "user", "content": "你好"}], "max_tokens": 512}
                """);
    }

    private static ObjectNode minimalMessagesPayload() {
        return document("""
                {"model": "company-messages", "max_tokens": 512,
                 "messages": [{"role": "user", "content": [{"type": "text", "text": "你好"}]}]}
                """);
    }

    private static String chatFrame(String data) {
        return "data: " + data + "\n\n";
    }

    private static String sseFrame(String event, String data) {
        return "event: " + event + "\ndata: " + data + "\n\n";
    }

    private static ObjectNode document(String json) {
        JsonNode parsed = parse(json);
        if (!(parsed instanceof ObjectNode objectNode)) {
            throw new IllegalArgumentException("The fixture is not a JSON object: " + json);
        }
        return objectNode;
    }

    private static JsonNode parse(String json) {
        try {
            return JSON.readTree(json);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Unparseable fixture", failure);
        }
    }

    private static String write(JsonNode node) {
        try {
            return JSON.writeValueAsString(node);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Unserialisable document", failure);
        }
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static List<String> textValues(JsonNode array, String field) {
        List<String> values = new ArrayList<>();
        array.forEach(element -> values.add(element.path(field).asText()));
        return values;
    }

    private static List<String> valuesOf(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(element -> values.add(element.asText()));
        return values;
    }

    private static List<String> fieldValuesOf(List<ObjectNode> nodes, String field) {
        List<String> values = new ArrayList<>();
        nodes.forEach(node -> values.add(node.path(field).asText()));
        return values;
    }

    /**
     * 中文说明：按帧顺序收集全部 {@code choices[].delta.tool_calls[]} 条目，让断言只谈增量片段本身的顺序与内容，
     * 不涉及实现的内部表示。
     * English summary: Collects every {@code choices[].delta.tool_calls[]} entry in frame order, so the assertions talk
     * about the order and content of the delta fragments themselves rather than about an implementation's internals.
     */
    private static List<JsonNode> toolCallsOf(String stream) {
        List<JsonNode> calls = new ArrayList<>();
        for (ObjectNode node : dataNodesOf(stream)) {
            for (JsonNode choice : node.path("choices")) {
                for (JsonNode call : choice.path("delta").path("tool_calls")) {
                    calls.add(call);
                }
            }
        }
        return calls;
    }

    private static List<Integer> toolCallDeltaIndexesOf(String stream) {
        List<Integer> indexes = new ArrayList<>();
        toolCallsOf(stream).forEach(call -> indexes.add(call.path("index").asInt(-1)));
        return indexes;
    }

    private static List<String> toolCallArgumentsOf(String stream) {
        List<String> fragments = new ArrayList<>();
        for (JsonNode call : toolCallsOf(stream)) {
            JsonNode arguments = call.path("function").path("arguments");
            if (arguments.isTextual()) {
                fragments.add(arguments.asText());
            }
        }
        return fragments;
    }

    private static List<String> toolCallIdsOf(String stream) {
        List<String> ids = new ArrayList<>();
        for (JsonNode call : toolCallsOf(stream)) {
            String id = call.path("id").asText();
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static List<String> partialJsonFragmentsOf(String stream) {
        List<String> fragments = new ArrayList<>();
        for (ObjectNode node : dataNodesOf(stream)) {
            JsonNode delta = node.path("delta");
            if ("input_json_delta".equals(delta.path("type").asText())) {
                fragments.add(delta.path("partial_json").asText());
            }
        }
        return fragments;
    }

    private static List<ObjectNode> dataNodesOf(String stream) {
        List<ObjectNode> nodes = new ArrayList<>();
        for (String line : stream.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("data:")) {
                continue;
            }
            String body = trimmed.substring("data:".length()).trim();
            if (body.isEmpty() || body.startsWith(DONE_MARKER)) {
                continue;
            }
            nodes.add(document(body));
        }
        return nodes;
    }

    private static int countOf(String haystack, String needle) {
        int count = 0;
        for (int index = haystack.indexOf(needle); index >= 0; index = haystack.indexOf(needle, index + 1)) {
            count++;
        }
        return count;
    }

    private static void assertOrdered(String text, String... markers) {
        int cursor = -1;
        for (String marker : markers) {
            int found = text.indexOf(marker, cursor + 1);
            assertThat(found).as("marker %s must come after the previous one", marker).isGreaterThan(cursor);
            cursor = found;
        }
    }

    private static boolean isStrictlyIncreasing(List<Integer> numbers) {
        for (int index = 1; index < numbers.size(); index++) {
            if (numbers.get(index) <= numbers.get(index - 1)) {
                return false;
            }
        }
        return true;
    }

    private static void assertNoSensitiveFragment(String document) {
        assertThat(document).doesNotContain(FORBIDDEN_FRAGMENTS.toArray(String[]::new));
    }

    /**
     * 中文说明：一次 {@code encodeStream} 驱动的全部可观察结果：下发文本、类型化事件序列、{@code [DONE]} 计数、
     * 终止信号与失败原因。用 {@code materialize()} 收集，所以既不需要 reactor-test 依赖，也不会把 onError 误判成
     * 「正常结束」。SPI 同时允许「提交前直接抛出」与「以 error 信号收束」两种合法形态，故同步抛出也记为 failure：
     * 被固定的永远是「零帧下发 + 502/504 + 缓冲全部释放」这一可观察合同，而不是其中某一种通知方式。
     * English summary: Everything one {@code encodeStream} run observably produced: the emitted text, the typed event
     * order, the {@code [DONE]} count, the terminal signal and the failure. Collected through {@code materialize()} so no
     * reactor-test dependency is needed and an onError can never be mistaken for a normal completion. The SPI admits both
     * a synchronous pre-commit throw and an error signal, so a throw is recorded as the failure too: what stays pinned is
     * the observable contract of "no frame plus a 502/504 plus every buffer released", never one particular way of
     * announcing it.
     */
    private static StreamOutcome drive(LlmProtocolStrategy strategy, LlmInvocationCommandDTO command,
            BufferLedger ledger, String... frames) {
        List<Signal<DataBuffer>> signals;
        try {
            signals = strategy.encodeStream(command, ledger.upstream(frames))
                    .materialize()
                    .collectList()
                    .block(Duration.ofSeconds(10));
        } catch (LlmInvocationException thrownBeforeCommit) {
            return new StreamOutcome("", thrownBeforeCommit, false);
        }
        StringBuilder text = new StringBuilder();
        Throwable failure = null;
        boolean completed = false;
        for (Signal<DataBuffer> signal : signals == null ? List.<Signal<DataBuffer>>of() : signals) {
            if (signal.getType() == SignalType.ON_NEXT) {
                DataBuffer buffer = signal.get();
                text.append(readText(buffer));
                DataBufferUtils.release(buffer);
            } else if (signal.getType() == SignalType.ON_ERROR) {
                failure = signal.getThrowable();
            } else if (signal.getType() == SignalType.ON_COMPLETE) {
                completed = true;
            }
        }
        return new StreamOutcome(text.toString(), failure, completed);
    }

    private static String readText(DataBuffer source) {
        byte[] bytes = new byte[source.readableByteCount()];
        source.read(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** 中文说明：一次 {@code encodeStream} 运行的结果快照，只在测试断言里出现，不含任何协议语义。 English summary: The result snapshot of one {@code encodeStream} run, used only by assertions and carrying no protocol semantics. */
    private static final class StreamOutcome {

        private final String text;

        private final Throwable failure;

        private final boolean completed;

        private StreamOutcome(String text, Throwable failure, boolean completed) {
            this.text = text;
            this.failure = failure;
            this.completed = completed;
        }

        private String text() {
            return text;
        }

        private Throwable failure() {
            return failure;
        }

        private boolean completed() {
            return completed;
        }

        private List<ObjectNode> dataNodes() {
            return dataNodesOf(text);
        }

        private List<String> eventTypes() {
            return fieldValuesOf(dataNodes(), "type");
        }

        private List<Integer> sequenceNumbers() {
            List<Integer> numbers = new ArrayList<>();
            for (ObjectNode node : dataNodes()) {
                if (node.has("sequence_number")) {
                    numbers.add(node.path("sequence_number").asInt());
                }
            }
            return numbers;
        }

        private int doneMarkers() {
            return countOf(text, DONE_MARKER);
        }
    }

    /**
     * 中文说明：上游缓冲台账：交给 {@code encodeStream} 的每一帧都包成一个计数代理，代理实现
     * {@link PooledDataBuffer}，因此 {@code DataBufferUtils.release} 必然落到它的 {@code release()} 上并被记数；其余
     * 读取方法全部转交给 Spring 共享工厂造出的真缓冲，语义与真实上游一致。
     * English summary: The upstream buffer ledger: every frame handed to {@code encodeStream} is wrapped in a counting
     * proxy that implements {@link PooledDataBuffer}, so {@code DataBufferUtils.release} always lands on its
     * {@code release()} and gets counted, while every read method is forwarded to a real buffer from Spring's shared
     * factory so the semantics stay the ones of a live upstream.
     */
    private static final class BufferLedger {

        private final List<Entry> entries = new CopyOnWriteArrayList<>();

        private Flux<DataBuffer> upstream(String... frames) {
            return Flux.fromIterable(Arrays.asList(frames)).map(this::issued);
        }

        private int issuedCount() {
            return entries.size();
        }

        private int unreleasedCount() {
            int unreleased = 0;
            for (Entry entry : entries) {
                if (entry.releases().get() == 0) {
                    unreleased++;
                }
            }
            return unreleased;
        }

        private void assertReleasedExactlyOnce() {
            for (Entry entry : entries) {
                assertThat(entry.releases().get())
                        .as("buffer for frame %s released exactly once", entry.frame())
                        .isEqualTo(1);
            }
        }

        private DataBuffer issued(String frame) {
            AtomicInteger releases = new AtomicInteger();
            DataBuffer delegate =
                    DefaultDataBufferFactory.sharedInstance.wrap(frame.getBytes(StandardCharsets.UTF_8));
            Object proxy = Proxy.newProxyInstance(LlmProtocolContractTest.class.getClassLoader(),
                    new Class<?>[] {PooledDataBuffer.class}, (instance, method, arguments) -> {
                        String name = method.getName();
                        if ("release".equals(name)) {
                            releases.incrementAndGet();
                            return Boolean.TRUE;
                        }
                        if ("isAllocated".equals(name)) {
                            return Boolean.TRUE;
                        }
                        if ("retain".equals(name) || "touch".equals(name)) {
                            return instance;
                        }
                        if ("hashCode".equals(name)) {
                            return System.identityHashCode(instance);
                        }
                        if ("equals".equals(name)) {
                            return instance == arguments[0];
                        }
                        if ("toString".equals(name)) {
                            return "counting-upstream-frame";
                        }
                        return method.invoke(delegate, arguments);
                    });
            entries.add(new Entry((DataBuffer) proxy, frame, releases));
            return (DataBuffer) proxy;
        }

        private record Entry(DataBuffer buffer, String frame, AtomicInteger releases) {
        }
    }
}
