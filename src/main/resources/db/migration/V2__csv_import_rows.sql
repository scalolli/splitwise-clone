CREATE TABLE csv_import_rows (
    id          BIGSERIAL PRIMARY KEY,
    import_id   UUID        NOT NULL,
    row_index   INT         NOT NULL,
    date        DATE        NOT NULL,
    description TEXT        NOT NULL,
    amount      DECIMAL(10, 2) NOT NULL,
    payer_id    BIGINT      NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_csv_import_rows_import_id ON csv_import_rows(import_id);
