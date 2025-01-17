package ru.practicum.comments.service;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comments.dto.CommentDto;
import ru.practicum.comments.dto.CommentNewDto;
import ru.practicum.comments.mapper.CommentMapper;
import ru.practicum.comments.model.Comment;
import ru.practicum.comments.repository.CommentRepository;
import ru.practicum.events.model.Event;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.exceptions.BadRequestException;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.requests.repository.RequestRepository;
import ru.practicum.users.model.User;
import ru.practicum.users.mapper.UserMapper;
import ru.practicum.users.repository.UserRepository;
import ru.practicum.users.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.events.enums.State.PUBLISHED;
import static ru.practicum.requests.enums.RequestStatus.CONFIRMED;


@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;


    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getComments(Long eventId, Integer from, Integer size) throws NotFoundException {
        Event event = getEvent(eventId);
        EventShortDto eventShort = EventMapper.toEventShortDto(event,
                requestRepository.countByEventIdAndStatus(eventId, CONFIRMED));
        log.info("Получаем комментарии к событию: eventId={} " +
                "с параметрами from={}, size={}", eventId, from, size);
        return commentRepository.findAllByEventId(eventId, PageRequest.of(from / size, size))
                .stream()
                .map(c -> CommentMapper.toCommentDto(c, UserMapper.toUserShortDto(c.getAuthor()), eventShort))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDto getCommentById(Long commentId) throws NotFoundException {
        Comment comment = getComment(commentId);
        UserShortDto userShort = UserMapper.toUserShortDto(comment.getAuthor());
        EventShortDto eventShort = EventMapper.toEventShortDto(comment.getEvent(),
                requestRepository.countByEventIdAndStatus(comment.getEvent().getId(), CONFIRMED));
        log.info("Получаем иформацию о комметарии commentId={}", commentId);
        return CommentMapper.toCommentDto(comment, userShort, eventShort);
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, CommentNewDto commentNewDto) throws NotFoundException {
        User author = getUser(userId);
        Event event = getEvent(eventId);
        if (event.getState() != PUBLISHED) {
            throw new BadRequestException("Event must have status PUBLISH to comment.");
        }
        UserShortDto userShort = UserMapper.toUserShortDto(author);
        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, CONFIRMED);
        EventShortDto eventShort = EventMapper.toEventShortDto(event, confirmedRequests);
        Comment comment = commentRepository.save(CommentMapper.toComment(commentNewDto, author, event, confirmedRequests));
        log.info("Добавляем комментарий от пользователя: userId={}, eventId={}, comment={}", userId, eventId, commentNewDto);
        return CommentMapper.toCommentDto(comment, userShort, eventShort);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long eventId, Long commentId, CommentNewDto commentNewDto) throws NotFoundException {
        User author = getUser(userId);
        Event event = getEvent(eventId);
        Comment comment = commentRepository.findById(commentId).orElseThrow(() ->
                new NotFoundException("Comment with id=" + commentId + " was not found"));
        if (comment.getEvent() != event) {
            throw new ValidationException("Comment for another event.");
        }
        comment.setText(commentNewDto.getText());
        comment.setEdited(LocalDateTime.now());
        UserShortDto userShort = UserMapper.toUserShortDto(author);
        EventShortDto eventShort = EventMapper.toEventShortDto(event,
                requestRepository.countByEventIdAndStatus(eventId, CONFIRMED));
        log.info("Обновляем комментарий пользователя: userId={}, eventId={}, commentId={}", userId, eventId, commentId);
        return CommentMapper.toCommentDto(comment, userShort, eventShort);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByUser(Long userId, Integer from, Integer size) throws NotFoundException {
        User author = getUser(userId);
        List<Comment> comments =
                commentRepository.findAllByAuthorId(
                        userId, PageRequest.of(from / size, size));
        UserShortDto userShort = UserMapper.toUserShortDto(author);
        List<CommentDto> result = new ArrayList<>();
        for (Comment c : comments) {
            EventShortDto eventShort = EventMapper.toEventShortDto(c.getEvent(), c.getConfirmedRequests());
            result.add(CommentMapper.toCommentDto(c, userShort, eventShort));
        }
        log.info("Получение информации о комментариях пользователя userId={} " +
                "с параметрами from={}, size={}", userId, from, size);
        return result;
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) throws NotFoundException {
        User author = getUser(userId);
        Comment comment = getComment(commentId);
        if (comment.getAuthor() != author) {
            throw new ValidationException("User who made a comment can delete it.");
        }
        log.info("Удаляем комментарий пользователя: userId={}, commentId={}", userId, commentId);
        commentRepository.deleteById(commentId);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) throws NotFoundException {
        getComment(commentId);
        log.info("Удаляем комментарий: commentId={}", commentId);
        commentRepository.deleteById(commentId);
    }

    private User getUser(Long userId) throws NotFoundException {
        return userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User with id=" + userId + " was not found"));
    }

    private Event getEvent(Long eventId) throws NotFoundException {
        return eventRepository.findById(eventId).orElseThrow(() ->
                new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private Comment getComment(Long commentId) throws NotFoundException {
        return commentRepository.findById(commentId).orElseThrow(() ->
                new NotFoundException("Comment with id=" + commentId + " was not found"));
    }
}
