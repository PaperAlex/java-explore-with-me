package ru.practicum.requests.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.requests.dto.RequestDto;
import ru.practicum.requests.model.Request;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestMapper {
    public static RequestDto toRequestDto(Request request) {
        return new RequestDto(
                request.getId(),
                request.getCreated(),
                request.getEvent().getId(),
                request.getRequester().getId(),
                request.getStatus()
        );
    }
}
