package noonchissaum.backend.domain.category.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.category.dto.req.AddCategoryReq;
import noonchissaum.backend.domain.category.dto.res.CategoryListRes;
import noonchissaum.backend.domain.category.dto.res.CategoryRes;
import noonchissaum.backend.domain.category.service.CategoryService;
import noonchissaum.backend.global.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
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

    /**
     * admin 카테고리 추가
     * */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryRes>> addCategory(@RequestBody AddCategoryReq req) {
        CategoryRes categoryRes = categoryService.addCategory(req);
        return ResponseEntity.ok(ApiResponse.success("카테고리 추가 완료", categoryRes));
    }


    /**카테고리 삭제-관리자*/
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
