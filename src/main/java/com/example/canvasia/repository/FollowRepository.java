package com.example.canvasia.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.canvasia.entity.Follow;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
    Optional<Follow> findByFollowerUsernameAndFollowingUsername(String followerUsername, String followingUsername);

    boolean existsByFollowerUsernameAndFollowingUsername(String followerUsername, String followingUsername);

    long countByFollowerUsername(String followerUsername);

    long countByFollowingUsername(String followingUsername);

    Page<Follow> findByFollowingUsernameOrderByCreatedAtDesc(String followingUsername, Pageable pageable);

    Page<Follow> findByFollowerUsernameOrderByCreatedAtDesc(String followerUsername, Pageable pageable);

        @Query("""
                        select f
                        from Follow f
                        where f.following.username = :username
                            and (
                                lower(f.follower.username) like lower(concat('%', :query, '%'))
                                or lower(coalesce(f.follower.displayName, '')) like lower(concat('%', :query, '%'))
                            )
                        order by
                                case
                                        when lower(f.follower.username) = lower(:query) then 0
                                        when lower(f.follower.displayName) = lower(:query) then 1
                                        else 2
                                end,
                                lower(coalesce(f.follower.displayName, f.follower.username)),
                                lower(f.follower.username)
                        """)
        Page<Follow> searchFollowersByFollowingUsername(
                        @Param("username") String username,
                        @Param("query") String query,
                        Pageable pageable
        );

        @Query("""
                select f.follower.id
                from Follow f
                where f.following.id = :followingUserId
                    and f.follower.id in :candidateUserIds
                """)
        List<UUID> findFollowerIdsByFollowingIdAndFollowerIdsIn(
                        @Param("followingUserId") UUID followingUserId,
                        @Param("candidateUserIds") List<UUID> candidateUserIds
        );
}
