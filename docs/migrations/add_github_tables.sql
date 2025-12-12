-- ============================================
-- TABLAS PARA INTEGRACIÓN CON GITHUB
-- ============================================

-- IMPORTANTE: Ejecutar TODO el script completo, no línea por línea
-- En DBeaver: Seleccionar todo (Ctrl+A) y ejecutar (Ctrl+Enter)
-- O usar el botón "Execute SQL Script" (Ctrl+Alt+X)

-- ============================================
-- 1. ELIMINAR TABLAS EXISTENTES (si existen)
-- ============================================

-- Orden inverso por dependencias
DROP TABLE IF EXISTS github_issues CASCADE;
DROP TABLE IF EXISTS pull_requests CASCADE;
DROP TABLE IF EXISTS commits CASCADE;
DROP TABLE IF EXISTS branches CASCADE;
DROP TABLE IF EXISTS repositories CASCADE;
DROP TABLE IF EXISTS installations CASCADE;

-- ============================================
-- 2. CREAR TABLAS
-- ============================================

-- Tabla: installations
-- Almacena las instalaciones de GitHub App en organizaciones/usuarios
CREATE TABLE installations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    installation_id BIGINT NOT NULL UNIQUE,
    account_login TEXT NOT NULL,
    account_type TEXT NOT NULL CHECK (account_type IN ('Organization', 'User')),
    account_id BIGINT NOT NULL,
    target_type TEXT CHECK (target_type IN ('Organization', 'User')),
    permissions JSONB DEFAULT '{}'::jsonb,
    events TEXT[] DEFAULT ARRAY[]::TEXT[],
    repository_selection TEXT CHECK (repository_selection IN ('all', 'selected')),
    app_id BIGINT,
    app_slug TEXT,
    suspended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índices para installations
CREATE INDEX installations_installation_id_idx ON installations(installation_id);
CREATE INDEX installations_account_login_idx ON installations(account_login);
CREATE INDEX installations_account_type_idx ON installations(account_type);

-- Tabla: repositories
-- Almacena repositorios vinculados a instalaciones
CREATE TABLE repositories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    installation_id UUID NOT NULL REFERENCES installations(id) ON DELETE CASCADE,
    github_repo_id BIGINT NOT NULL UNIQUE,
    node_id TEXT,
    name TEXT NOT NULL,
    full_name TEXT NOT NULL,
    owner_login TEXT NOT NULL,
    owner_type TEXT,
    private BOOLEAN DEFAULT false,
    description TEXT,
    fork BOOLEAN DEFAULT false,
    html_url TEXT,
    clone_url TEXT,
    ssh_url TEXT,
    default_branch TEXT DEFAULT 'main',
    language TEXT,
    topics TEXT[],
    archived BOOLEAN DEFAULT false,
    disabled BOOLEAN DEFAULT false,
    pushed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índices para repositories
CREATE INDEX repositories_installation_id_idx ON repositories(installation_id);
CREATE INDEX repositories_github_repo_id_idx ON repositories(github_repo_id);
CREATE INDEX repositories_full_name_idx ON repositories(full_name);
CREATE INDEX repositories_owner_login_idx ON repositories(owner_login);

-- Tabla: branches
-- Almacena branches de repositorios
CREATE TABLE branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_id UUID NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    sha TEXT NOT NULL,
    protected BOOLEAN DEFAULT false,
    commit_message TEXT,
    commit_author TEXT,
    commit_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(repo_id, name)
);

-- Índices para branches
CREATE INDEX branches_repo_id_idx ON branches(repo_id);
CREATE INDEX branches_name_idx ON branches(name);
CREATE INDEX branches_sha_idx ON branches(sha);

-- Tabla: commits
-- Almacena commits sincronizados
CREATE TABLE commits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_id UUID NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    branch_id UUID REFERENCES branches(id) ON DELETE SET NULL,
    sha TEXT NOT NULL,
    node_id TEXT,
    message TEXT NOT NULL,
    author_name TEXT,
    author_email TEXT,
    author_login TEXT,
    author_date TIMESTAMPTZ,
    committer_name TEXT,
    committer_email TEXT,
    committer_date TIMESTAMPTZ,
    tree_sha TEXT,
    parent_shas TEXT[],
    additions INT DEFAULT 0,
    deletions INT DEFAULT 0,
    changed_files INT DEFAULT 0,
    html_url TEXT,
    verified BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(repo_id, sha)
);

-- Índices para commits
CREATE INDEX commits_repo_id_idx ON commits(repo_id);
CREATE INDEX commits_branch_id_idx ON commits(branch_id);
CREATE INDEX commits_sha_idx ON commits(sha);
CREATE INDEX commits_author_login_idx ON commits(author_login);
CREATE INDEX commits_author_date_idx ON commits(author_date DESC);

-- Tabla: pull_requests
-- Almacena pull requests
CREATE TABLE pull_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_id UUID NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    github_pr_id BIGINT NOT NULL,
    number INT NOT NULL,
    node_id TEXT,
    state TEXT CHECK (state IN ('open', 'closed', 'merged')),
    title TEXT NOT NULL,
    body TEXT,
    user_login TEXT,
    user_id BIGINT,
    head_ref TEXT,
    head_sha TEXT,
    base_ref TEXT,
    base_sha TEXT,
    draft BOOLEAN DEFAULT false,
    merged BOOLEAN DEFAULT false,
    mergeable BOOLEAN,
    merged_by TEXT,
    merged_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    html_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(repo_id, number)
);

-- Índices para pull_requests
CREATE INDEX pull_requests_repo_id_idx ON pull_requests(repo_id);
CREATE INDEX pull_requests_github_pr_id_idx ON pull_requests(github_pr_id);
CREATE INDEX pull_requests_number_idx ON pull_requests(number);
CREATE INDEX pull_requests_state_idx ON pull_requests(state);
CREATE INDEX pull_requests_user_login_idx ON pull_requests(user_login);

-- Tabla: github_issues
-- Almacena issues de GitHub (diferente de issues internos)
CREATE TABLE github_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_id UUID NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    github_issue_id BIGINT NOT NULL,
    number INT NOT NULL,
    node_id TEXT,
    state TEXT CHECK (state IN ('open', 'closed')),
    title TEXT NOT NULL,
    body TEXT,
    user_login TEXT,
    user_id BIGINT,
    labels TEXT[],
    assignees TEXT[],
    milestone TEXT,
    locked BOOLEAN DEFAULT false,
    comments_count INT DEFAULT 0,
    closed_at TIMESTAMPTZ,
    html_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(repo_id, number)
);

-- Índices para github_issues
CREATE INDEX github_issues_repo_id_idx ON github_issues(repo_id);
CREATE INDEX github_issues_github_issue_id_idx ON github_issues(github_issue_id);
CREATE INDEX github_issues_number_idx ON github_issues(number);
CREATE INDEX github_issues_state_idx ON github_issues(state);
CREATE INDEX github_issues_user_login_idx ON github_issues(user_login);

-- Actualizar tabla webhook_logs para mejorarla
-- Ya existe, pero agregar columnas si no las tiene
ALTER TABLE webhook_logs
ADD COLUMN IF NOT EXISTS event_type TEXT,
ADD COLUMN IF NOT EXISTS delivery_id TEXT UNIQUE,
ADD COLUMN IF NOT EXISTS signature TEXT,
ADD COLUMN IF NOT EXISTS processed BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS processed_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS error_message TEXT;

-- Índices adicionales para webhook_logs
CREATE INDEX IF NOT EXISTS webhook_logs_event_type_idx ON webhook_logs(event_type);
CREATE INDEX IF NOT EXISTS webhook_logs_processed_idx ON webhook_logs(processed);
CREATE INDEX IF NOT EXISTS webhook_logs_created_at_idx ON webhook_logs(created_at DESC);

-- Actualizar tabla users para agregar índice si no existe
CREATE INDEX IF NOT EXISTS users_github_installation_id_idx ON users(github_installation_id);

-- Comentarios para documentación
COMMENT ON TABLE installations IS 'GitHub App installations en organizaciones o usuarios';
COMMENT ON TABLE repositories IS 'Repositorios vinculados a instalaciones de GitHub';
COMMENT ON TABLE branches IS 'Branches de repositorios sincronizados';
COMMENT ON TABLE commits IS 'Commits sincronizados desde GitHub';
COMMENT ON TABLE pull_requests IS 'Pull requests sincronizados desde GitHub';
COMMENT ON TABLE github_issues IS 'Issues de GitHub (diferente de issues internos del tablero)';

