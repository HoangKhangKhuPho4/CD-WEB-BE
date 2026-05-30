package com.cdweb.be.config;

import com.cdweb.be.entity.Permission;
import com.cdweb.be.entity.Role;
import com.cdweb.be.repository.PermissionRepository;
import com.cdweb.be.repository.RoleRepository;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds RBAC roles & permissions (aligned with electro-store-backend) when tables are empty.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class RbacDataInitializer implements ApplicationRunner {

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    ensureExtendedPermissions();
    Map<String, Permission> permissions;
    if (roleRepository.count() == 0 || permissionRepository.count() == 0) {
      log.info("Initializing RBAC roles and permissions...");
      permissions = seedPermissions();
      seedRoles(permissions);
      log.info("RBAC seed completed.");
    } else {
      permissions = loadPermissionMap();
    }
    alignSalesRole(permissions);
    alignWarehouseRole(permissions);
  }

  private Map<String, Permission> loadPermissionMap() {
    Map<String, Permission> map = new LinkedHashMap<>();
    permissionRepository.findAll().forEach(p -> map.put(p.getCode(), p));
    return map;
  }

  /** SALES — 8 quyền (thiết kế DB cd_web). */
  private void alignSalesRole(Map<String, Permission> p) {
    roleRepository
        .findByName("SALES")
        .ifPresent(
            role -> {
              Set<Permission> expected =
                  codes(
                      p,
                      "PRODUCT_VIEW",
                      "ORDER_VIEW_ALL",
                      "ORDER_CONFIRM",
                      "ORDER_CANCEL",
                      "ORDER_ASSIGN_SHIPPING",
                      "ORDER_TRACKING_UPDATE",
                      "WARRANTY_MANAGE",
                      "REPORT_SALES");
              syncRolePermissions(role, expected, "SALES (8 permissions)");
            });
  }

  /** WAREHOUSE — 11 quyền (thiết kế DB cd_web, không ORDER_CONFIRM / ORDER_MANAGE / PRODUCT_MANAGE / STOCK_RETURN). */
  private void alignWarehouseRole(Map<String, Permission> p) {
    roleRepository
        .findByName("WAREHOUSE")
        .ifPresent(
            role -> {
              Set<Permission> expected =
                  codes(
                      p,
                      "PRODUCT_VIEW",
                      "PRODUCT_CREATE",
                      "PRODUCT_UPDATE",
                      "STOCK_IMPORT",
                      "IMEI_MANAGE",
                      "INVENTORY_STAT",
                      "ORDER_VIEW_ALL",
                      "ORDER_CANCEL",
                      "ORDER_ASSIGN_SHIPPING",
                      "ORDER_TRACKING_UPDATE",
                      "WARRANTY_MANAGE");
              syncRolePermissions(role, expected, "WAREHOUSE (11 permissions)");
            });
  }

  private void syncRolePermissions(Role role, Set<Permission> expected, String label) {
    if (!samePermissionCodes(role.getPermissions(), expected)) {
      role.setPermissions(expected);
      roleRepository.save(role);
      log.info("Aligned {} role with {}. Re-login staff to refresh JWT.", role.getName(), label);
    }
  }

  private boolean samePermissionCodes(Set<Permission> current, Set<Permission> expected) {
    if (current == null || expected == null) {
      return current == expected;
    }
    Set<String> a = new LinkedHashSet<>();
    current.forEach(perm -> a.add(perm.getCode()));
    Set<String> b = new LinkedHashSet<>();
    expected.forEach(perm -> b.add(perm.getCode()));
    return a.equals(b);
  }

  private Map<String, Permission> seedPermissions() {
    String[][] defs = {
      {"PRODUCT_VIEW", "Xem sản phẩm", "Xem danh sách và chi tiết sản phẩm"},
      {"PRODUCT_CREATE", "Thêm sản phẩm", "Thêm sản phẩm mới"},
      {"PRODUCT_UPDATE", "Cập nhật sản phẩm", "Chỉnh sửa sản phẩm"},
      {"STOCK_IMPORT", "Nhập kho", "Lập phiếu nhập kho"},
      {"IMEI_MANAGE", "Quản lý IMEI", "Quản lý serial/IMEI"},
      {"INVENTORY_STAT", "Báo cáo tồn kho", "Xem báo cáo tồn kho"},
      {"ORDER_VIEW_ALL", "Xem tất cả đơn hàng", "Xem danh sách đơn hàng hệ thống"},
      {"ORDER_CONFIRM", "Xác nhận đơn hàng", "Xác nhận đơn (Sales)"},
      {"ORDER_CANCEL", "Hủy đơn hàng", "Hủy đơn hàng"},
      {"ORDER_ASSIGN_SHIPPING", "Giao vận chuyển", "Giao đơn cho đơn vị vận chuyển"},
      {"ORDER_TRACKING_UPDATE", "Cập nhật hành trình", "Cập nhật trạng thái vận đơn"},
      {"USER_PROFILE_UPDATE", "Cập nhật hồ sơ", "Cập nhật thông tin cá nhân"},
      {"USER_ORDER_HISTORY", "Lịch sử mua hàng", "Xem lịch sử đơn cá nhân"},
      {"USER_WARRANTY_LOOKUP", "Tra cứu bảo hành", "Tra cứu bảo hành"},
      {"AI_RECOMMEND_VIEW", "Gợi ý AI", "Nhận gợi ý sản phẩm AI"},
      {"CHECKOUT_PAYMENT", "Thanh toán", "Thanh toán đơn hàng"},
      {"USER_MANAGE", "Quản lý người dùng", "Quản lý tài khoản nhân viên/khách"},
      {"ROLE_PERM_EDIT", "Chỉnh sửa phân quyền", "Sửa quyền theo vai trò"},
      {"REPORT_REVENUE", "Báo cáo doanh thu", "Báo cáo doanh thu"},
      {"QR_MANAGE", "Quản lý QR", "Quản lý mã QR"},
      {"AI_MODEL_TRAIN", "Huấn luyện AI", "Cấu hình mô hình AI"},
      {"WARRANTY_MANAGE", "Quản lý bảo hành", "Xử lý bảo hành"},
      {"REPORT_SALES", "Báo cáo bán hàng", "Thống kê bán hàng"},
      {"PRODUCT_MANAGE", "Quản lý sản phẩm", "CRUD sản phẩm, danh mục, coupon"},
      {"ORDER_MANAGE", "Quản lý đơn hàng", "Quản trị đơn hàng admin"},
      {"CUSTOMER_VIEW", "Xem khách hàng", "Xem danh sách khách hàng"},
      {"STOCK_RETURN", "Trả hàng kho", "Xử lý trả hàng nhập kho"},
      {"SYSTEM_CONFIG_MANAGE", "Cấu hình hệ thống", "Cấu hình website & AI"},
      {"ORDER_CREATE", "Tạo đơn hàng", "Đặt hàng / checkout"},
    };

    Map<String, Permission> map = new LinkedHashMap<>();
    for (String[] def : defs) {
      Permission p = new Permission();
      p.setCode(def[0]);
      p.setName(def[1]);
      p.setDescription(def[2]);
      map.put(def[0], permissionRepository.save(p));
    }
    return map;
  }

  private void seedRoles(Map<String, Permission> p) {
    createRole("ADMIN", "Quản trị viên", allPermissions(p));

    createRole(
        "WAREHOUSE",
        "Nhân viên kho",
        codes(
            p,
            "PRODUCT_VIEW",
            "PRODUCT_CREATE",
            "PRODUCT_UPDATE",
            "STOCK_IMPORT",
            "IMEI_MANAGE",
            "INVENTORY_STAT",
            "ORDER_VIEW_ALL",
            "ORDER_CANCEL",
            "ORDER_ASSIGN_SHIPPING",
            "ORDER_TRACKING_UPDATE",
            "WARRANTY_MANAGE"));

    createRole(
        "SALES",
        "Nhân viên bán hàng",
        codes(
            p,
            "PRODUCT_VIEW",
            "ORDER_VIEW_ALL",
            "ORDER_CONFIRM",
            "ORDER_CANCEL",
            "ORDER_ASSIGN_SHIPPING",
            "ORDER_TRACKING_UPDATE",
            "WARRANTY_MANAGE",
            "REPORT_SALES"));

    createRole(
        "CUSTOMER",
        "Khách hàng",
        codes(
            p,
            "PRODUCT_VIEW",
            "USER_PROFILE_UPDATE",
            "USER_ORDER_HISTORY",
            "USER_WARRANTY_LOOKUP",
            "AI_RECOMMEND_VIEW",
            "CHECKOUT_PAYMENT",
            "ORDER_CREATE",
            "ORDER_CANCEL"));
  }

  private void createRole(String name, String description, Set<Permission> permissions) {
    Role role = new Role();
    role.setName(name);
    role.setDescription(description);
    role.setPermissions(permissions);
    roleRepository.save(role);
  }

  private Set<Permission> allPermissions(Map<String, Permission> p) {
    return new LinkedHashSet<>(p.values());
  }

  private Set<Permission> codes(Map<String, Permission> p, String... codes) {
    Set<Permission> set = new LinkedHashSet<>();
    Arrays.stream(codes)
        .map(p::get)
        .filter(perm -> perm != null)
        .forEach(set::add);
    return set;
  }

  /** Add permissions introduced after first deploy (idempotent). */
  private void ensureExtendedPermissions() {
    String[][] extra = {
      {"ORDER_MANAGE", "Quản lý đơn hàng", "Quản trị đơn hàng admin"},
      {"CUSTOMER_VIEW", "Xem khách hàng", "Xem danh sách khách hàng"},
      {"STOCK_RETURN", "Trả hàng kho", "Xử lý trả hàng nhập kho"},
      {"SYSTEM_CONFIG_MANAGE", "Cấu hình hệ thống", "Cấu hình website & AI"},
      {"ORDER_CREATE", "Tạo đơn hàng", "Đặt hàng / checkout"},
    };
    boolean added = false;
    for (String[] def : extra) {
      if (permissionRepository.findByCode(def[0]).isEmpty()) {
        Permission perm = new Permission();
        perm.setCode(def[0]);
        perm.setName(def[1]);
        perm.setDescription(def[2]);
        permissionRepository.save(perm);
        added = true;
      }
    }
    if (added) {
      log.info("Added missing extended RBAC permissions.");
    }
  }
}
