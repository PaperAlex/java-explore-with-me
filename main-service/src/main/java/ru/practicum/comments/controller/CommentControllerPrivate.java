package ru.practicum.comments.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comments.service.CommentService;
import ru.practicum.comments.dto.CommentDto;
import ru.practicum.comments.dto.CommentNewDto;
import ru.practicum.exceptions.NotFoundException;

import java.util.List;


@Validated
@RestController
@RequestMapping("/users/{userId}/comments")
@RequiredArgsConstructor
public class CommentControllerPrivate {
    private final CommentService commentService;

    @PostMapping("/{eventId}")
    @ResponseStatus(value = HttpStatus.CREATED)
    public CommentDto addComment(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody @Valid CommentNewDto commentNewDto) throws NotFoundException {
        return commentService.addComment(userId, eventId, commentNewDto);
    }

    @PatchMapping("/{eventId}/{commentId}")
    public CommentDto updateComment(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentNewDto commentNewDto) throws NotFoundException {
        return commentService.updateComment(userId, eventId, commentId, commentNewDto);
    }

    @GetMapping
    public List<CommentDto> getComments(
            @PathVariable Long userId,
            @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(value = "size", defaultValue = "10") @Positive Integer size)
            throws NotFoundException {
        return commentService.getCommentsByUser(userId, from, size);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long userId,
            @PathVariable Long commentId) throws NotFoundException {
        commentService.deleteComment(userId, commentId);
    }
}
