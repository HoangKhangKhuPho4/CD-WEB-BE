-- Xóa sản phẩm test: SP-583 .. SP-588 (id = 583,584,585,586,588)
-- Chạy: mysql -u root cd_web < src/main/resources/db/delete-test-products-583-588.sql

USE cd_web;

-- Mã SP-xxx trên Admin = id sản phẩm (products.id)
-- SP-583 iPhone 16 Pro Max | SP-584 test tối giản | SP-585/586 Update Tên 1 | SP-588 Test nổi bật

START TRANSACTION;

SELECT id, name FROM products WHERE id IN (583, 584, 585, 586, 588);

DELETE FROM user_interactions WHERE product_id IN (583, 584, 585, 586, 588);

DELETE ri FROM review_images ri
INNER JOIN reviews r ON r.id = ri.review_id
WHERE r.product_id IN (583, 584, 585, 586, 588);

DELETE FROM reviews WHERE product_id IN (583, 584, 585, 586, 588);

DELETE FROM wishlists WHERE product_id IN (583, 584, 585, 586, 588);

DELETE i FROM images i
INNER JOIN product_variants pv ON pv.id = i.variant_id
WHERE i.product_id IN (583, 584, 585, 586, 588)
   OR pv.product_id IN (583, 584, 585, 586, 588);

DELETE vav FROM variant_attribute_values vav
INNER JOIN product_variants pv ON pv.id = vav.variant_id
WHERE pv.product_id IN (583, 584, 585, 586, 588);

DELETE FROM product_specifications WHERE product_id IN (583, 584, 585, 586, 588);

DELETE FROM product_variants WHERE product_id IN (583, 584, 585, 586, 588);

DELETE FROM products WHERE id IN (583, 584, 585, 586, 588);

COMMIT;

SELECT COUNT(*) AS remaining FROM products WHERE id IN (583, 584, 585, 586, 588);
