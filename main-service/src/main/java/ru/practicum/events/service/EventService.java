package ru.practicum.events.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.events.dto.*;
import ru.practicum.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    EventFullDto addEvent(Long userId, EventNewDto eventNewDto) throws NotFoundException;

    EventFullDto updateEventByOwner(Long userId, Long eventId, EventUpdateUserDto updateEvent) throws NotFoundException;

    EventFullDto updateEventByAdmin(Long eventId, EventUpdateAdminDto updateEvent) throws NotFoundException;

    List<EventShortDto> getEventsByOwner(Long userId, Integer from, Integer size);

    EventFullDto getEventByOwner(Long userId, Long eventId) throws NotFoundException;

    List<EventViewsFullDto> getEventsByAdminParams(List<Long> users, List<String> states, List<Long> categories,
                                                   LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                   Integer from, Integer size) throws NotFoundException;

    List<EventViewsShortDto> getEvents(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart,
                                       LocalDateTime rangeEnd, Boolean onlyAvailable, String sort, Integer from,
                                       Integer size, HttpServletRequest request) throws NotFoundException;


    EventViewsFullDto getEventById(Long eventId, HttpServletRequest request) throws NotFoundException;
}
