package top.egon.fable.web.adapter.convertor;

import top.egon.fable.web.domain.common.Page;
import top.egon.fable.web.domain.entities.user.User;
import top.egon.fable.web.facade.dto.PageResponse;
import top.egon.fable.web.facade.dto.user.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("userAdapterConverter")
@RequiredArgsConstructor
public class UserAdapterConverter {
    @Qualifier("userAdapterMapperImpl")
    private final UserAdapterMapper userAdapterMapper;

    public UserDTO toDto(User user) {
        UserDTO dto = userAdapterMapper.convert(user);
        dto.setStatus(user.getStatus().name());
        return dto;
    }

    public PageResponse<UserDTO> toPageResponse(Page<User> page) {
        return PageResponse.of(
                page.records().stream().map(this::toDto).toList(),
                page.currentPage(),
                page.totalPages(),
                page.pageSize(),
                page.totalCount());
    }
}
