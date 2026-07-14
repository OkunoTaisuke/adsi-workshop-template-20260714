-- Sample users (password: password123)
INSERT INTO employees (email, password, name, role) VALUES
('admin@example.com', '$2a$10$EM3pVV88TwwhcsmIqoLGHeN.KVL7LqxT6UWaHUr/yO4QBsqP/2s8C', '管理者太郎', 'ADMIN'),
('tanaka@example.com', '$2a$10$EM3pVV88TwwhcsmIqoLGHeN.KVL7LqxT6UWaHUr/yO4QBsqP/2s8C', '田中太郎', 'EMPLOYEE'),
('sato@example.com', '$2a$10$EM3pVV88TwwhcsmIqoLGHeN.KVL7LqxT6UWaHUr/yO4QBsqP/2s8C', '佐藤花子', 'EMPLOYEE');
