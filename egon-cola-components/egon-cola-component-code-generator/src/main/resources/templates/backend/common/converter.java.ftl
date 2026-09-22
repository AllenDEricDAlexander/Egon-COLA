[#if converterKind == "persistence"]
package [=domainImplPackage];

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import [=poFqn];
import [=domainFqn];
import top.egon.cola.component.common.core.converter.BaseConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, builder = @Builder(disableBuilder = true))
public interface [=persistenceConverterType] extends BaseConverter<[=poType], [=domainType]> {

    @Override
    [=domainType] toTarget([=poType] source);

[#list ignoredPoFields as ignored]
    @Mapping(target = "[=ignored]", ignore = true)
[/#list]
    @Override
    [=poType] toSource([=domainType] target);
}
[#elseif converterKind == "result"]
package [=applicationConverterPackage];

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import [=domainFqn];
import [=applicationPackage].[=resultType];
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface [=resultConverterType] extends BaseForwardConverter<[=domainType], [=resultType]> {

    @Override
    @Mapping(target = "id", expression = "java(source.getId() == null ? null : Long.toString(source.getId()))")
    [=resultType] toTarget([=domainType] source);
}
[#elseif converterKind == "create"]
package [=applicationConverterPackage];

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import [=applicationPackage].[=createCommandType];
import [=domainFqn];
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface [=createConverterType] extends BaseForwardConverter<[=createCommandType], [=domainType]> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    [=domainType] toTarget([=createCommandType] source);
}
[#elseif converterKind == "update"]
package [=applicationConverterPackage];

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import [=applicationPackage].[=updateCommandType];
import [=domainFqn];
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface [=updateConverterType] extends BaseForwardConverter<[=updateCommandType], [=domainType]> {

    @Override
    @Mapping(target = "version", source = "expectedVersion")
    [=domainType] toTarget([=updateCommandType] source);
}
[#else]
package [=applicationConverterPackage];

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import [=applicationPackage].[=deleteCommandType];
import [=domainFqn];
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface [=deleteConverterType] extends BaseForwardConverter<[=deleteCommandType], [=domainType]> {

    @Override
    @Mapping(target = "version", source = "expectedVersion")
[#list domainFields as field]
    @Mapping(target = "[=field.javaName]", ignore = true)
[/#list]
    [=domainType] toTarget([=deleteCommandType] source);
}
[/#if]
