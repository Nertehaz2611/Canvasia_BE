package com.example.canvasia.repository;

import com.example.canvasia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    @Query("""
            select u
            from User u
            where lower(u.username) like lower(concat('%', :query, '%'))
               or lower(coalesce(u.displayName, '')) like lower(concat('%', :query, '%'))
            order by
                case
                    when lower(u.username) = lower(:query) then 0
                    when lower(u.displayName) = lower(:query) then 1
                    else 2
                end,
                lower(coalesce(u.displayName, u.username)),
                lower(u.username)
            """)
    List<User> searchByDisplayNameOrUsername(@Param("query") String query, Pageable pageable);
}
