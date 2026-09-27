package top.egon.cola.component.yuheng.llm.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yuheng.llm.config.LlmGatewayProperties;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.dto.LlmInvocationCommandDTO;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmCapabilityEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmModelKindEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmRouteSelectionStrategy;

/**
 * 中文说明：{@code LlmRoutingContractTest} 固定 Step 10 的路由合同顺序：授权、能力与「embedding 只许本地」这些过滤
 * 必须发生在加权选择<b>之前</b>，因此一次云 embedding 尝试都不可能发生——不是「选了云再回退」，而是云渠道在候选
 * 生成阶段就被排除，配置错了直接 422。断言只走公开入口 {@link LlmRouteSelectionStrategy#select}，不碰任何私有方法，
 * 也不依赖 Spring 容器：本 Step 不写配置，容器装配属运维启动期的事实。
 * English summary: {@code LlmRoutingContractTest} pins Step 10's routing order: authorization, capability and the
 * local-only embedding policy must run <b>before</b> weighted selection, so a cloud embedding attempt cannot happen —
 * a cloud channel is dropped while candidates are produced rather than chosen and then retried, and a misconfigured
 * alias fails with 422 outright. Assertions drive only the public entry
 * {@link LlmRouteSelectionStrategy#select}, touch no private member and start no Spring context, because this Step
 * writes no configuration and container assembly stays a deployment-time fact.
 *
 * 用法 / Usage: {@code ./mvnw -o -pl …/yuheng-llm-gateway -am -Dtest=LlmRoutingContractTest …} 聚焦执行。
 */
class LlmRoutingContractTest {

    private static final String SUBJECT = "svc:wiki-indexer";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    @DisplayName("embedding 别名一旦绑定云渠道即 422，不进入任何候选")
    void embeddingAliasWithCloudChannelFailsClosedBeforeSelection() {
        LlmModelSnapshotBO alias = embeddingAlias(
                route("local-a", LlmDeploymentEnum.LOCAL, "http://local-llm.internal:8000", 1, 100,
                        EnumSet.of(LlmCapabilityEnum.TEXT)),
                route("cloud-b", LlmDeploymentEnum.CLOUD, "https://api.cloud-llm.example", 2, 400,
                        EnumSet.of(LlmCapabilityEnum.TEXT)));

        assertThatExceptionOfType(CommonException.class)
                .isThrownBy(() -> strategy(true, List.of("local-llm.internal"), List.of("api.cloud-llm.example"))
                        .select(chatCommand(LlmProtocolEnum.OPENAI_CHAT, EnumSet.allOf(LlmDeploymentEnum.class)), alias))
                .satisfies(error -> {
                    assertThat(error.getCode()).isEqualTo(422);
                    assertThat(error.getStatus()).isEqualTo("LLM_EMBEDDING_CLOUD_CONFIGURED");
                });
    }

    @Test
    @DisplayName("embedding 的出网许可恒为 LOCAL，即使调用方允许 CLOUD 也从未被选中")
    void embeddingEgressStaysLocalOnlyAcrossRepeatedDraws() {
        LlmRouteSelectionStrategy strategy =
                strategy(true, List.of("local-llm.internal"), List.of("api.cloud-llm.example"));
        LlmModelSnapshotBO alias = embeddingAlias(
                route("local-a", LlmDeploymentEnum.LOCAL, "http://local-llm.internal:8000", 1, 60,
                        EnumSet.of(LlmCapabilityEnum.TEXT)),
                route("local-b", LlmDeploymentEnum.LOCAL, "http://local-llm.internal:8001", 2, 40,
                        EnumSet.of(LlmCapabilityEnum.TEXT)));
        LlmInvocationCommandDTO command =
                chatCommand(LlmProtocolEnum.OPENAI_CHAT, EnumSet.allOf(LlmDeploymentEnum.class));

        List<LlmModelSnapshotBO.RouteBO> draws = IntStream.range(0, 40)
                .mapToObj(index -> strategy.select(command, alias))
                .flatMap(candidates -> candidates.stream())
                .toList();

        assertThat(draws).isNotEmpty();
        assertThat(draws.stream().map(route -> route.getChannel().getDeployment()).collect(Collectors.toSet()))
                .containsExactly(LlmDeploymentEnum.LOCAL);
    }

    @Test
    @DisplayName("授权先于加权选择：主体、启用状态与协议三道门各自失败码不同")
    void authorizationPrecedesWeightedSelection() {
        LlmRouteSelectionStrategy strategy =
                strategy(true, List.of("local-llm.internal"), List.of("api.cloud-llm.example"));
        LlmModelSnapshotBO alias = chatAlias(route("local-a", LlmDeploymentEnum.LOCAL,
                "http://local-llm.internal:8000", 1, 100, EnumSet.of(LlmCapabilityEnum.TEXT)));

        assertThatExceptionOfType(CommonException.class)
                .isThrownBy(() -> strategy.select(
                        chatCommand(LlmProtocolEnum.OPENAI_CHAT, EnumSet.of(LlmDeploymentEnum.LOCAL))
                                .setCallerSubject("svc:not-allowed"), alias))
                .satisfies(error -> {
                    assertThat(error.getCode()).isEqualTo(403);
                    assertThat(error.getStatus()).isEqualTo("LLM_MODEL_NOT_AUTHORIZED");
                });

        alias.setEnabled(Boolean.FALSE);
        assertThatExceptionOfType(CommonException.class)
                .isThrownBy(() -> strategy.select(
                        chatCommand(LlmProtocolEnum.OPENAI_CHAT, EnumSet.of(LlmDeploymentEnum.LOCAL)), alias))
                .satisfies(error -> assertThat(error.getCode()).isEqualTo(404));

        alias.setEnabled(Boolean.TRUE);
        assertThatExceptionOfType(CommonException.class)
                .isThrownBy(() -> strategy.select(
                        chatCommand(LlmProtocolEnum.ANTHROPIC_MESSAGES, EnumSet.of(LlmDeploymentEnum.LOCAL)), alias))
                .satisfies(error -> {
                    assertThat(error.getCode()).isEqualTo(503);
                    assertThat(error.getStatus()).isEqualTo("LLM_NO_ELIGIBLE_ROUTE");
                });
    }

    @Test
    @DisplayName("能力不覆盖的渠道权重再高也被剔除，过滤发生在排序之前")
    void capabilityFilterDropsTheHeaviestRouteBeforeOrdering() {
        LlmRouteSelectionStrategy strategy =
                strategy(true, List.of("local-llm.internal"), List.of("api.cloud-llm.example"));
        LlmModelSnapshotBO alias = chatAlias(
                route("toolless", LlmDeploymentEnum.LOCAL, "http://local-llm.internal:8000", 1, 900,
                        EnumSet.of(LlmCapabilityEnum.TEXT)),
                route("tool-capable", LlmDeploymentEnum.LOCAL, "http://local-llm.internal:8001", 3, 10,
                        EnumSet.of(LlmCapabilityEnum.TEXT, LlmCapabilityEnum.FUNCTION_TOOLS)));
        LlmInvocationCommandDTO command =
                chatCommand(LlmProtocolEnum.OPENAI_CHAT, EnumSet.of(LlmDeploymentEnum.LOCAL))
                        .setRequiredCapabilities(Set.of(LlmCapabilityEnum.FUNCTION_TOOLS));

        List<LlmModelSnapshotBO.RouteBO> candidates =
                IntStream.range(0, 20).mapToObj(index -> strategy.select(command, alias))
                        .flatMap(List::stream).toList();

        assertThat(candidates).isNotEmpty();
        assertThat(candidates.stream().map(LlmModelSnapshotBO.RouteBO::getChannelKey).collect(Collectors.toSet()))
                .containsExactly("tool-capable");
        assertThat(strategy.select(command, alias).get(0).getChannelKey()).isEqualTo("tool-capable");
    }

    @Test
    @DisplayName("本地白名单为空即 503，且不回落到云渠道")
    void denyByDefaultWithoutAnAllowedLocalEgress() {
        LlmRouteSelectionStrategy strategy = strategy(true, List.of(), List.of("api.cloud-llm.example"));
        LlmModelSnapshotBO alias = chatAlias(
                route("local-a", LlmDeploymentEnum.LOCAL, "http://local-llm.internal:8000", 1, 60,
                        EnumSet.of(LlmCapabilityEnum.TEXT)),
                route("cloud-b", LlmDeploymentEnum.CLOUD, "https://api.cloud-llm.example", 2, 40,
                        EnumSet.of(LlmCapabilityEnum.TEXT)));

        assertThatExceptionOfType(CommonException.class)
                .isThrownBy(() -> strategy.select(
                        chatCommand(LlmProtocolEnum.OPENAI_CHAT, EnumSet.of(LlmDeploymentEnum.LOCAL)), alias))
                .satisfies(error -> {
                    assertThat(error.getCode()).isEqualTo(503);
                    assertThat(error.getStatus()).isEqualTo("LLM_NO_ELIGIBLE_ROUTE");
                });
    }

    @Test
    @DisplayName("通过全部过滤后才按优先级升序、带权抽取，云渠道需主机在白名单内")
    void orderedCandidatesOnlyAfterEveryFilterAndCloudHostMustBeAllowListed() {
        LlmRouteSelectionStrategy strategy =
                strategy(true, List.of("local-llm.internal"), List.of("api.cloud-llm.example"));
        LlmModelSnapshotBO alias = chatAlias(
                route("cloud-b", LlmDeploymentEnum.CLOUD, "https://api.cloud-llm.example", 2, 700,
                        EnumSet.of(LlmCapabilityEnum.TEXT)),
                route("local-a", LlmDeploymentEnum.LOCAL, "http://local-llm.internal:8000", 1, 300,
                        EnumSet.of(LlmCapabilityEnum.TEXT)),
                route("cloud-unlisted", LlmDeploymentEnum.CLOUD, "https://evil.example", 1, 900,
                        EnumSet.of(LlmCapabilityEnum.TEXT)));

        List<LlmModelSnapshotBO.RouteBO> candidates = strategy.select(
                chatCommand(LlmProtocolEnum.OPENAI_CHAT, EnumSet.allOf(LlmDeploymentEnum.class)), alias);

        assertThat(candidates).extracting(LlmModelSnapshotBO.RouteBO::getChannelKey)
                .contains("local-a", "cloud-b")
                .doesNotContain("cloud-unlisted");
        assertThat(candidates.get(0).getPriority()).isEqualTo(1);
    }

    private static LlmRouteSelectionStrategy strategy(boolean enabled, List<String> localEgress,
            List<String> cloudHosts) {
        LlmGatewayProperties properties = new LlmGatewayProperties()
                .setEnabled(enabled)
                .setAllowedLocalCidrs(localEgress)
                .setAllowedCloudHosts(cloudHosts)
                .setSecretsRoot("/run/secrets/yuheng-llm")
                .setMaxRequestBytes(1_048_576L)
                .setMaxFrameBytes(65_536)
                .setMaximumAttempts(3)
                .setStreamingThreads(8)
                .setIdentity(new LlmGatewayProperties.Identity()
                        .setResourceUri("https://llm.yuheng.internal")
                        .setServiceTokenAudience("yuheng-llm"));
        return new LlmRouteSelectionStrategy(properties);
    }

    private static LlmInvocationCommandDTO chatCommand(LlmProtocolEnum protocol,
            Set<LlmDeploymentEnum> allowedDeployments) {
        return new LlmInvocationCommandDTO()
                .setProtocol(protocol)
                .setModel("text-embedding-local")
                .setStream(Boolean.FALSE)
                .setPayload(JSON.createObjectNode().put("input", "contract"))
                .setCallerSubject(SUBJECT)
                .setAllowedDeployments(allowedDeployments)
                .setRequiredCapabilities(Set.of());
    }

    private static LlmModelSnapshotBO embeddingAlias(LlmModelSnapshotBO.RouteBO... routes) {
        return alias(LlmModelKindEnum.EMBEDDING, routes)
                .setDimensions(1024)
                .setEmbeddingSpaceId("space:wiki");
    }

    private static LlmModelSnapshotBO chatAlias(LlmModelSnapshotBO.RouteBO... routes) {
        return alias(LlmModelKindEnum.CHAT, routes);
    }

    private static LlmModelSnapshotBO alias(LlmModelKindEnum kind, LlmModelSnapshotBO.RouteBO... routes) {
        return new LlmModelSnapshotBO()
                .setModelKey("text-embedding-local")
                .setName("本地向量模型")
                .setCreatedAt(Instant.parse("2026-09-21T08:00:00Z"))
                .setKind(kind)
                .setEnabled(Boolean.TRUE)
                .setProtocols(List.of(LlmProtocolEnum.OPENAI_CHAT, LlmProtocolEnum.OPENAI_EMBEDDING))
                .setAllowedSubjects(List.of(SUBJECT))
                .setRevision(7L)
                .setRoutes(List.of(routes));
    }

    private static LlmModelSnapshotBO.RouteBO route(String channelKey, LlmDeploymentEnum deployment,
            String baseUrl, int priority, int weight, Set<LlmCapabilityEnum> capabilities) {
        LlmModelSnapshotBO.ChannelBO channel = new LlmModelSnapshotBO.ChannelBO()
                .setChannelKey(channelKey)
                .setDeployment(deployment)
                .setProtocol(LlmProtocolEnum.OPENAI_CHAT)
                .setBaseUrl(baseUrl)
                .setSecretRef("env:LLM_" + channelKey.toUpperCase().replace('-', '_'))
                .setEnabled(Boolean.TRUE)
                .setConnectTimeoutMs(2_000)
                .setHeaderTimeoutMs(30_000)
                .setIdleTimeoutMs(60_000)
                .setTotalTimeoutMs(120_000)
                .setMaxConcurrent(4);
        return new LlmModelSnapshotBO.RouteBO()
                .setChannelKey(channelKey)
                .setUpstreamModel("upstream-" + channelKey)
                .setPriority(priority)
                .setWeight(weight)
                .setCapabilities(capabilities)
                .setChannel(channel);
    }
}
