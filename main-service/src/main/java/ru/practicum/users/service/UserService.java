package ru.practicum.users.service;

import ru.practicum.users.dto.UserDto;
import ru.practicum.users.dto.UserNewDto;
import ru.practicum.exceptions.NotFoundException;

import java.util.List;

public interface UserService {
    UserDto addUser(UserNewDto userNewDto);

    List<UserDto> getUsers(List<Long> userIds, Integer from, Integer size);

    void deleteUser(Long userId) throws NotFoundException;
}
