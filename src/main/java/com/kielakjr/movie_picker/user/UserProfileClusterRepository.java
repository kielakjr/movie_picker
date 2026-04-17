package com.kielakjr.movie_picker.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserProfileClusterRepository extends JpaRepository<UserProfileCluster, Long> {

    List<UserProfileCluster> findByUserId(Long userId);

    List<UserProfileCluster> findByUserIdOrderById(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserProfileCluster c WHERE c.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
