-- Voucher mẫu + phát cho khách (chạy sau khi có users; Hibernate ddl-auto=update tạo bảng tự động)
INSERT IGNORE INTO vouchers (voucher_id, code, title, description, discount_type, discount_value, min_order_amount, max_discount_amount, valid_from, valid_until, status, require_combo) VALUES
(1,'NCC10','Giảm 10% vé phim','Áp dụng mọi suất 2D','PERCENT',10.00,100000.00,30000.00,'2026-01-01 00:00:00','2027-12-31 23:59:59','ACTIVE',0),
(2,'NCC50K','Giảm 50.000đ','Đơn từ 200.000đ','FIXED',50000.00,200000.00,NULL,'2026-01-01 00:00:00','2027-12-31 23:59:59','ACTIVE',0),
(3,'U22-55K','Gen Z -55K','Ưu đãi khách trẻ','FIXED',55000.00,150000.00,NULL,'2026-01-01 00:00:00','2027-12-31 23:59:59','ACTIVE',0),
(4,'COMBO20','Combo -20%','Cần có combo trong đơn','PERCENT',20.00,0.00,50000.00,'2026-01-01 00:00:00','2027-12-31 23:59:59','ACTIVE',1),
(5,'WELCOME5','Chào mừng -5%','Voucher đăng ký mới','PERCENT',5.00,50000.00,15000.00,'2026-01-01 00:00:00','2027-12-31 23:59:59','ACTIVE',0),
(6,'VIP100K','VIP -100K','Đơn lớn từ 400K','FIXED',100000.00,400000.00,NULL,'2026-01-01 00:00:00','2027-12-31 23:59:59','ACTIVE',0);

INSERT IGNORE INTO user_vouchers (user_voucher_id, user_id, voucher_id, status, claimed_at) VALUES
(1,3,1,'AVAILABLE','2026-05-18 10:00:00'),
(2,3,2,'AVAILABLE','2026-05-18 10:00:00'),
(3,3,3,'AVAILABLE','2026-05-18 10:00:00'),
(4,3,4,'AVAILABLE','2026-05-18 10:00:00'),
(5,3,5,'AVAILABLE','2026-05-18 10:00:00'),
(6,4,1,'AVAILABLE','2026-05-18 10:00:00'),
(7,4,2,'AVAILABLE','2026-05-18 10:00:00'),
(8,5,1,'AVAILABLE','2026-05-18 10:00:00'),
(9,5,5,'AVAILABLE','2026-05-18 10:00:00'),
(10,6,2,'AVAILABLE','2026-05-18 10:00:00'),
(11,7,3,'AVAILABLE','2026-05-18 10:00:00'),
(12,8,1,'AVAILABLE','2026-05-18 10:00:00');
