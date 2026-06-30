"""Patch cd_web.sql: align user/role FK types with JPA (bigint) + warehouse fulfillment."""
import re
from pathlib import Path

path = Path(__file__).resolve().parent.parent / "cd_web.sql"
text = path.read_text(encoding="utf-8")

replacements = [
    ("`user_id` int NULL DEFAULT NULL", "`user_id` bigint NULL DEFAULT NULL"),
    ("`user_id` int NOT NULL COMMENT", "`user_id` bigint NOT NULL COMMENT"),
    ("`user_id` int NOT NULL,", "`user_id` bigint NOT NULL,"),
    ("`role_id` int NOT NULL,", "`role_id` bigint NOT NULL,"),
    ("`created_by` int NULL DEFAULT NULL", "`created_by` bigint NULL DEFAULT NULL"),
    ("`updated_by` int NULL DEFAULT NULL", "`updated_by` bigint NULL DEFAULT NULL"),
    ("`user_address_id` int NULL DEFAULT NULL", "`user_address_id` bigint NULL DEFAULT NULL"),
    ("`staff_id` int NULL DEFAULT NULL", "`staff_id` bigint NULL DEFAULT NULL"),
]
for old, new in replacements:
    text = text.replace(old, new)

text = re.sub(
    r"(CREATE TABLE `users`\s*\(\s*\n\s*)`id` int NOT NULL AUTO_INCREMENT,",
    r"\1`id` bigint NOT NULL AUTO_INCREMENT,",
    text,
    count=1,
)
text = re.sub(
    r"(CREATE TABLE `roles`\s*\(\s*\n\s*)`id` int NOT NULL AUTO_INCREMENT,",
    r"\1`id` bigint NOT NULL AUTO_INCREMENT,",
    text,
    count=1,
)
text = re.sub(
    r"(CREATE TABLE `user_addresses`\s*\(\s*\n\s*)`id` int NOT NULL AUTO_INCREMENT,",
    r"\1`id` bigint NOT NULL AUTO_INCREMENT,",
    text,
    count=1,
)

if "picked_by_user_id" not in text:
    text = text.replace(
        "  `to_ward_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_vietnamese_ci NULL DEFAULT NULL,\n"
        "  PRIMARY KEY (`id`) USING BTREE,",
        "  `to_ward_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_vietnamese_ci NULL DEFAULT NULL,\n"
        "  `picked_by_user_id` bigint NULL DEFAULT NULL,\n"
        "  `picked_at` datetime(6) NULL DEFAULT NULL,\n"
        "  PRIMARY KEY (`id`) USING BTREE,",
        1,
    )
    text = text.replace(
        "  CONSTRAINT `orders_users_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) "
        "ON DELETE RESTRICT ON UPDATE RESTRICT\n) ENGINE = InnoDB AUTO_INCREMENT = 77",
        "  CONSTRAINT `orders_users_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) "
        "ON DELETE RESTRICT ON UPDATE RESTRICT,\n"
        "  CONSTRAINT `fk_orders_picked_by_user` FOREIGN KEY (`picked_by_user_id`) "
        "REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT\n"
        ") ENGINE = InnoDB AUTO_INCREMENT = 77",
        1,
    )
    text = re.sub(
        r"(INSERT INTO `orders` VALUES \([^;]+?)(\);)",
        lambda m: m.group(1) + ", NULL, NULL" + m.group(2),
        text,
    )

audit_block = """

-- ----------------------------
-- Table structure for inventory_audit_sheets
-- ----------------------------
DROP TABLE IF EXISTS `inventory_audit_scans`;
DROP TABLE IF EXISTS `inventory_audit_sheets`;
CREATE TABLE `inventory_audit_sheets`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `sheet_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `scanned_count` int NOT NULL DEFAULT 0,
  `variance` int NOT NULL DEFAULT 0,
  `note` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `approved_by` bigint NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `approved_at` datetime(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_inventory_audit_sheet_code`(`sheet_code` ASC) USING BTREE,
  INDEX `FK_inventory_audit_created_by`(`created_by` ASC) USING BTREE,
  INDEX `FK_inventory_audit_approved_by`(`approved_by` ASC) USING BTREE,
  CONSTRAINT `FK_inventory_audit_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `FK_inventory_audit_approved_by` FOREIGN KEY (`approved_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for inventory_audit_scans
-- ----------------------------
CREATE TABLE `inventory_audit_scans`  (
  `sheet_id` int NOT NULL,
  `scan_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  INDEX `FK_inventory_audit_scans_sheet`(`sheet_id` ASC) USING BTREE,
  CONSTRAINT `FK_inventory_audit_scans_sheet` FOREIGN KEY (`sheet_id`) REFERENCES `inventory_audit_sheets` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

"""

if "inventory_audit_sheets" not in text:
    text = text.replace("\nSET FOREIGN_KEY_CHECKS = 1;", audit_block + "\nSET FOREIGN_KEY_CHECKS = 1;")

path.write_text(text, encoding="utf-8")
print(f"Patched {path}")
