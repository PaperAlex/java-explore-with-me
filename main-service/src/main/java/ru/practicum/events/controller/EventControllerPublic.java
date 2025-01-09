package ru.practicum.events.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.events.service.EventService;
import ru.practicum.events.dto.EventViewsFullDto;
import ru.practicum.events.dto.EventViewsShortDto;
import ru.practicum.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventControllerPublic {
    private final EventService eventService;

    @GetMapping
    public List<EventViewsShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false)
            LocalDateTime rangeStart,
            @RequestParam(required = false)
            LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(defaultValue = "EVENT_DATE") String sort,
            @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero
            Integer from,
            @RequestParam(value = "size", defaultValue = "10") @Positive
            Integer size,
            HttpServletRequest request) throws NotFoundException {
        return eventService.getEvents(text, categories, paid, rangeStart, rangeEnd, onlyAvailable,
                sort, from, size, request);
    }

    @GetMapping("/{eventId}")
    public EventViewsFullDto getEventById(@PathVariable Long eventId, HttpServletRequest request) throws NotFoundException {
        return eventService.getEventById(eventId, request);
    }
}
