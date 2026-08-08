package top.egon.cola.component.ddc.admin.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DdcCacheCheckRow {

    private String resourceName;

    private String databaseValue;

    private String redisValue;

    private Long databaseVersion;

    private Long redisVersion;

    private boolean matched;
}
