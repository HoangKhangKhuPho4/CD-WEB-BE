package com.cdweb.be.controller;

import com.cdweb.be.entity.Image;
import com.cdweb.be.repository.ImageRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/img")
@CrossOrigin(origins = "*")
public class ImageController {

  private final ImageRepository imageRepository;

  private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

  // Đường dẫn gốc trên filesystem nơi chứa thư mục img/...
  @Value("${app.images.path:src/main/resources/static/}")
  private String imagesBasePath;

  public ImageController(ImageRepository imageRepository) {
    this.imageRepository = imageRepository;
  }

  @GetMapping("/{imageId}")
  public ResponseEntity<Resource> getImageById(@PathVariable("imageId") Integer imageId) {
    try {
      Image image =
          imageRepository
              .findById(imageId)
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.NOT_FOUND, "Image record not found in DB"));

      String imagePathFromDb = image.getImageUrl();
      logger.debug("getImageById id={} linkFromDb='{}'", imageId, imagePathFromDb);

      // Nếu DB lưu URL đầy đủ, redirect tới URL đó để trình duyệt tải trực tiếp
      if (imagePathFromDb != null && imagePathFromDb.matches("^https?://.*")) {
        logger.debug("Redirecting to external URL for image id={}", imageId);
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(imagePathFromDb))
            .build();
      }

      // chuẩn hoá đường dẫn DB: bỏ "./" hoặc "/" đầu
      String cleaned =
          imagePathFromDb == null
              ? ""
              : imagePathFromDb.replaceFirst("^\\./+", "").replaceFirst("^/+", "");
      logger.debug("Cleaned path='{}' (imagesBasePath='{}')", cleaned, imagesBasePath);

      // decode + normalize filename (handle spaces, %20, utf-8 chars)
      String decoded = URLDecoder.decode(cleaned, StandardCharsets.UTF_8);
      String normalized = decoded.replaceAll("\\\\+", "/");

      // Build a list of candidate Resource locations to try in order
      Resource resource = null;
      // 1) classpath under /static/
      Resource classpathStatic = new ClassPathResource("static/" + normalized);
      if (classpathStatic.exists() && classpathStatic.isReadable()) {
        resource = classpathStatic;
        logger.debug("Found on classpath: static/{}", normalized);
      }

      // 2) classpath root (some build setups put files directly on classpath root)
      if (resource == null) {
        Resource classpathRoot = new ClassPathResource(normalized);
        if (classpathRoot.exists() && classpathRoot.isReadable()) {
          resource = classpathRoot;
          logger.debug("Found on classpath root: {}", normalized);
        }
      }

      // 3) try filesystem app.images.path (resolve relative to working dir if needed)
      if (resource == null) {
        Path base = Paths.get(imagesBasePath);
        if (!base.isAbsolute()) {
          base = Paths.get(System.getProperty("user.dir")).resolve(imagesBasePath).normalize();
        }
        Path fsCandidate = base.resolve(normalized).toAbsolutePath().normalize();
        logger.debug("Trying filesystem path: {}", fsCandidate);
        Resource fsRes = new FileSystemResource(fsCandidate.toFile());
        if (fsRes.exists() && fsRes.isReadable()) {
          resource = fsRes;
          logger.debug("Found on filesystem: {}", fsCandidate);
        }
      }

      // 4) try common build output folder (target/classes/static)
      if (resource == null) {
        Path targetCandidate =
            Paths.get(System.getProperty("user.dir"), "target", "classes", "static", normalized)
                .toAbsolutePath()
                .normalize();
        logger.debug("Trying target/classes fallback: {}", targetCandidate);
        Resource fsRes = new FileSystemResource(targetCandidate.toFile());
        if (fsRes.exists() && fsRes.isReadable()) {
          resource = fsRes;
          logger.debug("Found in target/classes/static: {}", targetCandidate);
        }
      }

      // 5) As a last attempt, try ClassLoader resource lookup (handles jar packaging edge-cases)
      if (resource == null) {
        java.net.URL url = ImageController.class.getResource("/static/" + normalized);
        if (url == null) url = ImageController.class.getResource("/" + normalized);
        if (url != null) {
          Resource clsRes = new org.springframework.core.io.UrlResource(url);
          if (clsRes.exists() && clsRes.isReadable()) {
            resource = clsRes;
            logger.debug("Found by ClassLoader at URL: {}", url);
          }
        }
      }

      if (resource == null || !resource.exists() || !resource.isReadable()) {
        logger.warn(
            "Image not found (id={}, cleaned='{}') on any candidate paths", imageId, normalized);
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Image file not found for path: " + imagePathFromDb);
      }

      logger.debug(
          "Serving image id={} from {} (exists={}, readable={})",
          imageId,
          resource instanceof ClassPathResource ? "classpath" : "filesystem",
          resource.exists(),
          true);
      String contentType = probeContentType(resource, normalized);

      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(contentType))
          .header(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(7, TimeUnit.DAYS).getHeaderValue())
          .body(resource);

    } catch (ResponseStatusException rse) {
      throw rse;
    } catch (Exception e) {
      logger.error("Error serving image id={}: {}", imageId, e.getMessage(), e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Could not read image file", e);
    }
  }

  // Hỗ trợ truy cập trực tiếp bằng đường dẫn: /img/<any/path/to/file.jpg>
  @GetMapping("/**")
  public ResponseEntity<Resource> getImageByPath(HttpServletRequest request) {
    try {
      String uri = URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8);
      String prefix = request.getContextPath() + "/img/";
      String relativePath = uri.startsWith(prefix) ? uri.substring(prefix.length()) : uri;
      logger.debug("getImageByPath requestUri='{}' relativePath='{}'", uri, relativePath);

      if (relativePath.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image path is empty");
      }

      if (relativePath.matches("^https?://.*")) {
        logger.debug("Redirecting to external URL requested directly: {}", relativePath);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(relativePath)).build();
      }

      String cleaned = relativePath.replaceFirst("^\\./+", "").replaceFirst("^/+", "");
      String decoded = URLDecoder.decode(cleaned, StandardCharsets.UTF_8);
      String normalized = decoded.replaceAll("\\\\+", "/");
      // Reuse same candidate strategy as above
      Resource resource = null;
      Resource r1 = new ClassPathResource("static/" + normalized);
      if (r1.exists() && r1.isReadable()) resource = r1;
      else {
        Resource r2 = new ClassPathResource(normalized);
        if (r2.exists() && r2.isReadable()) resource = r2;
      }
      if (resource == null) {
        Path base = Paths.get(imagesBasePath);
        if (!base.isAbsolute())
          base = Paths.get(System.getProperty("user.dir")).resolve(imagesBasePath).normalize();
        Path fsCandidate = base.resolve(normalized).toAbsolutePath().normalize();
        logger.debug("Trying filesystem path for direct request: {}", fsCandidate);
        Resource fsRes = new FileSystemResource(fsCandidate.toFile());
        if (fsRes.exists() && fsRes.isReadable()) resource = fsRes;
      }
      if (resource == null) {
        Path targetCandidate =
            Paths.get(System.getProperty("user.dir"), "target", "classes", "static", normalized)
                .toAbsolutePath()
                .normalize();
        Resource fsRes = new FileSystemResource(targetCandidate.toFile());
        if (fsRes.exists() && fsRes.isReadable()) resource = fsRes;
      }
      if (resource == null) {
        java.net.URL url = ImageController.class.getResource("/static/" + normalized);
        if (url == null) url = ImageController.class.getResource("/" + normalized);
        if (url != null) {
          Resource clsRes = new org.springframework.core.io.UrlResource(url);
          if (clsRes.exists() && clsRes.isReadable()) resource = clsRes;
        }
      }

      if (resource == null || !resource.exists() || !resource.isReadable()) {
        logger.warn("Image path not found: {}", cleaned);
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Image file not found for path: " + cleaned);
      }

      String contentType = probeContentType(resource, normalized);

      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(contentType))
          .header(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(7, TimeUnit.DAYS).getHeaderValue())
          .body(resource);
    } catch (ResponseStatusException rse) {
      throw rse;
    } catch (Exception e) {
      logger.error("Error serving image by path: {}", e.getMessage(), e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Could not read image file", e);
    }
  }

  // Try to probe content type; fall back to extension mapping
  private String probeContentType(Resource resource, String pathHint) {
    try {
      if (resource instanceof FileSystemResource) {
        Path p = ((FileSystemResource) resource).getFile().toPath();
        String ct = Files.probeContentType(p);
        if (ct != null) return ct;
      } else {
        Path p = Paths.get(pathHint);
        String ct = Files.probeContentType(p);
        if (ct != null) return ct;
      }
    } catch (Exception ignored) {
    }
    String lc = pathHint.toLowerCase();
    if (lc.endsWith(".png")) return "image/png";
    if (lc.endsWith(".jpg") || lc.endsWith(".jpeg")) return "image/jpeg";
    if (lc.endsWith(".gif")) return "image/gif";
    return MediaType.APPLICATION_OCTET_STREAM_VALUE;
  }
}
