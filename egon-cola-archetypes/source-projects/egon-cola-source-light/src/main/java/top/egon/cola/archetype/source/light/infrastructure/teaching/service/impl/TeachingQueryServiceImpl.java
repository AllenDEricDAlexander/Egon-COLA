package top.egon.cola.archetype.source.light.infrastructure.teaching.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.light.domain.teaching.service.TeachingQueryService;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.light.domain.teaching.vos.ExternalCourse;
import top.egon.cola.archetype.source.light.infrastructure.teaching.client.TeachingQueryClient;

import java.util.Optional;

/** Teaching query capability; transport is delegated to the bound client implementation. */
@Validated
@Service("teachingQueryService")
@RequiredArgsConstructor
@Slf4j
public class TeachingQueryServiceImpl implements TeachingQueryService {
    @Qualifier("teachingQueryClient")
    private final TeachingQueryClient teachingQueryClient;

    @Override
    public Optional<ExternalCourse> findExternalCourse(CourseCode code) {
        return teachingQueryClient.findExternalCourse(code);
    }
}
