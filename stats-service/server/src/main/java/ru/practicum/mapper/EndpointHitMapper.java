package ru.practicum.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.EndpointHitDto;
import ru.practicum.model.EndpointHit;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EndpointHitMapper {
    public static EndpointHit toHitEntity(EndpointHitDto hitDto) {
        return new EndpointHit(
                hitDto.getId(),
                hitDto.getApp(),
                hitDto.getUri(),
                hitDto.getIp(),
                hitDto.getTimestamp()
        );
    }
}
