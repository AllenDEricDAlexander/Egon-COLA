package top.egon.cola.archetype.source.light.adapter.pojo.convertor;

import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.convertor.TeachingApplicationConvertorImpl;
import top.egon.cola.archetype.source.light.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.light.domain.teaching.enums.CourseStatus;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.light.facade.proto.ScheduleCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.teaching.dto.ScheduleCourseDTO;
import top.egon.cola.archetype.source.light.infrastructure.teaching.po.ClassCourseSchedulePO;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleUtcConverterTest {

    private final LightFacadeConverter mapper = new LightFacadeConverterImpl();

    @Test
    void allWindowCarriersDeclareInstant() throws Exception {
        for (Class<?> type : new Class<?>[]{top.egon.cola.archetype.source.light.adapter.teaching.pojo.dto.ScheduleCourseDTO.class,
                ScheduleCourseDTO.class, ScheduleCourseCommand.class}) {
            assertThat(type.getDeclaredField("startsAt").getType()).isEqualTo(Instant.class);
            assertThat(type.getDeclaredField("endsAt").getType()).isEqualTo(Instant.class);
        }
        assertThat(ClassCourseSchedulePO.class.getDeclaredField("startsAt").getType()).isEqualTo(Instant.class);
        assertThat(ClassCourseSchedulePO.class.getDeclaredField("endsAt").getType()).isEqualTo(Instant.class);
    }

    @Test
    void actualProtoAndMapStructConversionsCanonicalizeTheWindow() {
        ScheduleCourseRpcRequest input = ScheduleCourseRpcRequest.newBuilder().setSchoolClassId(1003L).setCourseId(1002L)
                .setStartsAt("2026-10-13T14:07:08.123456789+08:00")
                .setEndsAt("2026-10-13T07:07:08.123456789Z").setOperatorId("operator").setRequestId("request").build();
        assertThatCode(() -> mapper.toSource(input)).doesNotThrowAnyException();
        ScheduleCourseDTO dto = mapper.toSource(input);
        assertThat(dto.startsAt()).isEqualTo(Instant.parse("2026-10-13T06:07:08.123456789Z"));
        assertThat(mapper.toTarget(dto).getStartsAt()).isEqualTo("2026-10-13T06:07:08.123456789Z");
        ScheduleCourseCommand command = new ScheduleCourseCommand(dto.schoolClassId(), dto.courseId(), dto.startsAt(),
                dto.endsAt(), dto.operatorId(), dto.requestId());
        var window = new TeachingApplicationConvertorImpl().toSchedule(command,
                new Course(1002L, new CourseCode("math"), "Mathematics", CourseStatus.ACTIVE));
        assertThat(command.startsAt()).isEqualTo(Instant.parse("2026-10-13T06:07:08.123456Z"));
        assertThat(window.startsAt()).isEqualTo(command.startsAt());
        assertThat(window.endsAt()).isEqualTo(command.endsAt());
        assertThat(ClassCourseSchedulePO.builder().schoolClassId(1003L).courseId(1002L)
                .startsAt(window.startsAt()).endsAt(window.endsAt()).build().getStartsAt()).isEqualTo(command.startsAt());
    }

    @Test
    void protoNumbersTypesAndAbsenceStayUnchanged() {
        var descriptor = ScheduleCourseRpcRequest.getDescriptor();
        assertThat(descriptor.findFieldByName("starts_at").getNumber()).isEqualTo(3);
        assertThat(descriptor.findFieldByName("ends_at").getNumber()).isEqualTo(4);
        assertThat(descriptor.findFieldByName("starts_at").getType().name()).isEqualTo("STRING");
        assertThat(descriptor.findFieldByName("ends_at").hasPresence()).isTrue();
        assertThat(mapper.toSource(ScheduleCourseRpcRequest.getDefaultInstance()).startsAt()).isNull();
        assertThatThrownBy(() -> mapper.fromProtoTime("2026-10-13T14:07:08"))
                .isInstanceOf(DateTimeParseException.class);
    }
}
