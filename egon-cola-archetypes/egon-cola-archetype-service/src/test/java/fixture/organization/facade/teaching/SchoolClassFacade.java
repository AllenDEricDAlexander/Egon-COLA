package fixture.organization.facade.teaching;

import fixture.organization.facade.teaching.dto.SchoolClassDetailDTO;

public interface SchoolClassFacade {

    SchoolClassDetailDTO getSchoolClass(String schoolClassId);
}
