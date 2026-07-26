CREATE TABLE gateway_release_publication (
    release_id VARCHAR(64) NOT NULL,
    attempt_no INTEGER NOT NULL CHECK (attempt_no > 0),
    phase_order INTEGER NOT NULL CHECK (phase_order >= 0),
    phase_type VARCHAR(32) NOT NULL CHECK (
        phase_type IN ('CHUNK', 'ACTIVATION')
    ),
    config_key VARCHAR(512) NOT NULL,
    content_value TEXT NOT NULL,
    content_sha256 VARCHAR(64) NOT NULL CHECK (
        length(content_sha256) = 64
    ),
    expected_version BIGINT,
    change_id VARCHAR(128) NOT NULL,
    ddc_target_version BIGINT,
    ddc_status VARCHAR(32) NOT NULL CHECK (
        ddc_status IN (
            'PLANNED', 'RESOLVED', 'SUBMITTED', 'SUCCESS', 'FAILED',
            'PARTIAL_SUCCESS', 'TIMEOUT', 'UNKNOWN'
        )
    ),
    error_code VARCHAR(128),
    error_message VARCHAR(1024),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (release_id, attempt_no, phase_order),
    UNIQUE (change_id),
    FOREIGN KEY (release_id, attempt_no)
        REFERENCES gateway_release_attempt(release_id, attempt_no),
    CHECK (ddc_status = 'PLANNED' OR expected_version IS NOT NULL),
    CHECK (ddc_status <> 'SUCCESS' OR ddc_target_version IS NOT NULL)
);

CREATE INDEX idx_gateway_release_publication_incomplete
    ON gateway_release_publication (release_id, attempt_no, phase_order)
    WHERE ddc_status <> 'SUCCESS';
