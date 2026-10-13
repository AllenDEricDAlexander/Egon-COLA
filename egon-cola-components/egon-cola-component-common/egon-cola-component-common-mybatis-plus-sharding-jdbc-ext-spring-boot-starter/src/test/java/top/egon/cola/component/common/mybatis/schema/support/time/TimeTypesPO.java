package top.egon.cola.component.common.mybatis.schema.support.time;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName("time_types")
public class TimeTypesPO extends EgonModel<TimeTypesPO> {

    private Instant occurredAt;
    private OffsetDateTime offsetAt;
    private LocalDateTime localAt;
    private LocalDate pureDate;
    private LocalTime localTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    @TableName("unsupported_zoned_time")
    public static class ZonedTimePO extends EgonModel<ZonedTimePO> {
        private ZonedDateTime occurredAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    @TableName("unsupported_offset_time")
    public static class OffsetTimePO extends EgonModel<OffsetTimePO> {
        private OffsetTime occurredAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    @TableName("unsupported_duration")
    public static class DurationPO extends EgonModel<DurationPO> {
        private Duration elapsed;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    @TableName("unsupported_time_array")
    public static class TimeArrayPO extends EgonModel<TimeArrayPO> {
        private Instant[][] occurredAt;
    }
}
