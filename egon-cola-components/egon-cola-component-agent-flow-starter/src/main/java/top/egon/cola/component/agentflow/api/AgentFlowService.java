package top.egon.cola.component.agentflow.api;

import com.google.adk.events.Event;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.agentflow.config.AgentFlowSessionValidationGroup;

import java.util.List;

/** Internal Java facade for compiled flow discovery and in-memory session execution. */
public interface AgentFlowService {

    List<AgentFlowDescriptorDTO> listFlows();

    @Validated(AgentFlowSessionValidationGroup.Create.class)
    AgentFlowSessionResult createSession(@Valid AgentFlowSessionCommand command);

    @Validated(AgentFlowSessionValidationGroup.Delete.class)
    void deleteSession(@Valid AgentFlowSessionCommand command);

    List<Event> execute(@Valid AgentFlowExecutionCommand command);

    Flowable<Event> executeStream(@Valid AgentFlowExecutionCommand command);
}
