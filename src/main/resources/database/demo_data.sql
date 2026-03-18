-- =====================================================
-- FacturApp · Datos de Demostración
-- Se inserta solo si la BD está vacía al arrancar
-- Contraseña de todos los usuarios: admin123
-- Hash SHA-256 de "admin123":
-- 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
-- =====================================================

-- === USUARIOS ===
INSERT INTO usuarios (username, password_hash, email, rol, activo)
VALUES
    ('admin',  '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
               'admin@facturapp.com', 'ADMIN',   TRUE),
    ('usuario', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
               'usuario@facturapp.com', 'USUARIO', TRUE);

-- === CLIENTES ===
INSERT INTO clientes (nombre, nif_cif, email, telefono, direccion, fecha_creacion)
VALUES
    ('Tech Solutions SL',       'B12345678',
     'contacto@techsolutions.es', '912 345 678',
     'Calle Gran Vía 45, 3º, 28013 Madrid', '2024-01-10'),

    ('María García López',      '12345678A',
     'maria.garcia@email.com',  '687 234 567',
     'Paseo de Gracia 22, 08007 Barcelona', '2024-02-14'),

    ('Construcciones Iberia SA', 'A87654321',
     'info@iberia-const.com',   '954 678 901',
     'Avenida de Andalucía 100, 41013 Sevilla', '2024-03-01');

-- === PRODUCTOS ===
INSERT INTO productos (nombre, descripcion, precio, porcentaje_iva, activo)
VALUES
    ('Consultoría Técnica',
     'Servicio de consultoría y asesoramiento técnico por hora',
     85.00, 21, TRUE),

    ('Desarrollo Web (jornada)',
     'Desarrollo de aplicaciones web, precio por jornada de 8 horas',
     650.00, 21, TRUE),

    ('Licencia Software Anual',
     'Licencia de uso anual de software de gestión empresarial',
     1200.00, 21, TRUE),

    ('Soporte Técnico Mensual',
     'Plan de soporte técnico y mantenimiento mensual',
     150.00, 21, TRUE),

    ('Formación y Capacitación',
     'Sesión de formación presencial o remota (4 horas)',
     320.00, 21, TRUE);

-- === FACTURAS DE EJEMPLO ===
INSERT INTO facturas (numero, cliente_id, fecha_emision, estado, observaciones)
VALUES
    ('FAC-2024-0001', 1, '2024-01-20', 'PAGADA',
     'Pago realizado mediante transferencia bancaria'),
    ('FAC-2024-0002', 2, '2024-02-15', 'PAGADA',  NULL),
    ('FAC-2024-0003', 3, '2024-03-10', 'EMITIDA',
     'Pendiente de pago antes del 30/03/2024'),
    ('FAC-2024-0004', 1, '2024-03-15', 'EMITIDA',  NULL);

-- === LÍNEAS DE FACTURA ===
-- Factura 1 (Tech Solutions)
INSERT INTO lineas_factura (factura_id, producto_id, cantidad, precio_unitario, descuento)
VALUES
    (1, 1, 8,  85.00,  0.00),   -- 8h consultoría
    (1, 4, 1, 150.00,  0.00);   -- soporte mensual

-- Factura 2 (María García)
INSERT INTO lineas_factura (factura_id, producto_id, cantidad, precio_unitario, descuento)
VALUES
    (2, 5, 2, 320.00,  10.00);  -- 2 sesiones formación con 10% dto

-- Factura 3 (Construcciones Iberia)
INSERT INTO lineas_factura (factura_id, producto_id, cantidad, precio_unitario, descuento)
VALUES
    (3, 2, 5, 650.00, 0.00),    -- 5 jornadas desarrollo
    (3, 1, 4,  85.00, 0.00);    -- 4h consultoría

-- Factura 4 (Tech Solutions)
INSERT INTO lineas_factura (factura_id, producto_id, cantidad, precio_unitario, descuento)
VALUES
    (4, 3, 1, 1200.00, 5.00),   -- licencia anual con 5% dto
    (4, 4, 1,  150.00, 0.00);   -- soporte mensual
