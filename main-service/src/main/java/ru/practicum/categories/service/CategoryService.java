package ru.practicum.categories.service;

import ru.practicum.categories.dto.CategoryDto;
import ru.practicum.categories.dto.NewCategoryDto;
import ru.practicum.exceptions.NotFoundException;

import java.util.List;

public interface CategoryService {
    List<CategoryDto> getAllCategories(Integer from, Integer size);

    CategoryDto getCategoryById(Long id) throws NotFoundException;

    CategoryDto addCategory(NewCategoryDto newCategoryDto);

    CategoryDto updateCategory(Long categoryId, CategoryDto categoryDto) throws NotFoundException;

    void deleteCategory(Long categoryId) throws NotFoundException;
}
