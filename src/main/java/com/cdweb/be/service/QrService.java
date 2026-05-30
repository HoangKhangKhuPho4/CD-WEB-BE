package com.cdweb.be.service;

import com.cdweb.be.dto.QrDto;
import com.cdweb.be.dto.UserDto;
import com.cdweb.be.entity.Order;
import com.cdweb.be.entity.OrderStatusHistory;
import com.cdweb.be.entity.QrToken;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.OrderRepository;
import com.cdweb.be.repository.OrderStatusHistoryRepository;
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

  private static final long QR_TTL_SECONDS = 120;

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Autowired private QrTokenRepository qrTokenRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private OrderRepository orderRepository;
  @Autowired private OrderStatusHistoryRepository orderStatusHistoryRepository;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private ModelMapper modelMapper;

  @Transactional
  public QrDto.GenerateResponse generateQr(QrDto.GenerateRequest req) {
    QrToken.QrType qrType = parseQrType(req.getQrType());
    Integer orderId = req.getOrderId();

    if (qrType == QrToken.QrType.QR_ORDER_CONFIRMATION) {
      if (orderId == null) {
        throw new BadRequestException("orderId bắt buộc khi xác nhận đơn bằng QR");
      }
      Order order =
          orderRepository
              .findById(orderId)
              .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
      if (order.getStatus() != Order.OrderStatus.PENDING) {
        throw new BadRequestException("Chỉ xác nhận QR được khi đơn đang chờ xác nhận");
      }
    }

    String sessionId = UUID.randomUUID().toString();
    String qrJwt = buildQrJwt(sessionId, qrType.name(), orderId);

    QrToken qrToken = new QrToken();
    qrToken.setSessionId(sessionId);
    qrToken.setToken(qrJwt);
    qrToken.setQrType(qrType);
    qrToken.setStatus(QrToken.QrStatus.PENDING);
    qrToken.setExpiresAt(LocalDateTime.now().plusSeconds(QR_TTL_SECONDS));
    if (orderId != null) {
      qrToken.setOrderId(orderId);
    }
    qrTokenRepository.save(qrToken);

    String qrContent = "cdweb://qr?token=" + qrJwt;
    return new QrDto.GenerateResponse(sessionId, qrJwt, qrContent, QR_TTL_SECONDS, "PENDING");
  }

  @Transactional
  public QrDto.StatusResponse scanQr(String username, QrDto.ScanRequest req) {
    Claims claims = parseQrJwt(req.getToken());
    String sessionId = claims.getSubject();
    String qrTypeClaim = claims.get("qrType", String.class);

    QrToken qrToken =
        qrTokenRepository
            .findBySessionId(sessionId)
            .orElseThrow(() -> new BadRequestException("QR token không hợp lệ"));

    if (qrToken.isExpired() || qrToken.getStatus() == QrToken.QrStatus.EXPIRED) {
      throw new BadRequestException("QR đã hết hạn");
    }
    if (qrToken.getStatus() != QrToken.QrStatus.PENDING) {
      throw new BadRequestException("QR đã được sử dụng");
    }

    User user =
        userRepository
            .findByUsernameOrEmail(username, username)
            .orElseThrow(() -> new BadRequestException("User not found"));

    if (qrToken.getQrType() == QrToken.QrType.QR_ORDER_CONFIRMATION) {
      Order order = loadOrderForQr(qrToken.getOrderId());
      assertOrderOwner(order, user);
    }

    qrToken.setUser(user);
    qrToken.setStatus(QrToken.QrStatus.SCANNED);
    qrToken.setScannedAt(LocalDateTime.now());
    qrTokenRepository.save(qrToken);

    return buildStatusResponse(qrToken, qrTypeClaim, null, null);
  }

  @Transactional
  public QrDto.StatusResponse confirmQr(String username, QrDto.ConfirmRequest req) {
    QrToken qrToken =
        qrTokenRepository
            .findBySessionId(req.getSessionId())
            .orElseThrow(() -> new BadRequestException("QR session không tồn tại"));

    if (qrToken.getStatus() != QrToken.QrStatus.SCANNED) {
      throw new BadRequestException("QR chưa được quét hoặc đã xử lý");
    }
    if (qrToken.getUser() == null || !qrToken.getUser().getUsername().equals(username)) {
      throw new BadRequestException("Không có quyền xác nhận QR này");
    }

    qrToken.setStatus(QrToken.QrStatus.CONFIRMED);
    qrTokenRepository.save(qrToken);

    if (qrToken.getQrType() == QrToken.QrType.QR_ORDER_CONFIRMATION) {
      Order order = loadOrderForQr(qrToken.getOrderId());
      assertOrderOwner(order, qrToken.getUser());
      if (order.getStatus() == Order.OrderStatus.PENDING) {
        String fromStatus = order.getStatus().name();
        order.setStatus(Order.OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        orderRepository.save(order);
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrder(order);
        h.setFromStatus(fromStatus);
        h.setToStatus(Order.OrderStatus.CONFIRMED.name());
        h.setNote("Xác nhận đơn qua QR");
        h.setChangedBy(qrToken.getUser());
        orderStatusHistoryRepository.save(h);
      }
      return buildStatusResponse(qrToken, qrToken.getQrType().name(), null, order);
    }

    String loginJwt = jwtTokenProvider.generateTokenFromUsername(username);
    UserDto.Response userResponse = modelMapper.map(qrToken.getUser(), UserDto.Response.class);
    return buildStatusResponse(qrToken, QrToken.QrType.QR_LOGIN.name(), loginJwt, null);
  }

  public QrDto.StatusResponse getStatus(String sessionId) {
    QrToken qrToken =
        qrTokenRepository
            .findBySessionId(sessionId)
            .orElseThrow(() -> new BadRequestException("QR session không tồn tại"));

    if (qrToken.isExpired() && qrToken.getStatus() == QrToken.QrStatus.PENDING) {
      qrToken.setStatus(QrToken.QrStatus.EXPIRED);
      qrTokenRepository.save(qrToken);
    }

    String loginJwt = null;
    UserDto.Response userResponse = null;
    Order order = null;

    if (qrToken.getStatus() == QrToken.QrStatus.CONFIRMED) {
      if (qrToken.getQrType() == QrToken.QrType.QR_LOGIN && qrToken.getUser() != null) {
        loginJwt = jwtTokenProvider.generateTokenFromUsername(qrToken.getUser().getUsername());
        userResponse = modelMapper.map(qrToken.getUser(), UserDto.Response.class);
      } else if (qrToken.getQrType() == QrToken.QrType.QR_ORDER_CONFIRMATION
          && qrToken.getOrderId() != null) {
        order = orderRepository.findById(qrToken.getOrderId()).orElse(null);
      }
    }

    return buildStatusResponse(
        qrToken, qrToken.getQrType().name(), loginJwt, order != null ? order : null);
  }

  @Scheduled(fixedDelay = 300_000)
  public void cleanupExpiredTokens() {
    qrTokenRepository.expireOldTokens(LocalDateTime.now());
  }

  private QrDto.StatusResponse buildStatusResponse(
      QrToken qrToken, String qrType, String loginJwt, Order order) {
    QrDto.StatusResponse r = new QrDto.StatusResponse();
    r.setSessionId(qrToken.getSessionId());
    r.setStatus(qrToken.getStatus().name());
    r.setQrType(qrType);
    r.setJwtToken(loginJwt);
    if (qrToken.getUser() != null && loginJwt != null) {
      r.setUser(modelMapper.map(qrToken.getUser(), UserDto.Response.class));
    }
    if (order != null) {
      r.setOrderId(order.getId());
      r.setOrderCode(order.getOrderCode());
      r.setOrderStatus(order.getStatus().name());
    } else if (qrToken.getOrderId() != null) {
      orderRepository
          .findById(qrToken.getOrderId())
          .ifPresent(
              o -> {
                r.setOrderId(o.getId());
                r.setOrderCode(o.getOrderCode());
                r.setOrderStatus(o.getStatus().name());
              });
    }
    return r;
  }

  private Order loadOrderForQr(Integer orderId) {
    if (orderId == null) {
      throw new BadRequestException("QR đơn hàng thiếu mã đơn");
    }
    return orderRepository
        .findById(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
  }

  private void assertOrderOwner(Order order, User user) {
    if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
      throw new BadRequestException("Bạn không phải chủ đơn hàng này");
    }
  }

  private QrToken.QrType parseQrType(String raw) {
    if (raw == null || raw.isBlank()) {
      return QrToken.QrType.QR_LOGIN;
    }
    try {
      return QrToken.QrType.valueOf(raw);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("qrType không hợp lệ");
    }
  }

  private String buildQrJwt(String sessionId, String qrType, Integer orderId) {
    SecretKey key =
        Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var builder =
        Jwts.builder()
            .subject(sessionId)
            .claim("qrType", qrType)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + QR_TTL_SECONDS * 1000));
    if (orderId != null) {
      builder.claim("orderId", orderId);
    }
    return builder.signWith(key).compact();
  }

  private Claims parseQrJwt(String token) {
    try {
      SecretKey key =
          Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    } catch (Exception e) {
      throw new BadRequestException("QR token không hợp lệ hoặc đã hết hạn");
    }
  }
}
