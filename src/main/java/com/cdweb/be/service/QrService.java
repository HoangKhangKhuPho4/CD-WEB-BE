package com.cdweb.be.service;

import com.cdweb.be.dto.QrDto;
import com.cdweb.be.dto.UserDto;
import com.cdweb.be.entity.QrToken;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.repository.QrTokenRepository;
import com.cdweb.be.repository.UserRepository;
import com.cdweb.be.util.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QrService {

    private static final long QR_TTL_SECONDS = 120; // 2 phút

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Autowired private QrTokenRepository qrTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private ModelMapper modelMapper;

    // ───── Bước 1: Tạo QR (gọi từ web – màn hình chờ) ─────
    @Transactional
    public QrDto.GenerateResponse generateQr(QrDto.GenerateRequest req) {
        String sessionId = UUID.randomUUID().toString();

        // Tạo short-lived JWT (2 phút)
        String qrJwt = buildQrJwt(sessionId, req.getQrType(), req.getOrderId());

        QrToken qrToken = new QrToken();
        qrToken.setSessionId(sessionId);
        qrToken.setToken(qrJwt);
        qrToken.setQrType(QrToken.QrType.valueOf(req.getQrType()));
        qrToken.setStatus(QrToken.QrStatus.PENDING);
        qrToken.setExpiresAt(LocalDateTime.now().plusSeconds(QR_TTL_SECONDS));
        if (req.getOrderId() != null) qrToken.setOrderId(req.getOrderId());
        qrTokenRepository.save(qrToken);

        // QR content = backend verify URL (mobile app scan → gọi /api/qr/scan)
        String qrContent = "cdweb://qr?token=" + qrJwt;

        return new QrDto.GenerateResponse(sessionId, qrJwt, qrContent, QR_TTL_SECONDS, "PENDING");
    }

    // ───── Bước 2: Mobile scan → xác thực token ─────
    @Transactional
    public QrDto.StatusResponse scanQr(String username, QrDto.ScanRequest req) {
        Claims claims = parseQrJwt(req.getToken());
        String sessionId = claims.getSubject();

        QrToken qrToken = qrTokenRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BadRequestException("QR token không hợp lệ"));

        if (qrToken.isExpired() || qrToken.getStatus() == QrToken.QrStatus.EXPIRED) {
            throw new BadRequestException("QR đã hết hạn");
        }
        if (qrToken.getStatus() != QrToken.QrStatus.PENDING) {
            throw new BadRequestException("QR đã được sử dụng");
        }

        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        qrToken.setUser(user);
        qrToken.setStatus(QrToken.QrStatus.SCANNED);
        qrToken.setScannedAt(LocalDateTime.now());
        qrTokenRepository.save(qrToken);

        return new QrDto.StatusResponse(sessionId, "SCANNED", null, null);
    }

    // ───── Bước 3: Mobile confirm → web nhận JWT ─────
    @Transactional
    public QrDto.StatusResponse confirmQr(String username, QrDto.ConfirmRequest req) {
        QrToken qrToken = qrTokenRepository.findBySessionId(req.getSessionId())
                .orElseThrow(() -> new BadRequestException("QR session không tồn tại"));

        if (qrToken.getStatus() != QrToken.QrStatus.SCANNED) {
            throw new BadRequestException("QR chưa được quét hoặc đã xử lý");
        }

        // Đảm bảo người confirm chính là người scan
        if (!qrToken.getUser().getUsername().equals(username)) {
            throw new BadRequestException("Không có quyền xác nhận QR này");
        }

        qrToken.setStatus(QrToken.QrStatus.CONFIRMED);
        qrTokenRepository.save(qrToken);

        // Tạo JWT đầy đủ để web đăng nhập
        String loginJwt = jwtTokenProvider.generateTokenFromUsername(username);

        UserDto.Response userResponse = modelMapper.map(qrToken.getUser(), UserDto.Response.class);
        return new QrDto.StatusResponse(req.getSessionId(), "CONFIRMED", loginJwt, userResponse);
    }

    // ───── Polling / Kiểm tra trạng thái từ web ─────
    public QrDto.StatusResponse getStatus(String sessionId) {
        QrToken qrToken = qrTokenRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BadRequestException("QR session không tồn tại"));

        if (qrToken.isExpired() && qrToken.getStatus() == QrToken.QrStatus.PENDING) {
            qrToken.setStatus(QrToken.QrStatus.EXPIRED);
            qrTokenRepository.save(qrToken);
        }

        String loginJwt = null;
        UserDto.Response userResponse = null;

        if (qrToken.getStatus() == QrToken.QrStatus.CONFIRMED && qrToken.getUser() != null) {
            loginJwt = jwtTokenProvider.generateTokenFromUsername(qrToken.getUser().getUsername());
            userResponse = modelMapper.map(qrToken.getUser(), UserDto.Response.class);
        }

        return new QrDto.StatusResponse(sessionId, qrToken.getStatus().name(), loginJwt, userResponse);
    }

    // ───── Dọn dẹp token hết hạn mỗi 5 phút ─────
    @Scheduled(fixedDelay = 300_000)
    public void cleanupExpiredTokens() {
        qrTokenRepository.expireOldTokens(LocalDateTime.now());
    }

    // ───── Helpers ─────
    private String buildQrJwt(String sessionId, String qrType, Integer orderId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(sessionId)
                .claim("qrType", qrType)
                .claim("orderId", orderId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + QR_TTL_SECONDS * 1000))
                .signWith(key)
                .compact();
    }

    private Claims parseQrJwt(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            throw new BadRequestException("QR token không hợp lệ hoặc đã hết hạn");
        }
    }
}