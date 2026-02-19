package noonchissaum.backend.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    @Value("${spring.cloud.aws.s3.presign-expiration-minutes:60}")
    private long presignExpirationMinutes;

    /**
     * 단일 파일 업로드
     */
    public String upload(MultipartFile file, String dirName) {
        // 파일명 생성 (폴더명/UUID.확장자)
        String fileName = createFileName(file.getOriginalFilename(), dirName);

        try {
            // S3 업로드 요청 생성
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)                      // 버킷 이름
                    .key(fileName)                       // 파일 경로 (폴더/파일명)
                    .contentType(file.getContentType())  // 파일 타입
                    .build();

            // S3에 파일 업로드 실행
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        } catch (IOException e) {
            log.error("S3 파일 업로드 실패: {}", e.getMessage());
            throw new ApiException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        // 업로드된 파일의 URL 반환
        return getFileUrl(fileName);
    }

    /**
     * 다중 파일 업로드
     */
    public List<String> uploadMultiple(List<MultipartFile> files, String dirName) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(upload(file, dirName));  // 각 파일 업로드 후 URL 저장
        }
        return urls;
    }

    /**
     * 파일 삭제
     */
    public void delete(String fileUrl) {
        try {
            // URL에서 파일명 추출
            String fileName = extractFileName(fileUrl);

            // S3 삭제 요청 생성
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build();

            // S3에서 파일 삭제 실행
            s3Client.deleteObject(request);
            log.info("S3 파일 삭제 완료: {}", fileName);

        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", e.getMessage());
            throw new ApiException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    /**
     * 파일명 생성
     * - UUID 사용으로 파일명 중복 방지
     */
    private String createFileName(String originalFileName, String dirName) {
        String ext = getFileExtension(originalFileName);  // 확장자 추출
        return dirName + "/" + UUID.randomUUID() + ext;   // 폴더/UUID.확장자
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * S3 파일 URL 생성
     */
    private String getFileUrl(String fileName) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(presignExpirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("S3 URL 생성 실패: {}", e.getMessage());
            throw new ApiException(ErrorCode.FILE_URL_GENERATION_FAILED);
        }
    }

    /**
     * URL에서 파일명(키) 추출
     */
    private String extractFileName(String fileUrl) {
        String withoutQuery = fileUrl.split("\\?")[0];
        return withoutQuery.substring(withoutQuery.indexOf(".com/") + 5);
    }
}
