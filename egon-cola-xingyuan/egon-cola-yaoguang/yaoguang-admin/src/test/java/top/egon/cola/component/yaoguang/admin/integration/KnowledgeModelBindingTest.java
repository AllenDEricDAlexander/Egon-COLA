package top.egon.cola.component.yaoguang.admin.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yaoguang.admin.config.properties.KnowledgeProperties;
import top.egon.cola.component.yaoguang.admin.knowledge.domain.bo.KnowledgeBaseBO;
import top.egon.cola.component.yaoguang.admin.knowledge.domain.dto.KnowledgeBaseCommandDTO;
import top.egon.cola.component.yaoguang.admin.knowledge.domain.dto.KnowledgeModelPolicyDTO;
import top.egon.cola.component.yaoguang.admin.knowledge.domain.enums.KnowledgeEgressPolicyEnum;
import top.egon.cola.component.yaoguang.admin.knowledge.domain.enums.KnowledgeModelDeploymentEnum;
import top.egon.cola.component.yaoguang.admin.knowledge.domain.enums.KnowledgeModelKindEnum;
import top.egon.cola.component.yaoguang.admin.knowledge.domain.enums.KnowledgeModelProtocolEnum;
import top.egon.cola.component.yaoguang.admin.knowledge.repository.KnowledgeRepository;
import top.egon.cola.component.yaoguang.admin.knowledge.service.KnowledgeModelClientService;
import top.egon.cola.component.yaoguang.admin.knowledge.service.KnowledgeRetrievalService;
import top.egon.cola.component.yaoguang.admin.knowledge.service.impl.KnowledgeServiceImpl;
import top.egon.cola.component.yaoguang.admin.shared.domain.AdminActor;
import top.egon.cola.component.yaoguang.admin.shared.domain.bo.IdempotencyBO;
import top.egon.cola.component.yaoguang.admin.shared.domain.enums.AdminActorTypeEnum;
import top.egon.cola.component.yaoguang.admin.shared.repository.IdempotencyRepository;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KnowledgeModelBindingTest {

    private final KnowledgeRepository repository = mock(KnowledgeRepository.class);
    private final KnowledgeModelClientService models = mock(KnowledgeModelClientService.class);
    private final IdempotencyRepository intents = mock(IdempotencyRepository.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final AdminActor actor = new AdminActor("owner-a", AdminActorTypeEnum.USER,
            Set.of("yuheng:knowledge:write"), Set.of());
    private final KnowledgeServiceImpl service = new KnowledgeServiceImpl(repository,
            mock(KnowledgeRetrievalService.class), new KnowledgeProperties(), models, transactions,
            intents, new ObjectMapper(), Clock.systemUTC());

    @BeforeEach
    void installScope() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.TestingAuthenticationToken(
                        "owner-a", "irrelevant", "SCOPE_yuheng:knowledge:write"));
    }

    @AfterEach
    void clearScope() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void policyLookupPrecedesTheLocalWriteAndReplayNeedsNoRemoteDependency() {
        when(intents.find(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(models.describeModels(any())).thenReturn(policies(KnowledgeModelDeploymentEnum.LOCAL));
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(repository.insertBase(any())).thenAnswer(call -> call.<KnowledgeBaseBO>getArgument(0).setId("7001"));

        var result = service.createBase(actor, command(), "knowledge-create-intent-0001");
        assertThat(result.getEmbeddingSpaceId()).isEqualTo("space-a");
        assertThat(result.getDimensions()).isEqualTo(8);
        var order = inOrder(models, transactions, repository, intents);
        order.verify(models).describeModels(List.of("chat-main", "embedding-main"));
        order.verify(transactions).getTransaction(any());
        order.verify(repository).insertBase(any());
        var saved = ArgumentCaptor.forClass(IdempotencyBO.class);
        order.verify(intents).save(saved.capture());
        order.verify(transactions).commit(any());

        var base = ArgumentCaptor.forClass(KnowledgeBaseBO.class);
        verify(repository).insertBase(base.capture());
        when(repository.findBase("7001")).thenReturn(Optional.of(base.getValue()));
        when(intents.find(anyString(), anyString(), anyString())).thenReturn(Optional.of(saved.getValue()));
        assertThat(service.createBase(actor, command(), "knowledge-create-intent-0001").getId()).isEqualTo("7001");
        verify(models, times(1)).describeModels(any());
        verify(transactions, times(1)).getTransaction(any());
    }

    @Test
    void aCloudEmbeddingPolicyFailsBeforeOpeningAnyWriteTransaction() {
        when(intents.find(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(models.describeModels(any())).thenReturn(policies(KnowledgeModelDeploymentEnum.CLOUD));
        assertThatThrownBy(() -> service.createBase(actor, command(), "knowledge-create-intent-0001"))
                .isInstanceOfSatisfying(CommonException.class, error -> assertThat(error.getCode()).isEqualTo(422));
        verifyNoInteractions(transactions, repository);
    }

    private KnowledgeBaseCommandDTO command() {
        return new KnowledgeBaseCommandDTO().setName("Knowledge").setDescription("")
                .setEgressPolicy(KnowledgeEgressPolicyEnum.LOCAL_ONLY)
                .setChatModel("chat-main").setEmbeddingModel("embedding-main");
    }

    private List<KnowledgeModelPolicyDTO> policies(KnowledgeModelDeploymentEnum embeddingDeployment) {
        return List.of(new KnowledgeModelPolicyDTO("chat-main", KnowledgeModelKindEnum.CHAT,
                        List.of(KnowledgeModelProtocolEnum.OPENAI_CHAT), 2L, null, null, true,
                        List.of(KnowledgeModelDeploymentEnum.LOCAL)),
                new KnowledgeModelPolicyDTO("embedding-main", KnowledgeModelKindEnum.EMBEDDING,
                        List.of(KnowledgeModelProtocolEnum.OPENAI_EMBEDDING), 3L, 8, "space-a", true,
                        List.of(embeddingDeployment)));
    }
}
