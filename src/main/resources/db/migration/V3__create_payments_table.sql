CREATE TABLE payments (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    invoice_id         BIGINT       NOT NULL,
    payment_date       DATE         NOT NULL,
    amount             DOUBLE       NOT NULL,
    payment_method     VARCHAR(20)  NOT NULL,
    transaction_number VARCHAR(255) NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uq_payments_transaction_number UNIQUE (transaction_number),
    CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
);
