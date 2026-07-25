package top.egon.cola.component.ddc.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTarget;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcManagementDtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void publishTaskRoundTripsWithoutPersistenceFields() throws Exception {
        DdcManagementPublishTask task = new DdcManagementPublishTask(
                "change-1",
                DdcManagementPublishStatus.SUCCESS,
                3L,
                "checksum",
                1,
                1,
                0,
                0,
                0,
                1,
                List.of(new DdcManagementPublishTarget(
                        "instance-1",
                        "lease-1",
                        3L,
                        "SUCCESS",
                        null,
                        Instant.parse("2026-07-25T00:00:03Z")
                )),
                null,
                Instant.parse("2026-07-25T00:00:00Z"),
                Instant.parse("2026-07-25T00:00:01Z"),
                Instant.parse("2026-07-25T00:00:03Z")
        );

        String json = objectMapper.writeValueAsString(task);
        DdcManagementPublishTask restored =
                objectMapper.readValue(json, DdcManagementPublishTask.class);

        assertThat(restored).isEqualTo(task);
        assertThat(json)
                .doesNotContain("entity")
                .doesNotContain("repository")
                .doesNotContain("redisKey");
    }
}
