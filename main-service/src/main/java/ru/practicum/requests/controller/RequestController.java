package ru.practicum.requests.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.requests.service.RequestService;
import ru.practicum.requests.dto.RequestDto;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/requests")
public class RequestController {
    private final RequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequestDto addRequest(@PathVariable Long userId, @RequestParam Long eventId) throws NotFoundException {
        log.info("Запрос POST / /users/{userId}/requests / добавление запроса от пользователя {} на участие в событие {}",
                userId, eventId);
        return requestService.addRequest(userId, eventId);
    }

    @PatchMapping("/{requestId}/cancel")
    public RequestDto cancelRequest(@PathVariable Long userId, @PathVariable Long requestId) {
        log.info("Запрос PATCH / /users/{userId}/requests/{requestId}/cancel / отмена своего завпроса, пользователь {}," +
                " на участие в событие", userId);
        return requestService.cancelRequest(userId, requestId);
    }

    @GetMapping
    public List<RequestDto> getRequestsByUser(@PathVariable Long userId) throws NotFoundException {
        log.info("Запрос GET / /users/{userId}/requests / получение информации о заявках текущего пользователя {}, " +
                "на участие в чужих событиях", userId);
        return requestService.getRequestsByUser(userId);
    }
}
