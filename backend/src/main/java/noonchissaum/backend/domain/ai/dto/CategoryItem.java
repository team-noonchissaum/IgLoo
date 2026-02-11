package noonchissaum.backend.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryItem {
    private Long id;
    private String name;
    private Long parentId;
    private String path;
    private java.util.List<String> aliases;
    private boolean isLeaf;
}
