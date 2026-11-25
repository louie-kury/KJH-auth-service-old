-- Apply explicitly to the target auth database after review; do not run automatically.
CREATE TABLE signup_idempotency (
    id BIGINT NOT NULL AUTO_INCREMENT,
    key_digest BINARY(32) NOT NULL,
    proof_digest BINARY(32) NOT NULL,
    request_digest BINARY(32) NOT NULL,
    member_uuid CHAR(36) NOT NULL,
    completed_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_signup_idempotency_key UNIQUE (key_digest),
    CONSTRAINT uk_signup_idempotency_proof UNIQUE (proof_digest),
    INDEX idx_signup_idempotency_expires (expires_at)
) ENGINE=InnoDB;
