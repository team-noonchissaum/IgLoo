package noonchissaum.backend.domain.category.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.category.dto.CategoryListRes;
import noonchissaum.backend.domain.category.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    /**
     * 카테고리 전체 조회
     */
    @GetMapping
    public List<CategoryListRes> getCategories(){
        return categoryService.categoryList();
    }
}
