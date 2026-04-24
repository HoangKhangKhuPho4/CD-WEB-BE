package com.cdweb.be.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class FileUploadController {

  @Value("${app.images.path:src/main/resources/static/}")
  private String imagesBasePath;

  @PostMapping("/upload")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
    Map<String, Object> response = new HashMap<>();

    if (file.isEmpty()) {
      response.put("success", false);
      response.put("message", "Vui lòng chọn file để tải lên!");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Kiểm tra xem có phải là file ảnh hay không
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      response.put("success", false);
      response.put("message", "Chỉ hỗ trợ tải lên các tệp định dạng ảnh!");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    try {
      // 1. Chuẩn hoá đường dẫn gốc
      Path base = Paths.get(imagesBasePath);
      if (!base.isAbsolute()) {
        base = Paths.get(System.getProperty("user.dir")).resolve(imagesBasePath).normalize();
      }

      // 2. Tạo thư mục 'uploads' nếu chưa tồn tại
      Path uploadDir = base.resolve("uploads").toAbsolutePath().normalize();
      if (!Files.exists(uploadDir)) {
        Files.createDirectories(uploadDir);
      }

      // 3. Tạo tên file duy nhất tránh trùng lặp
      String originalFilename = file.getOriginalFilename();
      String extension = "";
      if (originalFilename != null && originalFilename.contains(".")) {
        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
      }
      String newFilename = UUID.randomUUID().toString() + extension;

      // 4. Lưu file vào thư mục đích
      Path targetLocation = uploadDir.resolve(newFilename);
      Files.copy(file.getInputStream(), targetLocation);

      // 5. Build URL trả về cho FE
      // Định dạng: /img/uploads/<newFilename>
      // Sẽ khớp với luồng GetMapping("/**") của ImageController
      String fileDownloadUri =
          ServletUriComponentsBuilder.fromCurrentContextPath()
              .path("/img/uploads/")
              .path(newFilename)
              .toUriString();

      response.put("success", true);
      response.put("url", fileDownloadUri);
      return ResponseEntity.ok(response);

    } catch (IOException ex) {
      response.put("success", false);
      response.put("message", "Lỗi trong quá trình lưu file: " + ex.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }
}
