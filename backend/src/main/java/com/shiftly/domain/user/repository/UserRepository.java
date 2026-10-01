package com.shiftly.domain.user.repository;

import com.shiftly.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /** /auth/me 처럼 조직 정보까지 한 번에 필요할 때 */
    @EntityGraph(attributePaths = "organization")
    Optional<User> findWithOrganizationById(Long id);
}
