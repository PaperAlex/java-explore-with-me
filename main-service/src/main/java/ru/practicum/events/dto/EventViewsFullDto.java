package ru.practicum.events.dto;

import lombok.*;
import ru.practicum.categories.dto.CategoryDto;
import ru.practicum.events.enums.State;
import ru.practicum.locations.dto.LocationDto;
import ru.practicum.users.dto.UserShortDto;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventViewsFullDto {
    private Long id;

    private String title;

    private String annotation;

    private CategoryDto category;

    private boolean paid;

    private LocalDateTime eventDate;

    private UserShortDto initiator;

    private Long views;

    private Long confirmedRequests;

    private String description;

    private Integer participantLimit;

    private State state;

    private LocalDateTime createdOn;

    private LocalDateTime publishedOn;

    private LocationDto location;

    private boolean requestModeration;

}
