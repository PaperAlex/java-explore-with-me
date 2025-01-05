package ru.practicum.users.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.users.dto.UserDto;
import ru.practicum.users.dto.UserNewDto;
import ru.practicum.users.dto.UserShortDto;
import ru.practicum.users.model.User;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserMapper {
    public static UserDto toUserDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    public static UserShortDto toUserShortDto(User user) {
        return new UserShortDto(
                user.getId(),
                user.getName()
        );
    }

    public static User toUser(UserNewDto userNewDto) {
        return new User(
                userNewDto.getName(),
                userNewDto.getEmail()
        );
    }
}
