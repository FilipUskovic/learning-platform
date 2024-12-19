package com.micro.learningplatform.models.dto.courses;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.micro.learningplatform.models.CourseStatus;
import com.micro.learningplatform.models.EntityCategory;
import com.micro.learningplatform.models.dto.DifficultyLevel;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseResponse(
        /** Pokusao sam razmisliti dali zelim String korsisti umjesto UUID-a
         * String je zauzima manje memoriski prostor, ali s druge strane moramo potrebna konverzija u uuid i dodatna validacija
         * Pošto je nama bitna konzistetnos podatka jer cemo kasnije prijeci na microsrvis arch
         *  -> i app vec ima snazan kopleksni domensku logiku i kompleksnu event strukutur
         *   Zadrzavamo UUUID I enum umjesto stringa jer :
         *   1- UUID -> zbog tipske sigurnosti i i konzistetnosti s domenom
         *   2. korisitmo enum jer je dio domenske logike i ima validaciju i tipsku sigurnost
         *
         */
        UUID id,
        String title,
        String description,
        CourseStatus status,
        EntityCategory category,  // Zadržavamo enum za validaciju i tipsku sigurnost
        DifficultyLevel difficultyLevel,
        CourseStatisticsDTO statistics,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime updatedAt

){

}


