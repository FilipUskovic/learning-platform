package com.micro.learningplatform.repositories;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseCriteria {
    private String status;
    private String category;

    // dodao pomocne metode ako ce mi zatrebati

    public boolean hasStatus() {
        return status != null && !status.isEmpty();
    }


    public boolean hasCategory() {
        return category != null && !category.isEmpty();
    }
}
