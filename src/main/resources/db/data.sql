INSERT INTO categories (id, name, icon)
SELECT 1, 'OLI', 'oli' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 1);
INSERT INTO categories (id, name, icon)
SELECT 2, 'BAN', 'ban' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 2);
INSERT INTO categories (id, name, icon)
SELECT 3, 'MESIN', 'mesin' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 3);
INSERT INTO categories (id, name, icon)
SELECT 4, 'AKI', 'aki' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 4);
INSERT INTO categories (id, name, icon)
SELECT 5, 'KAMPAS', 'kampas' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 5);
INSERT INTO categories (id, name, icon)
SELECT 6, 'LAMPU', 'lampu' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 6);

INSERT INTO products (id, category_id, name, sku, selling_price, cost_price, stock, minimum_stock, sold, icon, status)
SELECT 1, 1, 'Oli Shell Advance AX7', 'OLI-SHL-001', 65000, 28000, 156, 20, 320, 'oli', 'Tersedia'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 1);
INSERT INTO products (id, category_id, name, sku, selling_price, cost_price, stock, minimum_stock, sold, icon, status)
SELECT 2, 2, 'Ban IRC NR91 80/90', 'BAN-IRC-042', 215000, 165000, 42, 15, 145, 'ban', 'Tersedia'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 2);
INSERT INTO products (id, category_id, name, sku, selling_price, cost_price, stock, minimum_stock, sold, icon, status)
SELECT 3, 3, 'Busi Denso Iridium', 'BUS-DNS-992', 105000, 45000, 156, 12, 89, 'mesin', 'Tersedia'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 3);
INSERT INTO products (id, category_id, name, sku, selling_price, cost_price, stock, minimum_stock, sold, icon, status)
SELECT 4, 4, 'Aki GS Astra GTZ5S', 'AKI-GSA-005', 245000, 185000, 3, 10, 42, 'aki', 'Stok Kritis'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 4);

INSERT INTO suppliers (id, name, pic, phone, email, address, category, logo, status)
SELECT 1, 'PT Jaya Abadi Tekstil', 'Budi Santoso', '081234567890', 'budi@jayaabadi.id', 'Bandung', 'Sparepart Mesin', 'PT', 'AKTIF'
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE id = 1);
INSERT INTO suppliers (id, name, pic, phone, email, address, category, logo, status)
SELECT 2, 'CV Maju Terus', 'Ani Wijaya', '081234567891', 'ani@maju-terus.id', 'Jakarta', 'Ban & Kaki-kaki', 'CV', 'AKTIF'
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE id = 2);
INSERT INTO suppliers (id, name, pic, phone, email, address, category, logo, status)
SELECT 3, 'UD Berkah Jaya', 'Agus Setiawan', '081234567892', 'agus@berkahjaya.id', 'Surabaya', 'Kelistrikan', 'UD', 'NONAKTIF'
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE id = 3);

INSERT INTO users (id, name, username, email, password, role, status)
SELECT 1, 'Raihan Haryo', 'admin_sipart', 'admin@sipart.test', 'demo', 'Owner', 'Aktif'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 1);
INSERT INTO users (id, name, username, email, password, role, status)
SELECT 2, 'Admin Gudang', 'budi_gudang', 'gudang@sipart.test', 'demo', 'Admin Gudang', 'Aktif'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 2);
INSERT INTO users (id, name, username, email, password, role, status)
SELECT 3, 'Kasir SIPART', 'citra_kasir', 'kasir@sipart.test', 'demo', 'Admin Kasir', 'Aktif'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 3);

INSERT INTO purchases (id, supplier_name, item_name, category_name, quantity, unit_price, status, purchase_date)
SELECT 9201, 'PT. Global Tekno', 'Busi Denso Iridium', 'MESIN', 250, 49600, 'Barang Datang', '2023-10-20 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM purchases WHERE id = 9201);
INSERT INTO purchases (id, supplier_name, item_name, category_name, quantity, unit_price, status, purchase_date)
SELECT 9188, 'Sumber Makmur', 'Oli Shell Advance AX7', 'OLI', 45, 71000, 'Pending', '2023-10-19 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM purchases WHERE id = 9188);
INSERT INTO purchases (id, supplier_name, item_name, category_name, quantity, unit_price, status, purchase_date)
SELECT 9142, 'Anugerah Sparepart', 'Ban IRC NR91 80/90', 'BAN', 1200, 38166, 'Selesai', '2023-10-18 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM purchases WHERE id = 9142);

INSERT INTO stock_opnames (id, item_name, sku, system_stock, physical_stock, difference, status)
SELECT 1, 'Ban IRC NR82 - 90/90-14', 'TIRE-IRC-01', 45, 45, 0, 'MATCH'
WHERE NOT EXISTS (SELECT 1 FROM stock_opnames WHERE id = 1);
INSERT INTO stock_opnames (id, item_name, sku, system_stock, physical_stock, difference, status)
SELECT 2, 'Aki Kering GS Astra', 'BAT-GS-N540', 150, 138, -12, 'DIFFERENCE'
WHERE NOT EXISTS (SELECT 1 FROM stock_opnames WHERE id = 2);
INSERT INTO stock_opnames (id, item_name, sku, system_stock, physical_stock, difference, status)
SELECT 3, 'Busi NGK Iridium', 'PLG-NGK-IR', 80, NULL, NULL, 'PENDING'
WHERE NOT EXISTS (SELECT 1 FROM stock_opnames WHERE id = 3);
INSERT INTO stock_opnames (id, item_name, sku, system_stock, physical_stock, difference, status)
SELECT 4, 'V-Belt Honda Vario', 'BELT-HND-VR', 25, 25, 0, 'MATCH'
WHERE NOT EXISTS (SELECT 1 FROM stock_opnames WHERE id = 4);

INSERT INTO user_permissions (user_id, menu_name, page_access, can_add, can_edit, can_delete, can_export)
SELECT u.id, m.menu_name,
       CASE WHEN u.role = 'Owner' OR (u.role = 'Admin Gudang' AND m.menu_name IN ('dashboard', 'products', 'stock', 'suppliers', 'purchases')) OR (u.role = 'Admin Kasir' AND m.menu_name = 'sales') THEN TRUE ELSE FALSE END,
       CASE WHEN u.role = 'Owner' OR (u.role = 'Admin Gudang' AND m.menu_name IN ('dashboard', 'products', 'stock', 'suppliers', 'purchases')) OR (u.role = 'Admin Kasir' AND m.menu_name = 'sales') THEN TRUE ELSE FALSE END,
       CASE WHEN u.role = 'Owner' OR (u.role = 'Admin Gudang' AND m.menu_name IN ('dashboard', 'products', 'stock', 'suppliers', 'purchases')) OR (u.role = 'Admin Kasir' AND m.menu_name = 'sales') THEN TRUE ELSE FALSE END,
       CASE WHEN u.role = 'Owner' THEN TRUE ELSE FALSE END,
       CASE WHEN u.role = 'Owner' OR (u.role = 'Admin Gudang' AND m.menu_name IN ('dashboard', 'products', 'stock', 'suppliers', 'purchases')) OR (u.role = 'Admin Kasir' AND m.menu_name = 'sales') THEN TRUE ELSE FALSE END
FROM users u
CROSS JOIN (
    SELECT 'dashboard' menu_name UNION ALL SELECT 'products' UNION ALL SELECT 'stock'
    UNION ALL SELECT 'suppliers' UNION ALL SELECT 'purchases' UNION ALL SELECT 'sales'
    UNION ALL SELECT 'users'
) m
WHERE NOT EXISTS (
    SELECT 1 FROM user_permissions p WHERE p.user_id = u.id AND p.menu_name = m.menu_name
);

UPDATE user_permissions
SET page_access = FALSE, can_add = FALSE, can_edit = FALSE, can_delete = FALSE, can_export = FALSE
WHERE menu_name = 'sales'
  AND user_id IN (SELECT id FROM users WHERE role = 'Owner');
