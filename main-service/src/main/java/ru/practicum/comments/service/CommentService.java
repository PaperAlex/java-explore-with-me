package ru.practicum.comments.service;

import ru.practicum.comments.dto.CommentDto;
import ru.practicum.comments.dto.CommentNewDto;
import ru.practicum.exceptions.NotFoundException;

import java.util.List;

public interface CommentService {
    CommentDto addComment(Long userId, Long eventId, CommentNewDto commentNewDto) throws NotFoundException;

    CommentDto updateComment(Long userId, Long eventId, Long commentId, CommentNewDto commentNewDto) throws NotFoundException;

    List<CommentDto> getCommentsByUser(Long userId, Integer from, Integer size) throws NotFoundException;

    List<CommentDto> getComments(Long eventId, Integer from, Integer size) throws NotFoundException;

    CommentDto getCommentById(Long commentId) throws NotFoundException;

    void deleteComment(Long userId, Long commentId) throws NotFoundException;

    void deleteComment(Long commentId) throws NotFoundException;
}
