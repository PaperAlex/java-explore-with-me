package ru.practicum.requests.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.requests.dto.ConfirmedRequests;
import ru.practicum.requests.enums.RequestStatus;
import ru.practicum.requests.model.Request;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    Request findByIdAndRequesterId(Long requestId, Long userId);

    List<Request> findAllByEventId(Long eventId);

    List<Request> findAllByRequesterId(Long userId);

    List<Request> findAllByEventIdAndIdInAndStatus(Long eventId, List<Long> requestId, RequestStatus status);

    Boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("SELECT new ru.practicum.requests.dto.ConfirmedRequests(COUNT(DISTINCT r.id), r.event.id) " +
            "FROM Request AS r " +
            "WHERE r.event.id IN (:ids) AND r.status = :status " +
            "GROUP BY (r.event)")
    List<ConfirmedRequests> findAllByEventIdInAndStatus(@Param("ids")List<Long> ids, @Param("status")RequestStatus status);
}
