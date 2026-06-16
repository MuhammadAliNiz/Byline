package me.alinizamani.byline.repository;

import me.alinizamani.byline.domain.engagement.UserFollow;
import me.alinizamani.byline.domain.engagement.UserFollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, UserFollowId> {

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    @Query("SELECT uf FROM UserFollow uf JOIN FETCH uf.follower WHERE uf.following.id = :userId")
    List<UserFollow> findFollowersByUserId(@Param("userId") UUID userId);

    @Query("SELECT uf FROM UserFollow uf JOIN FETCH uf.following WHERE uf.follower.id = :userId")
    List<UserFollow> findFollowingByUserId(@Param("userId") UUID userId);
}