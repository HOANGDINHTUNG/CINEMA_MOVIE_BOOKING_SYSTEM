package com.re.cinemamoviebookingsystem.repository;

import com.re.cinemamoviebookingsystem.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("""
            SELECT u FROM User u
            JOIN FETCH u.role
            LEFT JOIN FETCH u.profile
            WHERE (:roleName IS NULL OR u.role.roleName = :roleName)
            AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY u.createdAt DESC
            """)
    Page<User> findForAdmin(@Param("roleName") String roleName,
                            @Param("keyword") String keyword,
                            Pageable pageable);

    @Query("""
            SELECT u FROM User u
            JOIN FETCH u.role
            LEFT JOIN FETCH u.profile
            WHERE u.userId = :userId
            """)
    Optional<User> findByIdWithDetails(@Param("userId") Long userId);
}
