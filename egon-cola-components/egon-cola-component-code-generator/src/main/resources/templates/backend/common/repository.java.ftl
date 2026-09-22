package [=repoPackage];

import [=poFqn];
import [=daoFqn];
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;

@Slf4j
@Validated
@Repository("[=repoBean]")
@RequiredArgsConstructor
public class [=repoType] extends EgonColaRepository<[=daoType], [=poType]> {

    @Getter
    @Qualifier("[=daoBean]")
    private final [=daoType] baseMapper;

    @Getter(AccessLevel.PROTECTED)
    @Qualifier("[=propertiesBean]")
    private final EgonColaMybatisPlusProperties properties;
}
