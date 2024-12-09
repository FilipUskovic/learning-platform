package com.micro.learningplatform.repositories;

import com.micro.learningplatform.models.CourseStatisticHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CourseStatisticsHistoryRepository extends JpaRepository<CourseStatisticHistory, UUID> {

    List<CourseStatisticHistory> findByCourseIdOrderBySnapshotTimestampDesc(UUID courseId);


    @Query("Select csh from CourseStatisticHistory csh where csh.course.Id =:courseId " +
            "AND csh.snapshotTimestamp BETWEEN :startDate and :endDate order by csh.snapshotTimestamp desc")
    List<CourseStatisticHistory> findByDateRange(
            @Param("courseId") UUID courseId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );



}
