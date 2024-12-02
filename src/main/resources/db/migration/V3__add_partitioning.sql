CREATE TABLE courses_partitioned (
                                     id UUID NOT NULL,
                                     title VARCHAR(200) NOT NULL,
                                     description TEXT,
                                     status VARCHAR(50) NOT NULL,
                                     created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                     updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                     version BIGINT NOT NULL
) PARTITION BY RANGE (created_at);


-- kriram particije po mjesecima
CREATE TABLE courses_y2024m01
    PARTITION OF courses_partitioned
        FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE courses_y2024m02
    PARTITION OF courses_partitioned
        FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

-- Kreiramo indekse na particijama
CREATE INDEX idx_courses_y2024m01_created
    ON courses_y2024m01 (created_at);
CREATE INDEX idx_courses_y2024m02_created
    ON courses_y2024m02 (created_at);