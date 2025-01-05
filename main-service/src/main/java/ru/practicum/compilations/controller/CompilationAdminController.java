package ru.practicum.compilations.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.compilations.service.CompilationService;
import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.NewCompilationDto;
import ru.practicum.compilations.dto.UpdateCompilationRequest;
import ru.practicum.exceptions.NotFoundException;

@Slf4j
@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
public class CompilationAdminController {
    private final CompilationService compilationService;

    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    public CompilationDto addCompilation(@RequestBody @Valid NewCompilationDto newCompilationDto) {
        log.info("Запрос POST /admin/compilations / add сохранить подборку: \"{}\"", newCompilationDto.getTitle());
        return compilationService.addCompilation(newCompilationDto);
    }

    @PatchMapping("/{compilationId}")
    public CompilationDto updateCompilation(@PathVariable Long compilationId,
                                            @RequestBody @Valid UpdateCompilationRequest updateCompilation)
            throws NotFoundException {
        log.info("Запрос PATCH /admin/compilations/{compilationId} / update обновить подборку: \"{}\"",
                updateCompilation.getTitle());
        return compilationService.updateCompilation(compilationId, updateCompilation);
    }

    @DeleteMapping("/{compilationId}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteCompilation(@PathVariable long compilationId) throws NotFoundException {
        log.info("Запрос DELETE /admin/compilations/{compilationId} / delete удалить подборку {}", compilationId);
        compilationService.deleteCompilation(compilationId);
    }
}
