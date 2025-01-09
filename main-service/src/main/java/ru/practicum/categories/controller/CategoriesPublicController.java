package ru.practicum.categories.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.categories.service.CategoryService;
import ru.practicum.categories.dto.CategoryDto;
import ru.practicum.exceptions.NotFoundException;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoriesPublicController {
    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDto> getAll(
            @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(value = "size", defaultValue = "10") @Positive Integer size) {
        return categoryService.getAllCategories(from, size);
    }

    @GetMapping("/{categoryId}")
    public CategoryDto getById(@PathVariable Long categoryId) throws NotFoundException {
        return categoryService.getCategoryById(categoryId);
    }
}