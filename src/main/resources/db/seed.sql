-- Smart Cinema seed: roles, users, rooms, seats, combos

-- =============================================================================
-- Smart Cinema - DU LIEU EXPORT TU DATABASE smart_cinema_db
-- Tao tu mysqldump - dong bo voi DB hien tai
-- INSERT IGNORE: chay lai an toan, khong trung khoa
-- Mat khau demo (BCrypt 123456): $2a$12$Kry5fB4yN8Doz3xshxS7Y...
-- =============================================================================

-- ========== ROLES ==========
INSERT IGNORE INTO roles (role_id, role_name) VALUES (1,'ADMIN');
INSERT IGNORE INTO roles (role_id, role_name) VALUES (3,'CUSTOMER');
INSERT IGNORE INTO roles (role_id, role_name) VALUES (2,'STAFF');
-- (3 rows)

-- ========== USERS ==========
INSERT IGNORE INTO users (user_id, username, password_hash, email, role_id, auth_provider, provider_id, created_at) VALUES (1,'admin_demo','$2a$12$Kry5fB4yN8Doz3xshxS7Y.Z0Qf8qAqyG7OepV.e98C.h9h8vjTj6O','admin@smartcinema.com',1,'LOCAL',NULL,'2026-05-18 01:35:55');
INSERT IGNORE INTO users (user_id, username, password_hash, email, role_id, auth_provider, provider_id, created_at) VALUES (2,'staff_phuong','$2a$12$Kry5fB4yN8Doz3xshxS7Y.Z0Qf8qAqyG7OepV.e98C.h9h8vjTj6O','phuong.staff@smartcinema.com',2,'LOCAL',NULL,'2026-05-18 01:35:55');
INSERT IGNORE INTO users (user_id, username, password_hash, email, role_id, auth_provider, provider_id, created_at) VALUES (3,'customer_minh','$2a$12$Kry5fB4yN8Doz3xshxS7Y.Z0Qf8qAqyG7OepV.e98C.h9h8vjTj6O','minh.customer@gmail.com',3,'LOCAL',NULL,'2026-05-18 01:35:55');
INSERT IGNORE INTO users (user_id, username, password_hash, email, role_id, auth_provider, provider_id, created_at) VALUES (4,'hoangdinhtung','$2a$12$izYfBE7BfGEChYupldahXONNd4eyuQ27kpr9qaCbZK.SrymCPzKzm','mtung3365@gmail.com',3,'LOCAL',NULL,'2026-05-18 07:45:19');
INSERT IGNORE INTO users (user_id, username, password_hash, email, role_id, auth_provider, provider_id, created_at) VALUES (5,'customer_lan','$2a$12$Kry5fB4yN8Doz3xshxS7Y.Z0Qf8qAqyG7OepV.e98C.h9h8vjTj6O','lan.nguyen@yahoo.com',3,'LOCAL',NULL,'2026-05-18 10:08:35');
INSERT IGNORE INTO users (user_id, username, password_hash, email, role_id, auth_provider, provider_id, created_at) VALUES (6,'customer_tuan','$2a$12$Kry5fB4yN8Doz3xshxS7Y.Z0Qf8qAqyG7OepV.e98C.h9h8vjTj6O','tuan.tran@outlook.com',3,'LOCAL',NULL,'2026-05-18 10:08:35');
INSERT IGNORE INTO users (user_id, username, password_hash, email, role_id, auth_provider, provider_id, created_at) VALUES (7,'customer_hoa','$2a$12$Kry5fB4yN8Doz3xshxS7Y.Z0Qf8qAqyG7OepV.e98C.h9h8vjTj6O','hoa.le@gmail.com',3,'LOCAL',NULL,'2026-05-18 10:08:35');
INSERT IGNORE INTO users (user_id, username, password_hash, email, role_id, auth_provider, provider_id, created_at) VALUES (8,'customer_duc','$2a$12$Kry5fB4yN8Doz3xshxS7Y.Z0Qf8qAqyG7OepV.e98C.h9h8vjTj6O','duc.pham@icloud.com',3,'LOCAL',NULL,'2026-05-18 10:08:35');
INSERT IGNORE INTO users (user_id, username, password_hash, email, role_id, auth_provider, provider_id, created_at) VALUES (9,'customer_mai','$2a$12$Kry5fB4yN8Doz3xshxS7Y.Z0Qf8qAqyG7OepV.e98C.h9h8vjTj6O','mai.vo@hotmail.com',3,'LOCAL',NULL,'2026-05-18 10:08:35');
INSERT IGNORE INTO users (user_id, username, password_hash, email, role_id, auth_provider, provider_id, created_at) VALUES (10,'customer_kien','$2a$12$Kry5fB4yN8Doz3xshxS7Y.Z0Qf8qAqyG7OepV.e98C.h9h8vjTj6O','kien.bui@gmail.com',3,'LOCAL',NULL,'2026-05-18 10:08:35');
INSERT IGNORE INTO users (user_id, username, password_hash, email, role_id, auth_provider, provider_id, created_at) VALUES (11,'customer_thao','$2a$12$Kry5fB4yN8Doz3xshxS7Y.Z0Qf8qAqyG7OepV.e98C.h9h8vjTj6O','thao.dang@yahoo.com',3,'LOCAL',NULL,'2026-05-18 10:08:35');
INSERT IGNORE INTO users (user_id, username, password_hash, email, role_id, auth_provider, provider_id, created_at) VALUES (12,'customer_viet','$2a$12$Kry5fB4yN8Doz3xshxS7Y.Z0Qf8qAqyG7OepV.e98C.h9h8vjTj6O','viet.hoang@gmail.com',3,'LOCAL',NULL,'2026-05-18 10:08:35');
-- (12 rows)

-- ========== USER_PROFILES ==========
INSERT IGNORE INTO user_profiles (profile_id, user_id, full_name, phone_number, updated_at) VALUES (1,1,'Hệ Thống Admin','0123456789','2026-05-18 01:35:55');
INSERT IGNORE INTO user_profiles (profile_id, user_id, full_name, phone_number, updated_at) VALUES (2,2,'Nguyễn Thanh Phương','0987654321','2026-05-18 01:35:55');
INSERT IGNORE INTO user_profiles (profile_id, user_id, full_name, phone_number, updated_at) VALUES (3,3,'Trần Quốc Minh','0909123456','2026-05-18 01:35:55');
INSERT IGNORE INTO user_profiles (profile_id, user_id, full_name, phone_number, updated_at) VALUES (4,4,'Hoang Dinh Tung','0867422301','2026-05-18 07:45:19');
INSERT IGNORE INTO user_profiles (profile_id, user_id, full_name, phone_number, updated_at) VALUES (5,5,'Nguyễn Thị Lan','0933111222','2026-05-18 10:08:35');
INSERT IGNORE INTO user_profiles (profile_id, user_id, full_name, phone_number, updated_at) VALUES (6,6,'Trần Anh Tuấn','0944222333','2026-05-18 10:08:35');
INSERT IGNORE INTO user_profiles (profile_id, user_id, full_name, phone_number, updated_at) VALUES (7,7,'Lê Thị Hoa','0955333444','2026-05-18 10:08:35');
INSERT IGNORE INTO user_profiles (profile_id, user_id, full_name, phone_number, updated_at) VALUES (8,8,'Phạm Văn Đức','0966444555','2026-05-18 10:08:35');
INSERT IGNORE INTO user_profiles (profile_id, user_id, full_name, phone_number, updated_at) VALUES (9,9,'Võ Thị Mai','0977555666','2026-05-18 10:08:35');
INSERT IGNORE INTO user_profiles (profile_id, user_id, full_name, phone_number, updated_at) VALUES (10,10,'Bùi Văn Kiên','0988666777','2026-05-18 10:08:35');
INSERT IGNORE INTO user_profiles (profile_id, user_id, full_name, phone_number, updated_at) VALUES (11,11,'Đặng Thị Thảo','0999777888','2026-05-18 10:08:35');
INSERT IGNORE INTO user_profiles (profile_id, user_id, full_name, phone_number, updated_at) VALUES (12,12,'Hoàng Quốc Việt','0900888999','2026-05-18 10:08:35');
-- (12 rows)
-- ========== ROOMS ==========
INSERT IGNORE INTO rooms (room_id, room_name, total_seats) VALUES (1,'Phòng Chiếu 1 (IMAX)',20);
INSERT IGNORE INTO rooms (room_id, room_name, total_seats) VALUES (2,'Phòng Chiếu 2 (Standard)',10);
INSERT IGNORE INTO rooms (room_id, room_name, total_seats) VALUES (3,'Phòng 3 (Dolby Atmos)',18);
INSERT IGNORE INTO rooms (room_id, room_name, total_seats) VALUES (4,'Phòng 4 (4DX)',24);
INSERT IGNORE INTO rooms (room_id, room_name, total_seats) VALUES (5,'Phòng 5 (Couple)',12);
INSERT IGNORE INTO rooms (room_id, room_name, total_seats) VALUES (6,'Phòng 6 (VIP Lounge)',8);
-- (6 rows)

-- ========== SEATS ==========
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (1,1,'A',1,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (2,1,'A',2,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (3,1,'A',3,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (4,1,'A',4,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (5,1,'A',5,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (6,1,'A',6,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (7,1,'A',7,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (8,1,'A',8,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (9,1,'A',9,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (10,1,'A',10,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (11,1,'B',1,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (12,1,'B',2,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (13,1,'B',3,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (14,1,'B',4,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (15,1,'B',5,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (16,1,'B',6,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (17,1,'B',7,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (18,1,'B',8,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (19,1,'B',9,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (20,1,'B',10,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (21,2,'A',1,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (22,2,'A',2,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (23,2,'A',3,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (24,2,'A',4,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (25,2,'A',5,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (26,2,'A',6,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (27,2,'A',7,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (28,2,'A',8,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (29,2,'A',9,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (30,2,'A',10,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (31,3,'A',1,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (32,3,'A',2,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (33,3,'A',3,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (34,3,'A',4,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (35,3,'A',5,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (36,3,'A',6,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (37,3,'B',1,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (38,3,'B',2,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (39,3,'B',3,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (40,3,'B',4,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (41,3,'B',5,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (42,3,'B',6,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (43,3,'C',1,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (44,3,'C',2,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (45,3,'C',3,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (46,3,'C',4,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (47,3,'C',5,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (48,3,'C',6,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (49,4,'A',1,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (50,4,'A',2,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (51,4,'A',3,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (52,4,'A',4,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (53,4,'A',5,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (54,4,'A',6,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (55,4,'A',7,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (56,4,'A',8,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (57,4,'B',1,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (58,4,'B',2,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (59,4,'B',3,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (60,4,'B',4,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (61,4,'B',5,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (62,4,'B',6,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (63,4,'B',7,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (64,4,'B',8,'STANDARD');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (65,4,'C',1,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (66,4,'C',2,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (67,4,'C',3,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (68,4,'C',4,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (69,4,'C',5,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (70,4,'C',6,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (71,4,'C',7,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (72,4,'C',8,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (73,5,'A',1,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (74,5,'A',2,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (75,5,'A',3,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (76,5,'A',4,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (77,5,'B',1,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (78,5,'B',2,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (79,5,'B',3,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (80,5,'B',4,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (81,5,'C',1,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (82,5,'C',2,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (83,5,'C',3,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (84,5,'C',4,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (85,6,'V',1,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (86,6,'V',2,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (87,6,'V',3,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (88,6,'V',4,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (89,6,'V',5,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (90,6,'V',6,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (91,6,'V',7,'VIP');
INSERT IGNORE INTO seats (seat_id, room_id, row_name, seat_number, seat_type) VALUES (92,6,'V',8,'VIP');
-- (92 rows)
-- ========== COMBOS ==========
INSERT IGNORE INTO combos (combo_id, name, description, price, status) VALUES (1,'Combo Solo','1 Bắp ngọt vừa + 1 Nước ngọt Coca-Cola cỡ vừa',75000.00,'ACTIVE');
INSERT IGNORE INTO combos (combo_id, name, description, price, status) VALUES (2,'Combo Couple','1 Bắp ngọt lớn + 2 Nước ngọt Coca-Cola lớn',115000.00,'ACTIVE');
INSERT IGNORE INTO combos (combo_id, name, description, price, status) VALUES (3,'Combo Super Family','2 Bắp lớn vị phô mai/caramel + 4 Nước ngọt tự chọn',220000.00,'ACTIVE');
INSERT IGNORE INTO combos (combo_id, name, description, price, status) VALUES (4,'Combo Student','1 Bắp nhỏ + 1 nước (ưu đãi SV)',55000.00,'ACTIVE');
INSERT IGNORE INTO combos (combo_id, name, description, price, status) VALUES (5,'Combo Premium','1 Bắp caramel + 2 nước energy + snack',165000.00,'ACTIVE');
INSERT IGNORE INTO combos (combo_id, name, description, price, status) VALUES (6,'Combo Zero Sugar','1 Bắp + 2 nước không đường',89000.00,'ACTIVE');
INSERT IGNORE INTO combos (combo_id, name, description, price, status) VALUES (7,'Combo Ice Cream','2 kem Häagen + 2 nước',135000.00,'ACTIVE');
INSERT IGNORE INTO combos (combo_id, name, description, price, status) VALUES (8,'Combo Deluxe (ngung ban)','Gói cũ — thay bằng Premium',200000.00,'INACTIVE');
-- (8 rows)
