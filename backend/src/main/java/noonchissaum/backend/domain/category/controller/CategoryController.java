package noonchissaum.backend.domain.category.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.category.dto.CategoryListRes;
import noonchissaum.backend.domain.category.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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


    /**카테고리 삭제-관리자*/
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
