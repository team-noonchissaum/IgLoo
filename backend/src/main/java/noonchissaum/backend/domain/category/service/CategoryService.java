package noonchissaum.backend.domain.category.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.category.dto.res.CategoryListRes;
import noonchissaum.backend.domain.category.dto.req.AddCategoryReq;
import noonchissaum.backend.domain.category.dto.res.CategoryRes;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public Category getcategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    public List<CategoryListRes> categoryList(){
        return categoryRepository.findAll()
                .stream()
                .map(CategoryListRes::from)
                .toList();
    }

    public CategoryRes addCategory(AddCategoryReq req) {
        Category parent = null;
        if (req.parentId() != null) {
            parent = getcategory(req.parentId());
        }

        Category category = new Category(req.name(), parent);
        categoryRepository.save(category);
        return CategoryRes.from(category);
    }
}
