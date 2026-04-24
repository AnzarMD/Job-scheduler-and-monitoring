-- V1__init_schema.sql
-- Flyway runs this automatically on first startup.
-- The V1__ prefix = version 1. Next migration would be V2__add_something.sql

-- ─────────────────────────────────────────────────────
-- TABLE: tenants
-- ─────────────────────────────────────────────────────
CREATE TABLE tenants (
                         id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         name        VARCHAR(100) NOT NULL UNIQUE,
                         slug        VARCHAR(50)  NOT NULL UNIQUE,
                         api_key     VARCHAR(64)  NOT NULL UNIQUE,
                         plan        VARCHAR(20)  NOT NULL DEFAULT 'FREE',
                         max_jobs    INTEGER      NOT NULL DEFAULT 10,
                         is_active   BOOLEAN      NOT NULL DEFAULT true,
                         created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                         updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ─────────────────────────────────────────────────────
-- TABLE: users
-- ─────────────────────────────────────────────────────
CREATE TABLE users (
                       id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       tenant_id       UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
                       email           VARCHAR(255) NOT NULL,
                       password_hash   VARCHAR(255) NOT NULL,
                       first_name      VARCHAR(100),
                       last_name       VARCHAR(100),
                       role            VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
                       is_active       BOOLEAN      NOT NULL DEFAULT true,
                       last_login_at   TIMESTAMPTZ,
                       created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       CONSTRAINT uq_users_email_tenant UNIQUE (email, tenant_id)
);

-- ─────────────────────────────────────────────────────
-- TABLE: jobs
-- ─────────────────────────────────────────────────────
CREATE TABLE jobs (
                      id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                      tenant_id             UUID          NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
                      name                  VARCHAR(200)  NOT NULL,
                      description           TEXT,
                      cron_expression       VARCHAR(100)  NOT NULL,
                      timezone              VARCHAR(50)   NOT NULL DEFAULT 'UTC',
                      target_url            VARCHAR(2000) NOT NULL,
                      http_method           VARCHAR(10)   NOT NULL DEFAULT 'POST',
                      request_body          TEXT,
                      request_headers       TEXT,
                      timeout_seconds       INTEGER       NOT NULL DEFAULT 30,
                      retry_limit           INTEGER       NOT NULL DEFAULT 3,
                      retry_delay_seconds   INTEGER       NOT NULL DEFAULT 60,
                      status                VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
                      consecutive_failures  INTEGER       NOT NULL DEFAULT 0,
                      last_executed_at      TIMESTAMPTZ,
                      next_execution_at     TIMESTAMPTZ,
                      created_by            UUID REFERENCES users(id),
                      created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
                      updated_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
                      CONSTRAINT uq_jobs_name_tenant UNIQUE (name, tenant_id)
);

-- ─────────────────────────────────────────────────────
-- TABLE: job_executions
-- ─────────────────────────────────────────────────────
CREATE TABLE job_executions (
                                id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                job_id           UUID        NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
                                tenant_id        UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
                                status           VARCHAR(20) NOT NULL,
                                attempt_number   INTEGER     NOT NULL DEFAULT 1,
                                triggered_by     VARCHAR(20) NOT NULL DEFAULT 'SCHEDULER',
                                started_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                                finished_at      TIMESTAMPTZ,
                                duration_ms      BIGINT,
                                http_status_code INTEGER,
                                response_body    TEXT,
                                error_message    TEXT,
                                created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─────────────────────────────────────────────────────
-- TABLE: alert_configs
-- ─────────────────────────────────────────────────────
CREATE TABLE alert_configs (
                               id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               job_id            UUID        NOT NULL REFERENCES jobs(id) ON DELETE CASCADE UNIQUE,
                               tenant_id         UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
                               failure_threshold INTEGER     NOT NULL DEFAULT 3,
                               webhook_url       VARCHAR(2000),
                               is_enabled        BOOLEAN     NOT NULL DEFAULT true,
                               last_alerted_at   TIMESTAMPTZ,
                               created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─────────────────────────────────────────────────────
-- TABLE: alert_logs
-- ─────────────────────────────────────────────────────
CREATE TABLE alert_logs (
                            id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            job_id       UUID          NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
                            tenant_id    UUID          NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
                            webhook_url  VARCHAR(2000) NOT NULL,
                            payload      TEXT          NOT NULL,
                            http_status  INTEGER,
                            sent_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
                            is_delivered BOOLEAN       NOT NULL DEFAULT false
);

-- ─────────────────────────────────────────────────────
-- INDEXES
-- ─────────────────────────────────────────────────────
CREATE INDEX idx_users_tenant     ON users(tenant_id);
CREATE INDEX idx_users_email      ON users(email);
CREATE INDEX idx_jobs_tenant      ON jobs(tenant_id);
CREATE INDEX idx_jobs_status      ON jobs(status);
CREATE INDEX idx_job_exec_job     ON job_executions(job_id);
CREATE INDEX idx_job_exec_tenant  ON job_executions(tenant_id);
CREATE INDEX idx_job_exec_status  ON job_executions(status);
CREATE INDEX idx_job_exec_started ON job_executions(started_at DESC);
CREATE INDEX idx_alert_logs_job   ON alert_logs(job_id);