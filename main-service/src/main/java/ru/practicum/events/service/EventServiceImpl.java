package ru.practicum.events.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.categories.model.Category;
import ru.practicum.categories.mapper.CategoryMapper;
import ru.practicum.categories.repository.CategoryRepository;
import ru.practicum.categories.service.CategoryServiceImpl;
import ru.practicum.events.dto.*;
import ru.practicum.events.enums.State;
import ru.practicum.events.enums.StateActionAdmin;
import ru.practicum.events.enums.StateActionPrivate;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.exceptions.BadRequestException;
import ru.practicum.exceptions.ForbiddenException;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.locations.model.Location;
import ru.practicum.locations.mapper.LocationMapper;
import ru.practicum.locations.repository.LocationRepository;
import ru.practicum.requests.repository.RequestRepository;
import ru.practicum.requests.dto.ConfirmedRequests;
import ru.practicum.stats.StatsClient;
import ru.practicum.ViewStatsDto;
import ru.practicum.EndpointHitDto;
import ru.practicum.users.model.User;
import ru.practicum.users.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.practicum.events.enums.State.PENDING;
import static ru.practicum.events.enums.State.PUBLISHED;
import static ru.practicum.events.enums.StateActionAdmin.PUBLISH_EVENT;
import static ru.practicum.events.enums.StateActionAdmin.REJECT_EVENT;
import static ru.practicum.events.enums.StateActionPrivate.CANCEL_REVIEW;
import static ru.practicum.events.enums.StateActionPrivate.SEND_TO_REVIEW;
import static ru.practicum.requests.enums.RequestStatus.CONFIRMED;


@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryServiceImpl categoryService;
    private final LocationRepository locationRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;
    @Value("${app}")
    String app;

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, EventNewDto eventNewDto) throws NotFoundException {
        validateEventTime(eventNewDto.getEventDate());
        User user = userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User with id=" + userId + " was not found"));
        Long catId = eventNewDto.getCategory();
        Category category = categoryRepository.findById(catId).orElseThrow(() ->
                new NotFoundException("Category with id=" + catId + " was not found"));
        Location location = validateLocation(LocationMapper.toLocation(eventNewDto.getLocation()));
        Event event = EventMapper.toEvent(eventNewDto);
        event.setInitiator(user);
        event.setCategory(category);
        event.setLocation(location);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(PENDING);
        log.info("Запрос POST / /users/{userId}/events /,addEvent добавление пользователем {}" +
                " нового события {}", userId, eventNewDto);
        return EventMapper.toEventFullDto(eventRepository.save(event), 0L);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByOwner(Long userId, Long eventId, EventUpdateUserDto updateEvent)
            throws NotFoundException {
        getEvent(eventId);
        Optional<Event> event = eventRepository.findByIdAndInitiatorId(eventId, userId);
        if (event.get().getState() == PUBLISHED) {
            throw new ForbiddenException("Cannot update the event because it's not in the right state: PUBLISHED");
        }
        String annotation = updateEvent.getAnnotation();
        if (annotation != null && !annotation.isBlank()) {
            event.get().setAnnotation(annotation);
        }
        if (updateEvent.getCategory() != null) {
            event.get().setCategory(CategoryMapper.toCategory(categoryService.getCategoryById(updateEvent.getCategory())));
        }
        String description = updateEvent.getDescription();
        if (description != null && !description.isBlank()) {
            event.get().setDescription(description);
        }
        LocalDateTime eventDate = updateEvent.getEventDate();
        if (eventDate != null) {
            validateEventTime(eventDate);
            event.get().setEventDate(eventDate);
        }
        if (updateEvent.getLocation() != null) {
            Location location = validateLocation(LocationMapper.toLocation(updateEvent.getLocation()));
            event.get().setLocation(location);
        }
        if (updateEvent.getPaid() != null) {
            event.get().setPaid(updateEvent.getPaid());
        }
        if (updateEvent.getParticipantLimit() != null) {
            event.get().setParticipantLimit(updateEvent.getParticipantLimit());
        }
        if (updateEvent.getRequestModeration() != null) {
            event.get().setRequestModeration(updateEvent.getRequestModeration());
        }
        String title = updateEvent.getTitle();
        if (title != null && !title.isBlank()) {
            event.get().setTitle(title);
        }
        if (updateEvent.getStateAction() != null) {
            StateActionPrivate stateActionPrivate = StateActionPrivate.valueOf(updateEvent.getStateAction());
            if (stateActionPrivate.equals(SEND_TO_REVIEW)) {
                event.get().setState(PENDING);
            } else if (stateActionPrivate.equals(CANCEL_REVIEW)) {
                event.get().setState(State.CANCELED);
            }
        }
        log.info("Запрос PATCH / /users/{userId}/events/{eventId} /, updateEventByOwner изменение события {} " +
                "добавленного текущим пользователем {}", event, userId);
        return EventMapper.toEventFullDto(eventRepository.save(event.get()),
                requestRepository.countByEventIdAndStatus(eventId, CONFIRMED));
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, EventUpdateAdminDto updateEvent) throws NotFoundException {
        Event event = getEvent(eventId);
        if (updateEvent.getStateAction() != null) {
            StateActionAdmin stateAction = StateActionAdmin.valueOf(updateEvent.getStateAction());
            if (!event.getState().equals(PENDING) && stateAction.equals(PUBLISH_EVENT)) {
                throw new ForbiddenException("Cannot publish the event because it's not in the right state: not PENDING");
            }
            if (event.getState().equals(PUBLISHED) && stateAction.equals(REJECT_EVENT)) {
                throw new ForbiddenException("Cannot reject the event because it's not in the right state: PUBLISHED");
            }
            if (stateAction.equals(PUBLISH_EVENT)) {
                event.setState(PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (stateAction.equals(REJECT_EVENT)) {
                event.setState(State.CANCELED);
            }
        }
        String annotation = updateEvent.getAnnotation();
        if (annotation != null && !annotation.isBlank()) {
            event.setAnnotation(annotation);
        }
        if (updateEvent.getCategory() != null) {
            event.setCategory(CategoryMapper.toCategory(categoryService.getCategoryById(updateEvent.getCategory())));
        }
        String description = updateEvent.getDescription();
        if (description != null && !description.isBlank()) {
            event.setDescription(description);
        }
        LocalDateTime eventDate = updateEvent.getEventDate();
        if (eventDate != null) {
            validateEventTime(eventDate);
            event.setEventDate(eventDate);
        }
        if (updateEvent.getLocation() != null) {
            event.setLocation(validateLocation(LocationMapper.toLocation(updateEvent.getLocation())));
        }
        if (updateEvent.getPaid() != null) {
            event.setPaid(updateEvent.getPaid());
        }
        if (updateEvent.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEvent.getParticipantLimit());
        }
        if (updateEvent.getRequestModeration() != null) {
            event.setRequestModeration(updateEvent.getRequestModeration());
        }
        String title = updateEvent.getTitle();
        if (title != null && !title.isBlank()) {
            event.setTitle(title);
        }
        log.info("PATCH / /admin/events/{eventId} /, updateEventByAdmin обновление данных события {}", event);
        return EventMapper.toEventFullDto(eventRepository.save(event),
                requestRepository.countByEventIdAndStatus(eventId, CONFIRMED));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsByOwner(Long userId, Integer from, Integer size) {
        List<Event> events = eventRepository.findAllByInitiatorId(userId, PageRequest.of(from / size, size));
        List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Long> confirmedRequests = requestRepository.findAllByEventIdInAndStatus(ids, CONFIRMED)
                .stream()
                .collect(Collectors.toMap(ConfirmedRequests::getEvent, ConfirmedRequests::getCount));
        log.info("Запрос GET / /users/{userId}/events /, getEventsByOwner получение событий добавленных текущим" +
                " пользователем {}", userId);
        return events.stream()
                .map(event -> EventMapper.toEventShortDto(event, confirmedRequests.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventByOwner(Long userId, Long eventId) throws NotFoundException {
        getEvent(eventId);
        log.info("Запрос GET / /users/{userId}/events/{eventId} /, getEventByOwner получение информации о" +
                " событие {} добавленом текущим пользователем {}", eventId, userId);
        Optional<Event> event = eventRepository.findByIdAndInitiatorId(eventId, userId);
        return EventMapper.toEventFullDto(event.get(), requestRepository.countByEventIdAndStatus(eventId, CONFIRMED));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventViewsFullDto> getEventsByAdminParams(List<Long> users, List<String> states, List<Long> categories,
                                                          LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                          Integer from, Integer size) throws NotFoundException {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Incorrectly made request, Start is after End");
        }
        Specification<Event> specification = Specification.where(null);
        if (users != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    root.get("initiator").get("id").in(users));
        }
        if (states != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    root.get("state").as(String.class).in(states));
        }
        if (categories != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    root.get("category").get("id").in(categories));
        }
        if (rangeStart != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
        }
        if (rangeEnd != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
        }
        List<Event> events = eventRepository.findAll(specification, PageRequest.of(from / size, size)).getContent();
        List<EventViewsFullDto> result = new ArrayList<>();
        List<String> uris = events.stream()
                .map(event -> String.format("/events/%s", event.getId()))
                .collect(Collectors.toList());
        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new NotFoundException("Start not found"));
        ResponseEntity<Object> response = statsClient.getStats(start, LocalDateTime.now(), uris, true);
        List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Long> confirmedRequests = requestRepository.findAllByEventIdInAndStatus(ids, CONFIRMED).stream()
                .collect(Collectors.toMap(ConfirmedRequests::getEvent, ConfirmedRequests::getCount));
        for (Event event : events) {
            ObjectMapper mapper = new ObjectMapper();
            List<ViewStatsDto> statsDto = mapper.convertValue(response.getBody(), new TypeReference<>() {
            });
            if (!statsDto.isEmpty()) {
                result.add(EventMapper.toEventFullDtoWithViews(event, statsDto.getFirst().getHits(),
                        confirmedRequests.getOrDefault(event.getId(), 0L)));
            } else {
                result.add(EventMapper.toEventFullDtoWithViews(event, 0L,
                        confirmedRequests.getOrDefault(event.getId(), 0L)));
            }
        }
        log.info("Запрос GET / /admin/events /, getEventsByAdminParams с параметрами");
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventViewsShortDto> getEvents(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart,
                                              LocalDateTime rangeEnd, Boolean onlyAvailable, String sort, Integer from,
                                              Integer size, HttpServletRequest request) throws NotFoundException {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Incorrectly made request, Start is after End");
        }
        Specification<Event> specification = Specification.where(null);
        if (text != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.or(
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("annotation")), "%" + text.toLowerCase() + "%"),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + text.toLowerCase() + "%")
                    ));
        }
        if (categories != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    root.get("category").get("id").in(categories));
        }
        if (paid != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("paid"), paid));
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime = Objects.requireNonNullElseGet(rangeStart, () -> now);
        specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThan(root.get("eventDate"), startDateTime));
        if (rangeEnd != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThan(root.get("eventDate"), rangeEnd));
        }
        if (onlyAvailable != null && onlyAvailable) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("participantLimit"), 0));
        }
        specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("state"), PUBLISHED));
        PageRequest pageRequest;
        if (sort.equals("EVENT_DATE")) {
            pageRequest = PageRequest.of(from / size, size, Sort.by("eventDate"));
        } else if (sort.equals("VIEWS")) {
            pageRequest = PageRequest.of(from / size, size, Sort.by("views").descending());
        } else {
            throw new ValidationException("Unknown sort: " + sort);
        }
        List<Event> events = eventRepository.findAll(specification, pageRequest).getContent();
        List<EventViewsShortDto> result = new ArrayList<>();
        List<String> uris = events.stream()
                .map(event -> String.format("/events/%s", event.getId()))
                .collect(Collectors.toList());
        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new NotFoundException("Start not found"));
        ResponseEntity<Object> response = statsClient.getStats(start, LocalDateTime.now(), uris, true);
        System.out.println(response.getBody());
        List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Long> confirmedRequests = requestRepository.findAllByEventIdInAndStatus(ids, CONFIRMED)
                .stream()
                .collect(Collectors.toMap(ConfirmedRequests::getEvent, ConfirmedRequests::getCount));
        for (Event event : events) {
            ObjectMapper mapper = new ObjectMapper();
            List<ViewStatsDto> statsDto = mapper.convertValue(response.getBody(), new TypeReference<>() {
            });
            if (!statsDto.isEmpty()) {
                result.add(EventMapper.toEventShortDtoWithViews(event, statsDto.getFirst().getHits(),
                        confirmedRequests.getOrDefault(event.getId(), 0L)));
            } else {
                result.add(EventMapper.toEventShortDtoWithViews(event, 0L,
                        confirmedRequests.getOrDefault(event.getId(), 0L)));
            }
        }
        EndpointHitDto hit = new EndpointHitDto(app, request.getRequestURI(), request.getRemoteAddr(),
                LocalDateTime.now());
        statsClient.save(hit);
        log.info("Запрос GET / /events /, getEvents получение событий с возможностью фильтрации");
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public EventViewsFullDto getEventById(Long eventId, HttpServletRequest request) throws NotFoundException {
        Event event = getEvent(eventId);
        if (event.getState() != PUBLISHED) {
            throw new NotFoundException("Event is not PUBLISHED");
        }
        ResponseEntity<Object> response = statsClient.getStats(event.getCreatedOn(), LocalDateTime.now(),
                List.of(request.getRequestURI()), true);
        ObjectMapper mapper = new ObjectMapper();
        List<ViewStatsDto> statsDto = mapper.convertValue(response.getBody(), new TypeReference<>() {
        });
        EventViewsFullDto result;
        if (!statsDto.isEmpty()) {
            result = EventMapper.toEventFullDtoWithViews(event, statsDto.getFirst().getHits(),
                    requestRepository.countByEventIdAndStatus(eventId, CONFIRMED));
        } else {
            result = EventMapper.toEventFullDtoWithViews(event, 0L,
                    requestRepository.countByEventIdAndStatus(eventId, CONFIRMED));
        }
        EndpointHitDto hit = new EndpointHitDto(app, request.getRequestURI(), request.getRemoteAddr(),
                LocalDateTime.now());
        statsClient.save(hit);
        log.info("Запрос GET / /events/{eventId} /, getEventById получам событие id: {} ", eventId);
        return result;
    }

    private void validateEventTime(LocalDateTime eventTime) {
        if (eventTime.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Incorrectly made request, event time");
        }
    }

    private Location validateLocation(Location location) {
        if (locationRepository.existsByLatAndLon(location.getLat(), location.getLon())) {
            return locationRepository.findByLatAndLon(location.getLat(), location.getLon());
        } else {
            return locationRepository.save(location);
        }
    }

    private Event getEvent(Long eventId) throws NotFoundException {
        return eventRepository.findById(eventId).orElseThrow(() ->
                new NotFoundException("Event with id=" + eventId + " was not found"));
    }

}