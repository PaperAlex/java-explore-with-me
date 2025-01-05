package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EndpointHitDto;
import ru.practicum.ViewStatsDto;
import ru.practicum.exception.BadRequestException;
import ru.practicum.mapper.EndpointHitMapper;
import ru.practicum.model.EndpointHit;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;

    @Transactional
    @Override
    public EndpointHitDto save(EndpointHitDto hitDto) {
        log.info("Сохраняем: {}", hitDto);
        EndpointHit endpointHit = statsRepository.save(EndpointHitMapper.toHitEntity(hitDto));
        return EndpointHitMapper.toEndpointHitDto(endpointHit);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        if (start.isAfter(end)) {
            throw new BadRequestException("Время указанно не верно");
        }
        if (unique) {
            if (uris != null) {
                log.info("Unique uris. Статистика уникальных обращений с {} по {}", start, end);
                return statsRepository.getHitsWithUrisWithUniqueIp(uris, start, end);
            }
            log.info("Без uris. Статистика уникальных обращений с {} по {}", start, end);
            return statsRepository.getHitsWithoutUrisWithUniqueIp(start, end);
        } else {
            if (uris != null) {
                log.info("Not unique uris. Статистика обращений с {} по {}", start, end);
                return statsRepository.getAllHitsWithUris(uris, start, end);
            }
            log.info("Без uris. Статистика обращений с {} по {}", start, end);
            return statsRepository.getAllHitsWithoutUris(start, end);
        }
    }
}