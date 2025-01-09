package ru.practicum.locations.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.locations.model.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    boolean existsByLatAndLon(Float lat, Float lon);

    Location findByLatAndLon(Float lat, Float lon);
}
