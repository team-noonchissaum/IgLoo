package noonchissaum.backend.domain.category.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.category.dto.res.CategoryListRes;
import noonchissaum.backend.domain.category.dto.req.AddCategoryReq;
import noonchissaum.backend.domain.category.dto.res.CategoryRes;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.category.repository.CategoryRepository;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;

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

    /**카테고리 삭제*/
    @Transactional
    public void deleteCategory(Long categoryId) {
        Category target = categoryRepository.findById(categoryId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리"));

        if("기타".equals(target.getName())){
            throw new IllegalArgumentException("'기타' 카테고리는 삭제할 수 없습니다.");
        }

        Category etcCategory = categoryRepository.findByName("기타").orElseThrow(() -> new IllegalArgumentException("'기타' 카테고리가 존재하지 않습니다."));

        List<Category> deleteTargets = new ArrayList<>();//삭제 대상+하위 카테고리 모음
        collectCategories(target, deleteTargets);

        List<Item>items = itemRepository.findByCategoryIn(deleteTargets);
        for(Item item : items){
            item.changeCategory(etcCategory);
        }

        //하위 카테고리->부모카테고리 순서대로 삭제시키기
        for(int i=deleteTargets.size()-1;i>=0;i--){
            categoryRepository.delete(deleteTargets.get(i));
        }
    }
    //카테고리 '기타'로 이동시키기
    private void collectCategories(Category category, List<Category> result){
        result.add(category);
        List<Category> children = categoryRepository.findByParent(category);
        for(Category child : children){
            collectCategories(child, result);
        }
    }
}
