CREATE TABLE ratings (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id  UUID        NOT NULL,
    from_user_id UUID       NOT NULL,
    to_user_id  UUID        NOT NULL,
    role_target VARCHAR(16) NOT NULL,
    score       INT         NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment     VARCHAR(500),
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    UNIQUE (booking_id, from_user_id)
);

CREATE INDEX idx_ratings_to_user_role ON ratings (to_user_id, role_target);
