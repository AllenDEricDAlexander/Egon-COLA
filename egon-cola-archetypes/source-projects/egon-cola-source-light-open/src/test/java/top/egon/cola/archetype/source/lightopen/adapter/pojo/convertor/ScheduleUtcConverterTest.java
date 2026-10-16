package top.egon.cola.archetype.source.lightopen.adapter.pojo.convertor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.convertor.TeachingApplicationConvertorImpl;
import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.lightopen.domain.teaching.enums.CourseStatus;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.ScheduleCourseDTO;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.po.ClassCourseSchedulePO;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ScheduleUtcConverterTest {
    @Test
    void allWindowCarriersDeclareInstant() throws Exception {
        for (Class<?> type : new Class<?>[]{top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.dto.ScheduleCourseDTO.class,
                ScheduleCourseDTO.class, ScheduleCourseCommand.class}) {
            assertThat(type.getDeclaredField("startsAt").getType()).isEqualTo(Instant.class);
            assertThat(type.getDeclaredField("endsAt").getType()).isEqualTo(Instant.class);
        }
        assertThat(ClassCourseSchedulePO.class.getDeclaredField("startsAt").getType()).isEqualTo(Instant.class);
        assertThat(ClassCourseSchedulePO.class.getDeclaredField("endsAt").getType()).isEqualTo(Instant.class);
    }

    @Test
    void actualMapperAndJavaFacadeCarrierUseTheCanonicalInstant() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withPropertyValues("spring.jackson.serialization.write-dates-as-timestamps=false")
                .run(context -> {
                    ObjectMapper json = context.getBean(ObjectMapper.class);
                    var request = assertDoesNotThrow(() -> json.readValue("""
                            {"startsAt":"2026-10-13T14:07:08.123456789+08:00","endsAt":"2026-10-13T07:07:08.123456789Z"}
                            """, top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.dto.ScheduleCourseDTO.class));
                    assertThat((Object) request.startsAt()).isEqualTo(Instant.parse("2026-10-13T06:07:08.123456789Z"));
                    ScheduleCourseDTO dto = new ScheduleCourseDTO(1003L, 1002L, request.startsAt(), request.endsAt(), "operator", "request");
                    ScheduleCourseCommand command = new ScheduleCourseCommand(dto.schoolClassId(), dto.courseId(), dto.startsAt(),
                            dto.endsAt(), dto.operatorId(), dto.requestId());
                    var window = new TeachingApplicationConvertorImpl().toSchedule(command,
                            new Course(1002L, new CourseCode("math"), "Mathematics", CourseStatus.ACTIVE));
                    assertThat((Object) command.startsAt()).isEqualTo(Instant.parse("2026-10-13T06:07:08.123456Z"));
                    assertThat(window.startsAt()).isEqualTo(command.startsAt());
                    assertThat(ClassCourseSchedulePO.builder().schoolClassId(1003L).courseId(1002L)
                            .startsAt(window.startsAt()).endsAt(window.endsAt()).build().getStartsAt()).isEqualTo(command.startsAt());
                    assertThat(json.writeValueAsString(window)).contains("2026-10-13T06:07:08.123456Z");
                });
    }
}
