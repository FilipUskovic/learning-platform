package com.micro.learningplatform.services;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseModule;
import com.micro.learningplatform.models.CourseStatus;
import com.micro.learningplatform.repositories.CourseRepository;
import com.micro.learningplatform.shared.exceptions.CourseNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseModuleManager {

    private final CourseRepository courseRepository;

    /* Omogucuje promjenu redosljeta modula untar tecaja
      -> korsitimo streamapi za mapiranje novih pozicija i imam validaciju prije pormjena redosljeda


     */
/*
    public void renderModules(UUID courseId, List<UUID> newOrder) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        validateReorderRequest(course, newOrder);
        Map<UUID, Integer> newPositions = createNewPositionMap(newOrder);
        
        updateModulePositions(course.getModules(), newPositions);

        courseRepository.save(course);
        
    }

    private void updateModulePositions(List<CourseModule> modules, Map<UUID, Integer> newPositions) {
       modules.forEach(module -> {
           module.setSequenceNumber(newPositions.get(module.getSequenceNumber()));
       });
    }

 */

    private Map<UUID, Integer> createNewPositionMap(List<UUID> newOrder) {
        return IntStream.range(0, newOrder.size())
                .boxed().collect(Collectors.toMap(
                        newOrder::get,
                        i -> i + 1
                ));
    }

    private void validateReorderRequest(Course course, List<UUID> newOrder) {
        if (course.getCourseStatus() != CourseStatus.DRAFT) {
            throw new IllegalStateException(
                    "Can only reorder modules for courses in DRAFT status");
        }

        if (newOrder.size() != course.getModules().size()) {
            throw new IllegalArgumentException(
                    "New order must contain all existing modules");
        }

        Set<UUID> existingIds = course.getModules().stream()
                .map(CourseModule::getId)
                .collect(Collectors.toSet());

        if (!new HashSet<>(newOrder).equals(existingIds)) {
            throw new IllegalArgumentException(
                    "New order must contain exactly the same modules");
        }
    }
}
