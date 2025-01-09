package ru.practicum.events.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import ru.practicum.locations.dto.LocationDto;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventUpdateAdminDto {

    @Size(min = 20, max = 2000)
    private String annotation;

    Long category;

    @Size(min = 20, max = 7000)
    private String description;

    @Future
    private LocalDateTime eventDate;

    private LocationDto location;

    private Boolean paid;

    @PositiveOrZero
    private Integer participantLimit;

    private Boolean requestModeration;

    private String stateAction;

    @Size(min = 3, max = 120)
    private String title;
}
