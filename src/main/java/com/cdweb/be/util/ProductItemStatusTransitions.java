package com.cdweb.be.util;

import com.cdweb.be.entity.ProductItem.ProductItemStatus;
import com.cdweb.be.exception.BadRequestException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** Quy tắc chuyển trạng thái IMEI/Serial hợp lệ. */
public final class ProductItemStatusTransitions {

  private static final Map<ProductItemStatus, Set<ProductItemStatus>> ALLOWED =
      new EnumMap<>(ProductItemStatus.class);

  static {
    ALLOWED.put(
        ProductItemStatus.AVAILABLE,
        Set.of(ProductItemStatus.RESERVED, ProductItemStatus.DEFECTIVE));
    ALLOWED.put(
        ProductItemStatus.RESERVED,
        Set.of(ProductItemStatus.AVAILABLE, ProductItemStatus.SOLD));
    ALLOWED.put(
        ProductItemStatus.SOLD,
        Set.of(ProductItemStatus.IN_REPAIR, ProductItemStatus.RETURNED));
    ALLOWED.put(ProductItemStatus.IN_REPAIR, Set.of(ProductItemStatus.SOLD));
    ALLOWED.put(
        ProductItemStatus.RETURNED,
        Set.of(ProductItemStatus.AVAILABLE, ProductItemStatus.DEFECTIVE));
    ALLOWED.put(ProductItemStatus.DEFECTIVE, Set.of(ProductItemStatus.AVAILABLE));
  }

  private ProductItemStatusTransitions() {}

  public static void assertTransition(
      ProductItemStatus from, ProductItemStatus to, boolean force) {
    if (from == to) {
      return;
    }
    if (force) {
      return;
    }
    Set<ProductItemStatus> targets = ALLOWED.get(from);
    if (targets == null || !targets.contains(to)) {
      throw new BadRequestException(
          "Không thể chuyển trạng thái từ '"
              + from
              + "' sang '"
              + to
              + "'. "
              + "Cho phép: "
              + (targets != null ? targets : "[]"));
    }
  }
}
