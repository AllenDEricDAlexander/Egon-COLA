package ${package}.infrastructure.client.impl.user;

import ${package}.domain.client.user.UserClient;
import ${package}.domain.common.Page;
import ${package}.domain.entities.user.User;
import ${package}.domain.repos.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Component("userClientImpl")
@Validated
@RequiredArgsConstructor
public class UserClientImpl implements UserClient {
    @Qualifier("userRepositoryImpl")
    private final UserRepository userRepository;

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    @Override
    public Page<User> findPage(int currentPage, int pageSize) {
        return userRepository.findPage(currentPage, pageSize);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
