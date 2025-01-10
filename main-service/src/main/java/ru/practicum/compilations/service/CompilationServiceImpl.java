package ru.practicum.compilations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.NewCompilationDto;
import ru.practicum.compilations.dto.UpdateCompilationRequest;
import ru.practicum.compilations.mapper.CompilationMapper;
import ru.practicum.compilations.model.Compilation;
import ru.practicum.compilations.repository.CompilationRepository;
import ru.practicum.events.model.Event;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.requests.repository.RequestRepository;
import ru.practicum.requests.dto.ConfirmedRequests;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.practicum.requests.enums.RequestStatus.CONFIRMED;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;

    @Transactional(readOnly = true)
    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations;

        if (pinned != null) {
            compilations = compilationRepository.findAllByPinned(pinned, pageable);
        } else {
            compilations = compilationRepository.findAll(pageable).getContent();
        }

        List<Long> compilationIds = compilations.stream()
                .map(Compilation::getId)
                .collect(Collectors.toList());

        List<Event> events = eventRepository.findAllByInitiatorIdIn(compilationIds);

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> confirmedRequests = requestRepository.findAllByEventIdInAndStatus(eventIds, CONFIRMED)
                .stream()
                .collect(Collectors.toMap(ConfirmedRequests::getEvent, ConfirmedRequests::getCount));

        List<CompilationDto> result = compilations.stream()
                .map(compilation -> {
                    CompilationDto compilationDto = CompilationMapper.toCompilationDto(compilation);
                    List<EventShortDto> eventShortDtos = compilation.getEvents().stream()
                            .map(event -> EventMapper.toEventShortDto(event, confirmedRequests.get(event.getId())))
                            .collect(Collectors.toList());
                    compilationDto.setEvents(eventShortDtos);
                    return compilationDto;
                })
                .collect(Collectors.toList());
        log.info("getCompilations Получаем подборку событий");
        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public CompilationDto getCompilationById(Long compilationId) throws NotFoundException {
        Compilation compilation = getCompilation(compilationId);
        CompilationDto compilationDto = CompilationMapper.toCompilationDto(compilation);
        if (compilation.getEvents() != null) {
            List<Long> ids = compilation.getEvents().stream().map(Event::getId).collect(Collectors.toList());
            Map<Long, Long> confirmedRequests = requestRepository.findAllByEventIdInAndStatus(ids, CONFIRMED)
                    .stream()
                    .collect(Collectors.toMap(ConfirmedRequests::getEvent, ConfirmedRequests::getCount));
            compilationDto.setEvents(compilation.getEvents().stream()
                    .map(event -> EventMapper.toEventShortDto(event, confirmedRequests.get(event.getId())))
                    .collect(Collectors.toList()));
        }
        log.info("getCompilationById Получаем подборки событий id={}", compilationDto.getId());
        return compilationDto;
    }

    @Transactional
    @Override
    public CompilationDto addCompilation(NewCompilationDto newCompilationDto) {
        Compilation compilation = CompilationMapper.toCompilationEntity(newCompilationDto);
        setEvents(compilation, newCompilationDto.getEvents());
        log.info("addCompilation Сохраняем подборку: \"{}\"", newCompilationDto.getTitle());
        return setCompilationDto(compilation);
    }

    @Transactional
    @Override
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateCompilation) throws NotFoundException {
        Compilation compilation = getCompilation(compId);
        if (updateCompilation.getEvents() != null) {
            Set<Event> events = updateCompilation.getEvents().stream()
                    .map(id -> {
                        Event event = new Event();
                        event.setId(id);
                        return event;
                    }).collect(Collectors.toSet());
            compilation.setEvents(events);
        }
        if (updateCompilation.getPinned() != null) {
            compilation.setPinned(updateCompilation.getPinned());
        }
        String title = updateCompilation.getTitle();
        if (title != null && !title.isBlank()) {
            compilation.setTitle(title);
        }
        log.info("updateCompilation обновляем подборку: {}", updateCompilation.getTitle());
        return setCompilationDto(compilation);
    }

    @Transactional
    @Override
    public void deleteCompilation(Long compilationId) throws NotFoundException {
        getCompilation(compilationId);
        log.info("Запрос DELETE /admin/compilations/{compilationId} /, deleteCompilation удаляем подборку {}", compilationId);
        compilationRepository.deleteById(compilationId);
    }

    private void setEvents(Compilation compilation, List<Long> eventIds) {
        if (eventIds != null) {
            compilation.setEvents(eventRepository.findAllByIdIn(eventIds));
        }
    }

    private CompilationDto setCompilationDto(Compilation compilation) {
        CompilationDto compilationDto = CompilationMapper.toCompilationDto(compilationRepository.save(compilation));
        if (compilation.getEvents() != null) {
            List<Long> ids = compilation.getEvents().stream().map(Event::getId).collect(Collectors.toList());
            Map<Long, Long> confirmedRequests = requestRepository.findAllByEventIdInAndStatus(ids, CONFIRMED)
                    .stream()
                    .collect(Collectors.toMap(ConfirmedRequests::getEvent, ConfirmedRequests::getCount));
            compilationDto.setEvents(compilation.getEvents().stream()
                    .map(event -> EventMapper.toEventShortDto(event, confirmedRequests.get(event.getId())))
                    .collect(Collectors.toList()));
        }
        return compilationDto;
    }

    private Compilation getCompilation(Long compilationId) throws NotFoundException {
        return compilationRepository.findById(compilationId).orElseThrow(() ->
                new NotFoundException("Compilation id=" + compilationId + " not found"));
    }
}
