package ru.practicum.locations.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.locations.dto.LocationDto;
import ru.practicum.locations.model.Location;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationMapper {
    public static Location toLocation(LocationDto locationDto) {
        return new Location(locationDto.getLat(), locationDto.getLon());
    }

    public static LocationDto toLocationDto(Location location) {
        return new LocationDto(location.getLat(), location.getLon());
    }
}
