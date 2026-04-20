CREATE TABLE fraud_reports (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id     VARCHAR(128) NOT NULL,
    subject_type   VARCHAR(32)  NOT NULL,
    risk_level     VARCHAR(16)  NOT NULL,
    reason         VARCHAR(512) NOT NULL,
    rule_triggered VARCHAR(64)  NOT NULL,
    reviewed       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_fraud_subject     ON fraud_reports (subject_id, created_at);
CREATE INDEX idx_fraud_unreviewed  ON fraud_reports (reviewed) WHERE reviewed = FALSE;
