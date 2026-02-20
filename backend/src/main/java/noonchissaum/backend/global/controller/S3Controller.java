package noonchissaum.backend.global.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.service.S3Service;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * 프로필, 상품 이미지 업로드/삭제 공통
 */

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    /**
     * 단일 이미지 업로드
     * POST /api/images?type=profile
     * POST /api/images?type=item
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type
    ) {
        String dirName = getDirName(type);  // type에 따라 S3 폴더 결정
        String url = s3Service.upload(file, dirName);
        return ResponseEntity.ok(ApiResponse.success("이미지 업로드 성공", url));
    }

    /**
     * 다중 이미지 업로드
     * POST /api/images/multiple?type=item
     */
    @PostMapping(value = "/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<String>>> uploadMultipleImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("type") String type
    ) {
        String dirName = getDirName(type);
        List<String> urls = s3Service.uploadMultiple(files, dirName);
        return ResponseEntity.ok(ApiResponse.success("이미지 업로드 성공", urls));
    }

    /**
     * 이미지 삭제
     * DELETE /api/images?url=https://...
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteImage(@RequestParam("url") String url) {
        s3Service.delete(url);
        return ResponseEntity.ok(ApiResponse.success("이미지 삭제 성공"));
    }

    /**
     * 타입에 따라 S3 폴더명 반환
     */
    private String getDirName(String type) {
        return switch (type.toLowerCase()) {
            case "profile" -> "profiles";
            case "item" -> "items";
            default -> "etc";
        };
    }
}
