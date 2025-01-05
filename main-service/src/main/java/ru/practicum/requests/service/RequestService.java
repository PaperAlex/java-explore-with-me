package ru.practicum.requests.service;

import ru.practicum.exceptions.NotFoundException;
import ru.practicum.requests.dto.EventRequestStatusUpdateRequest;
import ru.practicum.requests.dto.EventRequestStatusUpdateResult;
import ru.practicum.requests.dto.RequestDto;

import java.util.List;

public interface RequestService {
    RequestDto addRequest(Long userId, Long eventId) throws NotFoundException;

    EventRequestStatusUpdateResult updateRequestsStatus(Long userId, Long eventId,
                                                        EventRequestStatusUpdateRequest statusUpdateRequest) throws NotFoundException;

    RequestDto cancelRequest(Long userId, Long requestId);

    List<RequestDto> getRequestsByEventOwner(Long userId, Long eventId) throws NotFoundException;

    List<RequestDto> getRequestsByUser(Long userId) throws NotFoundException;
}
