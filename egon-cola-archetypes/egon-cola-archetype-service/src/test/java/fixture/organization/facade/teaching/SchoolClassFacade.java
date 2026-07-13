package fixture.organization.facade.teaching;

import fixture.organization.facade.dto.teaching.SchoolClassDetailDTO;

public interface SchoolClassFacade {

    SchoolClassDetailDTO getSchoolClass(String schoolClassId);
}
