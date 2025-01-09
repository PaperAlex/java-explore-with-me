package ru.practicum.categories.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.categories.dto.CategoryDto;
import ru.practicum.categories.dto.NewCategoryDto;
import ru.practicum.categories.mapper.CategoryMapper;
import ru.practicum.categories.model.Category;
import ru.practicum.categories.repository.CategoryRepository;
import ru.practicum.exceptions.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories(Integer from, Integer size) {
        log.info("Запрос GET /categories /, getAllCategories категории, параментры: from = {}, size = {}", from, size);
        return categoryRepository.findAll(PageRequest.of(from / size, size)).stream()
                .map(CategoryMapper::toCategoryDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long categoryId) throws NotFoundException {
        log.info("Запрос GET /categories /, getCategoryById категории {}", categoryId);
        return CategoryMapper.toCategoryDto(getCategory(categoryId));
    }

    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        log.info("Запрос POST / admin/categories /, addCategory сохранить категорию: \"{}\"", newCategoryDto.getName());
        return CategoryMapper.toCategoryDto(categoryRepository.save(CategoryMapper.toCategory(newCategoryDto)));
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long categoryId, CategoryDto categoryDto) throws NotFoundException {
        Category category = getCategory(categoryId);
        category.setName(categoryDto.getName());
        log.info("Запрос PATCH / admin/categories /, updateCategory обновить категорию: \"{}\"", categoryDto.getName());
        return CategoryMapper.toCategoryDto(categoryRepository.save(category));
    }


    @Override
    @Transactional
    public void deleteCategory(Long categoryId) throws NotFoundException {
        getCategory(categoryId);
        log.info("Запрос DELETE / admin/categories / deleteCategory удаляем данные о категории: {}", categoryId);
        categoryRepository.deleteById(categoryId);
    }

    private Category getCategory(Long categoryId) throws NotFoundException {
        return categoryRepository.findById(categoryId).orElseThrow(() ->
                new NotFoundException("Category with id=" + categoryId + " was not found"));
    }
}
