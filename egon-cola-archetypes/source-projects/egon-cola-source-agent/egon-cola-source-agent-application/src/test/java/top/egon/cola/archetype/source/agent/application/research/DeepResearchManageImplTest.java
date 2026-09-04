package top.egon.cola.archetype.source.agent.application.research;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.agent.application.research.command.StartDeepResearchCommand;
import top.egon.cola.archetype.source.agent.application.research.config.DeepResearchRuntimeProperties;
import top.egon.cola.archetype.source.agent.application.research.exception.DeepResearchApplicationException;
import top.egon.cola.archetype.source.agent.application.research.manage.impl.DeepResearchManageImpl;
import top.egon.cola.archetype.source.agent.application.research.service.ResearchCapacityService;
import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.research.gateway.DeepResearchAgentGateway;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchEvent;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchTaskBO;
import top.egon.cola.archetype.source.agent.domain.research.model.ReportLanguageEnum;
import top.egon.cola.archetype.source.agent.domain.research.model.ResearchStageEnum;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchEventObserverService;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchRunService;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeepResearchManageImplTest {

    private static ValidatorFactory validatorFactory;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void saturates_four_fair_permits_before_gateway_and_reuses_after_cancel() {
        RecordingGateway gateway = new RecordingGateway();
        DeepResearchManageImpl manage = manage(gateway, 4);
        List<DeepResearchRunService> runs = new ArrayList<>();

        for (int index = 0; index < 4; index++) {
            runs.add(manage.startResearch(command("topic-" + index), event -> { }));
        }
        DeepResearchApplicationException capacityFailure = assertThrows(DeepResearchApplicationException.class,
                () -> manage.startResearch(command("fifth"), event -> { }));
        assertEquals(ResearchErrorCodeEnum.RESEARCH_CAPACITY_EXHAUSTED, capacityFailure.code());
        assertEquals(4, gateway.starts);

        runs.getFirst().cancel();
        assertNotNull(manage.startResearch(command("after-cancel"), event -> { }));
        assertEquals(5, gateway.starts);
    }

    @Test
    void emits_only_one_terminal_outcome_and_releases_once() {
        RecordingGateway gateway = new RecordingGateway();
        DeepResearchManageImpl manage = manage(gateway, 1);
        List<DeepResearchEvent> received = new ArrayList<>();
        DeepResearchRunService run = manage.startResearch(command("terminal"), received::add);

        gateway.emit(DeepResearchEvent.completed(run.runId(), 1, "report",
                Instant.parse("2026-09-04T08:00:01Z"), "trace-1"));
        gateway.emit(DeepResearchEvent.failed(run.runId(), 2, ResearchErrorCodeEnum.RESEARCH_INTERNAL_ERROR,
                Instant.parse("2026-09-04T08:00:02Z"), "trace-1"));

        assertEquals(1, received.size());
        assertEquals("COMPLETED", received.getFirst().type().name());
        assertNotNull(manage.startResearch(command("reuse"), event -> { }));
    }

    @Test
    void releases_capacity_when_gateway_fails_or_observer_throws() {
        RecordingGateway failingGateway = new RecordingGateway();
        failingGateway.failOnStart = true;
        DeepResearchManageImpl failingManage = manage(failingGateway, 1);
        assertThrows(DeepResearchApplicationException.class,
                () -> failingManage.startResearch(command("sync-failure"), event -> { }));
        failingGateway.failOnStart = false;
        assertNotNull(failingManage.startResearch(command("after-failure"), event -> { }));

        RecordingGateway observerGateway = new RecordingGateway();
        DeepResearchManageImpl observerManage = manage(observerGateway, 1);
        DeepResearchRunService run = observerManage.startResearch(command("observer-failure"), event -> {
            throw new IllegalStateException("observer failure");
        });
        assertThrows(IllegalStateException.class, () -> observerGateway.emit(DeepResearchEvent.progress(run.runId(), 1,
                ResearchStageEnum.PLANNING, "Planner", "planning",
                Instant.parse("2026-09-04T08:00:01Z"), "trace-1")));
        assertNotNull(observerManage.startResearch(command("after-observer"), event -> { }));
    }

    private static DeepResearchManageImpl manage(RecordingGateway gateway, int permits) {
        DeepResearchRuntimeProperties properties = new DeepResearchRuntimeProperties(
                permits, Duration.ofMinutes(5), 20, Duration.ofSeconds(15));
        return new DeepResearchManageImpl(gateway, new ResearchCapacityService(permits),
                new ValidationUtils(validatorFactory.getValidator()), properties,
                Clock.fixed(Instant.parse("2026-09-04T08:00:00Z"), ZoneOffset.UTC));
    }

    private static StartDeepResearchCommand command(String topic) {
        return new StartDeepResearchCommand(topic, ReportLanguageEnum.ZH_CN, 8, "trace-1");
    }

    private static final class RecordingGateway implements DeepResearchAgentGateway {
        private final AtomicInteger nextRun = new AtomicInteger();
        private final List<DeepResearchEventObserverService> observers = new ArrayList<>();
        private int starts;
        private boolean failOnStart;

        @Override
        public DeepResearchRunService start(DeepResearchTaskBO task, DeepResearchEventObserverService observer) {
            starts++;
            if (failOnStart) {
                throw new IllegalStateException("gateway unavailable");
            }
            observers.add(observer);
            return new RecordingRun(task.runId(), nextRun);
        }

        private void emit(DeepResearchEvent event) {
            observers.getLast().onEvent(event);
        }
    }

    private static final class RecordingRun implements DeepResearchRunService {
        private final String runId;
        private final AtomicInteger cancellations;

        private RecordingRun(String runId, AtomicInteger cancellations) {
            this.runId = runId;
            this.cancellations = cancellations;
        }

        @Override
        public String runId() {
            return runId;
        }

        @Override
        public void cancel() {
            cancellations.incrementAndGet();
        }
    }
}
