package top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo;

import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.dao.ClassCourseScheduleDAO;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.dao.CourseDAO;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.dao.SchoolClassDAO;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.po.ClassCourseSchedulePO;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.po.CoursePO;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.po.SchoolClassPO;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import static org.assertj.core.api.Assertions.assertThat;

class SchoolClassRepositoryImplTest {

    @Test
    void teaching_persistence_contract_uses_common_mapper_and_model() {
        assertThat(EgonColaMapper.class).isAssignableFrom(CourseDAO.class);
        assertThat(EgonColaMapper.class).isAssignableFrom(SchoolClassDAO.class);
        assertThat(EgonColaMapper.class).isAssignableFrom(ClassCourseScheduleDAO.class);
        assertThat(EgonModel.class).isAssignableFrom(CoursePO.class);
        assertThat(EgonModel.class).isAssignableFrom(SchoolClassPO.class);
        assertThat(EgonModel.class).isAssignableFrom(ClassCourseSchedulePO.class);
        assertThat(SchoolClassPO.class.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class)
                .value()).isEqualTo("light_school_classes");
    }
}
