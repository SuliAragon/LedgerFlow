-- =====================================================
-- FacturApp · Esquema de Base de Datos H2
-- Se ejecuta al iniciar la app (CREATE IF NOT EXISTS)
-- =====================================================

CREATE TABLE IF NOT EXISTS usuarios (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)   NOT NULL UNIQUE,
    password_hash  VARCHAR(255)  NOT NULL,
    email          VARCHAR(100),
    rol            VARCHAR(20)   NOT NULL DEFAULT 'USUARIO',
    activo         BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS clientes (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(150)  NOT NULL,
    nif_cif        VARCHAR(20)   NOT NULL UNIQUE,
    email          VARCHAR(100),
    telefono       VARCHAR(30),
    direccion      VARCHAR(250),
    fecha_creacion DATE          DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS productos (
    id              BIGINT         AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(150)   NOT NULL,
    descripcion     VARCHAR(500),
    precio          DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    porcentaje_iva  INT            NOT NULL DEFAULT 21,
    activo          BOOLEAN        NOT NULL DEFAULT TRUE
);

-- empresa_config debe ir ANTES de facturas (FK empresa_id → empresa_config)
CREATE TABLE IF NOT EXISTS empresa_config (
    id               BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre_empresa   VARCHAR(150),
    nif              VARCHAR(30),
    nombre_emisor    VARCHAR(150),
    cargo            VARCHAR(100),
    telefono         VARCHAR(30),
    email            VARCHAR(100),
    web              VARCHAR(200),
    direccion        VARCHAR(250),
    ciudad           VARCHAR(100),
    codigo_postal    VARCHAR(10),
    provincia        VARCHAR(100),
    cuenta_bancaria  VARCHAR(50),
    notas_pie        VARCHAR(600)
);

CREATE TABLE IF NOT EXISTS facturas (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    numero         VARCHAR(25)   NOT NULL UNIQUE,
    cliente_id     BIGINT        NOT NULL,
    fecha_emision  DATE          NOT NULL DEFAULT CURRENT_DATE,
    estado         VARCHAR(20)   NOT NULL DEFAULT 'BORRADOR',
    observaciones  VARCHAR(500),
    empresa_id     BIGINT,
    CONSTRAINT fk_factura_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_factura_empresa FOREIGN KEY (empresa_id) REFERENCES empresa_config(id)
);

CREATE TABLE IF NOT EXISTS lineas_factura (
    id              BIGINT         AUTO_INCREMENT PRIMARY KEY,
    factura_id      BIGINT         NOT NULL,
    producto_id     BIGINT         NOT NULL,
    cantidad        INT            NOT NULL DEFAULT 1,
    precio_unitario DECIMAL(10, 2) NOT NULL,
    descuento       DECIMAL(5, 2)  NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_linea_factura  FOREIGN KEY (factura_id)  REFERENCES facturas(id)  ON DELETE CASCADE,
    CONSTRAINT fk_linea_producto FOREIGN KEY (producto_id) REFERENCES productos(id)
);

CREATE TABLE IF NOT EXISTS empresa_logos (
    id           BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(150)  NOT NULL,
    ruta_archivo VARCHAR(600)  NOT NULL,
    fecha_subida DATE          DEFAULT CURRENT_DATE,
    activo       BOOLEAN       NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS presupuestos (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    numero         VARCHAR(25)   NOT NULL UNIQUE,
    cliente_id     BIGINT        NOT NULL,
    fecha_emision  DATE          NOT NULL DEFAULT CURRENT_DATE,
    estado         VARCHAR(20)   NOT NULL DEFAULT 'BORRADOR',
    observaciones  VARCHAR(500),
    empresa_id     BIGINT,
    CONSTRAINT fk_pres_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_pres_empresa FOREIGN KEY (empresa_id) REFERENCES empresa_config(id)
);

CREATE TABLE IF NOT EXISTS lineas_presupuesto (
    id               BIGINT         AUTO_INCREMENT PRIMARY KEY,
    presupuesto_id   BIGINT         NOT NULL,
    producto_id      BIGINT         NOT NULL,
    cantidad         INT            NOT NULL DEFAULT 1,
    precio_unitario  DECIMAL(10, 2) NOT NULL,
    descuento        DECIMAL(5, 2)  NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_lp_presupuesto FOREIGN KEY (presupuesto_id) REFERENCES presupuestos(id) ON DELETE CASCADE,
    CONSTRAINT fk_lp_producto    FOREIGN KEY (producto_id)    REFERENCES productos(id)
);

-- Índices para mejorar rendimiento en consultas frecuentes
CREATE INDEX IF NOT EXISTS idx_facturas_cliente      ON facturas(cliente_id);
CREATE INDEX IF NOT EXISTS idx_facturas_fecha        ON facturas(fecha_emision);
CREATE INDEX IF NOT EXISTS idx_lineas_factura_id     ON lineas_factura(factura_id);
CREATE INDEX IF NOT EXISTS idx_presupuestos_cliente  ON presupuestos(cliente_id);
CREATE INDEX IF NOT EXISTS idx_lineas_presupuesto_id ON lineas_presupuesto(presupuesto_id);
