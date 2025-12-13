# ✅ CHECKLIST DE TAREAS - Dev A y Dev B

## 📊 Estado Actual del Proyecto

### **Progreso General:**
- ✅ Autenticación: 90% (Login y Register completos)
- ✅ Webhooks: 80% (PRs e Issues completos, faltan Commits/Branches) 
- ❌ API REST: 0% (Por iniciar)
- ⚠️ Sincronización: 40% (Inicial ok, falta incremental)
- ⚠️ Base de Datos: 70% (Tablas GitHub ok, falta optimizar)

---

## 👤 DEV A - TUS TAREAS PENDIENTES

### **1. AUTENTICACIÓN ✅ COMPLETADO**
- [x] `/auth/github/register` - Endpoint creado
- [x] Callback registro - Maneja installationId
- [x] Guardar instalación - InstallationService implementado
- [x] Sincronización inicial - SyncService funcionando
- [x] Guardar datos GitHub - User + Installation + Repos

**Estado:** 100% ✅

---

### **2. WEBHOOKS ✅ 80% COMPLETADO (Isabella - DEV B)**

#### ✅ Completado por Isabella (DEV B):
- [x] Endpoint `/webhooks/github` creado
- [x] Validar firma HMAC (X-Hub-Signature-256)
- [x] Guardar payload en `webhook_logs`
- [x] Manejar evento `ping`
- [x] Manejar evento `installation` (created/deleted/suspended)
- [x] **Manejar evento `pull_request` (opened, closed, edited, reopened, synchronize)** ✨
- [x] **Manejar evento `issues` (opened, closed, edited, reopened, labeled)** ✨
- [x] **Crear DTOs: `PullRequestEventDTO`, `IssueEventDTO`** ✨
- [x] **Crear Mappers: `PullRequestMapper`, `IssueMapper`** ✨
- [x] **Integrar mappers en WebhookService** ✨
- [x] **Agregar métodos a GitHubApiService: `getPullRequests()`, `getIssues()`** ✨
- [x] **Compilación exitosa sin errores** ✅

**Ver documentación completa en:** `docs/WEBHOOK_MODULE_COMPLETED.md`

#### ⏳ Pendiente (RESPONSABILIDAD DEV A):

**A. Evento `installation_repositories` (ALTA PRIORIDAD)**
- [ ] Procesar acción "added" (repos agregados)
  - [ ] Extraer lista de repos agregados del payload
  - [ ] Sincronizar nuevos repos a BD
  - [ ] Actualizar campo `repository_selection` si cambió
  
- [ ] Procesar acción "removed" (repos removidos)
  - [ ] Extraer lista de repos removidos del payload
  - [ ] Marcar repos como inactivos o eliminarlos de BD
  - [ ] Actualizar logs

**B. Evento `push` (ALTA PRIORIDAD)**
- [ ] Crear entidad `Commit` (ya existe la tabla)
- [ ] Crear `CommitRepository`
- [ ] Implementar `handlePushEvent()` en WebhookService
  - [ ] Extraer commits del payload
  - [ ] Guardar cada commit en tabla `commits`
  - [ ] Vincular commits con `branch_id` y `repo_id`
  - [ ] Guardar metadata: author, message, additions, deletions

**C. Eventos `create` / `delete` (MEDIA PRIORIDAD)**
- [ ] Crear entidad `Branch` (ya existe la tabla)
- [ ] Crear `BranchRepository`
- [ ] Implementar `handleCreateEvent()` en WebhookService
  - [ ] Detectar si es branch o tag
  - [ ] Si es branch: guardarlo en tabla `branches`
  - [ ] Vincular con `repo_id`
  
- [ ] Implementar `handleDeleteEvent()` en WebhookService
  - [ ] Marcar branch como eliminado en BD
  - [ ] Actualizar commits relacionados

**Archivos a modificar:**
```
src/main/java/com/paradox/service_java/
├── model/
│   ├── Commit.java        ← CREAR
│   └── Branch.java        ← CREAR
├── repository/
│   ├── CommitRepository.java   ← CREAR
│   └── BranchRepository.java   ← CREAR
└── service/
    └── WebhookService.java     ← MODIFICAR
```

---

### **3. API REST ❌ 0% - TUS ENDPOINTS**

#### **A. Repositorios**
- [ ] `GET /api/repos/user` - Listar repos del usuario autenticado
  - [ ] Crear `RepoController`
  - [ ] Obtener user desde JWT
  - [ ] Buscar repos por `installation_id` del user
  - [ ] Retornar lista con metadata básica

- [ ] `GET /api/repos/{repoId}` - Detalles de un repositorio
  - [ ] Buscar repo por UUID
  - [ ] Incluir: branches count, commits count, último commit
  - [ ] Retornar 404 si no existe o no tiene acceso

#### **B. Branches**
- [ ] `GET /api/branches/{repoId}` - Listar branches de un repo
  - [ ] Validar que user tenga acceso al repo
  - [ ] Obtener branches de BD
  - [ ] Incluir: último commit, protected, author

- [ ] `GET /api/branch/{branchId}` - Detalles de un branch específico
  - [ ] Buscar branch por UUID
  - [ ] Incluir commits recientes (últimos 10)
  - [ ] Retornar estadísticas (commits count, contributors)

#### **C. Commits**
- [ ] `GET /api/commits?repoId={}&branchId={}&limit={}` - Listar commits
  - [ ] Filtros: repoId, branchId, author, dateFrom, dateTo
  - [ ] Paginación (default 20 por página)
  - [ ] Ordenar por fecha desc
  - [ ] Incluir: author, message, additions, deletions

- [ ] `GET /api/commit/{sha}` - Detalles de un commit específico
  - [ ] Buscar por SHA
  - [ ] Incluir: archivos modificados, diff stats, parent commits
  - [ ] Retornar 404 si no existe

#### **D. Issues (GitHub Issues, no internos)**
- [ ] `GET /api/github/issues?repoId={}` - Listar issues de GitHub
  - [ ] Filtros: state (open/closed), labels, assignee
  - [ ] Obtener de tabla `github_issues`
  - [ ] Paginación

- [ ] `GET /api/github/issue/{number}?repoId={}` - Detalles de issue
  - [ ] Buscar por number y repoId
  - [ ] Incluir: comments count, labels, assignees, milestone

#### **E. Pull Requests**
- [ ] `GET /api/prs?repoId={}` - Listar PRs
  - [ ] Filtros: state (open/closed/merged), author
  - [ ] Obtener de tabla `pull_requests`
  - [ ] Incluir: mergeable, draft, review status

- [ ] `GET /api/pr/{number}?repoId={}` - Detalles de PR
  - [ ] Buscar por number y repoId
  - [ ] Incluir: files changed, additions, deletions, reviews

**Archivos a crear:**
```
src/main/java/com/paradox/service_java/
├── controller/
│   ├── RepoController.java      ← CREAR
│   ├── BranchController.java    ← CREAR
│   ├── CommitController.java    ← CREAR
│   ├── GithubIssueController.java ← CREAR
│   └── PullRequestController.java ← CREAR
├── service/
│   ├── RepoService.java         ← CREAR
│   ├── BranchService.java       ← CREAR
│   ├── CommitService.java       ← CREAR
│   ├── GithubIssueService.java  ← CREAR
│   └── PullRequestService.java  ← CREAR
└── dto/
    ├── RepoResponse.java        ← CREAR
    ├── BranchResponse.java      ← CREAR
    ├── CommitResponse.java      ← CREAR
    └── ... (más DTOs según necesites)
```

---

### **4. SINCRONIZACIÓN ⚠️ 50% COMPLETADO**

#### ✅ Ya Completado:
- [x] Sincronización inicial de repos
- [x] Obtener installation token
- [x] Guardar metadata de repos

#### ⏳ Pendiente:
- [ ] `POST /api/sync/init` - Endpoint manual para iniciar sync
  - [ ] Validar que user tenga permisos
  - [ ] Llamar a `SyncService.syncInitial()`
  - [ ] Retornar resumen de repos sincronizados

- [ ] Mejorar sincronización inicial
  - [ ] Sincronizar branches por defecto (al menos `main`/`master`)
  - [ ] Sincronizar commits recientes (últimos 100)
  - [ ] Guardar configuración de repo (topics, language, etc.)

---

### **5. BASE DE DATOS ✅ 70% COMPLETADO**

#### ✅ Ya Completado:
- [x] Tabla `installations`
- [x] Tabla `repositories`
- [x] Tabla `webhook_logs`
- [x] Entidad `Installation`
- [x] Entidad `Repository`
- [x] Entidad `WebhookLog`
- [x] Repositorios JPA correspondientes

#### ⏳ Pendiente:
- [ ] Crear entidad `Commit` (tabla ya existe)
- [ ] Crear entidad `Branch` (tabla ya existe)
- [ ] Crear `CommitRepository` con queries personalizadas
- [ ] Crear `BranchRepository` con queries personalizadas
- [ ] Optimizar índices si es necesario

---

## 📝 RESUMEN - TUS PRÓXIMAS TAREAS (Orden Recomendado)

### **Semana 1: Webhooks Avanzados**
1. ✅ Crear entidades `Commit` y `Branch`
2. ✅ Crear repositorios JPA
3. ✅ Implementar evento `push` (guardar commits)
4. ✅ Implementar evento `installation_repositories`
5. ✅ Implementar eventos `create`/`delete` (branches)

### **Semana 2: API REST - Parte 1**
1. ✅ Crear `RepoController` + Service (2 endpoints)
2. ✅ Crear `BranchController` + Service (2 endpoints)
3. ✅ Crear `CommitController` + Service (2 endpoints)

### **Semana 3: API REST - Parte 2**
1. ✅ Crear `GithubIssueController` + Service (2 endpoints)
2. ✅ Crear `PullRequestController` + Service (2 endpoints)
3. ✅ Testing completo de todos los endpoints

---

---

## 👤 DEV B (ISABELLA) - TAREAS PENDIENTES

### **1. AUTENTICACIÓN ✅ COMPLETADO**
- [x] `/auth/github/login` - OAuth flow
- [x] Callback login - Maneja code
- [x] Obtener access_token de GitHub
- [x] Generar JWT interno
- [x] Obtener datos usuario
- [x] Guardar/actualizar user en BD

**Estado:** 100% ✅

---

### **2. WEBHOOKS ❌ 0% - TU RESPONSABILIDAD**

#### **A. Evento `pull_request` (ALTA PRIORIDAD)**
- [ ] Crear entidad `PullRequest` (tabla ya existe)
- [ ] Crear `PullRequestRepository`
- [ ] Implementar `handlePullRequestEvent()` en WebhookService
  - [ ] Acciones a manejar: `opened`, `closed`, `merged`, `reopened`, `synchronize`
  - [ ] Extraer datos del PR del payload
  - [ ] Guardar/actualizar en tabla `pull_requests`
  - [ ] Campos importantes: number, state, title, body, head/base refs, merged, draft
  - [ ] Vincular con `repo_id`

**B. Evento `issues` (ALTA PRIORIDAD)**
- [ ] Crear entidad `GithubIssue` (tabla ya existe)
- [ ] Crear `GithubIssueRepository`
- [ ] Implementar `handleIssuesEvent()` en WebhookService
  - [ ] Acciones a manejar: `opened`, `closed`, `reopened`, `edited`, `assigned`, `labeled`
  - [ ] Extraer datos del issue del payload
  - [ ] Guardar/actualizar en tabla `github_issues`
  - [ ] Campos importantes: number, state, title, body, labels, assignees, comments_count
  - [ ] Vincular con `repo_id`

**C. Enriquecimiento de Payloads (MEDIA PRIORIDAD)**
- [ ] Crear DTOs específicos para eventos
  - [ ] `PullRequestEventDTO`
  - [ ] `IssueEventDTO`
  - [ ] Incluir: usuarios, labels, estado, metadata

- [ ] Crear mappers: GitHub Payload → Modelo Interno
  - [ ] `PullRequestMapper`: JSON → PullRequest entity
  - [ ] `IssueMapper`: JSON → GithubIssue entity
  - [ ] Extraer y normalizar datos (fechas, usuarios, etc.)

**D. Sincronización Incremental (MEDIA PRIORIDAD)**
- [ ] Crear `IncrementalSyncService`
- [ ] Detectar cambios desde último sync
  - [ ] Por repo: últimos commits, PRs, issues
  - [ ] Comparar con BD y actualizar solo diferencias
  
- [ ] Disparar sync automática desde webhooks
  - [ ] Cuando llega `push` → Sincronizar commits del branch
  - [ ] Cuando llega `pull_request` → Sincronizar PR completo
  - [ ] Cuando llega `issues` → Sincronizar issue completo

**Archivos a crear/modificar:**
```
src/main/java/com/paradox/service_java/
├── model/
│   ├── PullRequest.java        ← CREAR
│   └── GithubIssue.java        ← CREAR
├── repository/
│   ├── PullRequestRepository.java   ← CREAR
│   └── GithubIssueRepository.java   ← CREAR
├── service/
│   ├── WebhookService.java          ← MODIFICAR
│   └── IncrementalSyncService.java  ← CREAR
├── dto/webhook/
│   ├── PullRequestEventDTO.java     ← CREAR
│   └── IssueEventDTO.java           ← CREAR
└── mapper/
    ├── PullRequestMapper.java       ← CREAR
    └── IssueMapper.java             ← CREAR
```

---

### **3. API REST ❌ 0% - TUS ENDPOINTS**

#### **A. Repositorios**
- [ ] `GET /api/repos/project/{projectId}` - Repos asociados a un proyecto
  - [ ] Crear `ProjectRepoController`
  - [ ] Buscar repos vinculados a un proyecto interno
  - [ ] Incluir estadísticas: commits count, PRs count, issues count

- [ ] `GET /api/repos/stats` - Estadísticas generales de repos
  - [ ] Total de repos
  - [ ] Repos por lenguaje
  - [ ] Repos públicos vs privados
  - [ ] Actividad reciente (commits últimos 30 días)

#### **B. Branches**
- [ ] `GET /api/branches/changes?repoId={}` - Cambios recientes en branches
  - [ ] Branches con commits nuevos (últimas 24h)
  - [ ] Branches con PRs abiertos
  - [ ] Incluir: último commit, fecha, autor

- [ ] `GET /api/branch/{branchId}/protection` - Configuración de protección
  - [ ] Obtener reglas de protección del branch
  - [ ] Requiere llamar a GitHub API
  - [ ] Cachear resultado

#### **C. Commits**
- [ ] `GET /api/commits/{branchName}?repoId={}` - Commits de un branch
  - [ ] Filtrar por nombre de branch
  - [ ] Incluir diferencias con branch base (si existe PR)
  - [ ] Paginación

- [ ] `GET /api/commit/{sha}/files` - Archivos modificados en commit
  - [ ] Obtener lista de archivos del commit
  - [ ] Incluir: additions, deletions, changes por archivo
  - [ ] Puede requerir llamada a GitHub API

#### **D. Issues**
- [ ] `GET /api/github/issues/labels?repoId={}` - Issues por label
  - [ ] Agrupar issues por labels
  - [ ] Contar issues por label
  - [ ] Filtros: state (open/closed)

- [ ] `GET /api/github/issues/assigned?userId={}` - Issues asignados
  - [ ] Filtrar por usuario asignado
  - [ ] Incluir: repo, estado, prioridad, due date
  - [ ] Ordenar por fecha de creación

#### **E. Pull Requests**
- [ ] `GET /api/prs/open?repoId={}` - PRs abiertos
  - [ ] Solo PRs con state = 'open'
  - [ ] Incluir: author, reviewers, mergeable, draft
  - [ ] Ordenar por fecha de creación desc

- [ ] `GET /api/pr/{number}/reviews?repoId={}` - Reviews de un PR
  - [ ] Obtener reviews del PR
  - [ ] Incluir: reviewer, state (approved/changes_requested), comments
  - [ ] Puede requerir llamada a GitHub API

**Archivos a crear:**
```
src/main/java/com/paradox/service_java/
├── controller/
│   ├── ProjectRepoController.java    ← CREAR
│   ├── BranchAdvancedController.java ← CREAR
│   ├── CommitAdvancedController.java ← CREAR
│   └── ... (controladores avanzados)
├── service/
│   ├── ProjectRepoService.java       ← CREAR
│   ├── RepoStatsService.java         ← CREAR
│   └── ... (servicios correspondientes)
└── dto/
    ├── RepoStatsResponse.java        ← CREAR
    ├── BranchChangesResponse.java    ← CREAR
    └── ... (DTOs para respuestas complejas)
```

---

### **4. SINCRONIZACIÓN INCREMENTAL ❌ 0%**

#### **A. Endpoint de sincronización completa**
- [ ] `POST /api/sync/full` - Sincronizar todo desde GitHub
  - [ ] Crear `IncrementalSyncController`
  - [ ] Validar permisos de admin
  - [ ] Sincronizar por instalación:
    - [ ] Repos nuevos/actualizados
    - [ ] Commits desde última sync
    - [ ] PRs activos
    - [ ] Issues abiertos
  - [ ] Retornar resumen detallado

#### **B. Detección de cambios**
- [ ] Crear `ChangeDetectionService`
- [ ] Implementar lógica de detección:
  - [ ] Comparar timestamps: BD vs GitHub
  - [ ] Identificar repos con cambios
  - [ ] Identificar branches con nuevos commits
  - [ ] Identificar PRs/issues modificados

#### **C. Sincronización selectiva**
- [ ] Sincronizar solo commits nuevos
  - [ ] Obtener SHA del último commit en BD
  - [ ] Llamar a GitHub API: `/repos/{owner}/{repo}/commits?since={sha}`
  - [ ] Guardar solo commits nuevos

- [ ] Sincronizar solo PRs modificados
  - [ ] Filtrar por `updated_at > last_sync_date`
  - [ ] Actualizar solo los que cambiaron

- [ ] Sincronizar solo issues modificados
  - [ ] Filtrar por `updated_at > last_sync_date`
  - [ ] Actualizar solo los que cambiaron

#### **D. Resumen para .NET**
- [ ] Crear DTO `SyncSummaryDTO`
  - [ ] Total de cambios detectados
  - [ ] Cambios por tipo (commits, PRs, issues)
  - [ ] Cambios por repo
  - [ ] Timestamp de sincronización

- [ ] Endpoint para obtener resumen
  - [ ] `GET /api/sync/summary?since={}` 
  - [ ] Retornar cambios desde fecha específica
  - [ ] Formato compatible con backend .NET

---

### **5. BASE DE DATOS ❌ 0% - TU RESPONSABILIDAD**

#### **A. Crear entidades faltantes**
- [ ] `PullRequest.java` (tabla ya existe)
- [ ] `GithubIssue.java` (tabla ya existe)

#### **B. Crear repositorios JPA**
- [ ] `PullRequestRepository` con queries:
  - [ ] `findByRepoIdAndState(UUID repoId, String state)`
  - [ ] `findByRepoIdAndNumber(UUID repoId, Integer number)`
  - [ ] `findOpenPRsByRepoId(UUID repoId)`
  - [ ] `findByAuthorLogin(String authorLogin)`

- [ ] `GithubIssueRepository` con queries:
  - [ ] `findByRepoIdAndState(UUID repoId, String state)`
  - [ ] `findByRepoIdAndNumber(UUID repoId, Integer number)`
  - [ ] `findByLabelsContaining(String label)`
  - [ ] `findByAssigneesContaining(String username)`

#### **C. Mappers GitHub → Interno**
- [ ] `PullRequestMapper.java`
  - [ ] `fromGitHubPayload(JsonNode json)` → PullRequest
  - [ ] `fromGitHubApi(Map<String, Object> data)` → PullRequest
  - [ ] Normalizar campos, extraer datos anidados

- [ ] `IssueMapper.java`
  - [ ] `fromGitHubPayload(JsonNode json)` → GithubIssue
  - [ ] `fromGitHubApi(Map<String, Object> data)` → GithubIssue
  - [ ] Extraer labels, assignees, milestone

---

## 📝 RESUMEN - TAREAS DE ISABELLA (Orden Recomendado)

### **Semana 1: Webhooks Avanzados**
1. ✅ Crear entidades `PullRequest` y `GithubIssue`
2. ✅ Crear repositorios JPA
3. ✅ Implementar evento `pull_request` (todas las acciones)
4. ✅ Implementar evento `issues` (todas las acciones)
5. ✅ Testing de webhooks

### **Semana 2: Enriquecimiento y Mappers**
1. ✅ Crear DTOs de eventos
2. ✅ Crear mappers GitHub → Entidades
3. ✅ Implementar enriquecimiento de payloads
4. ✅ Integrar mappers en WebhookService

### **Semana 3: API REST**
1. ✅ Implementar endpoints de repos avanzados (2)
2. ✅ Implementar endpoints de branches avanzados (2)
3. ✅ Implementar endpoints de commits avanzados (2)
4. ✅ Implementar endpoints de issues avanzados (2)
5. ✅ Implementar endpoints de PRs avanzados (2)

### **Semana 4: Sincronización Incremental**
1. ✅ Crear `IncrementalSyncService`
2. ✅ Implementar detección de cambios
3. ✅ Implementar sincronización selectiva
4. ✅ Crear endpoint `/sync/full`
5. ✅ Crear resumen para .NET

---

## 🎯 COORDINACIÓN ENTRE DEV A Y DEV B

### **Archivos Compartidos (Cuidado con conflictos):**
- `WebhookService.java` - Ambos lo modifican
  - **Dev A:** Eventos push, create, delete, installation_repositories
  - **Dev B:** Eventos pull_request, issues, enriquecimiento

**Recomendación:** 
- Dev A trabaja en `handlePushEvent()`, `handleCreateEvent()`, etc.
- Dev B trabaja en `handlePullRequestEvent()`, `handleIssuesEvent()`, etc.
- Hacer commits frecuentes para evitar conflictos grandes

### **Dependencias:**
- **Dev B** necesita que **Dev A** termine entidades `Commit` y `Branch` para referencias
- **Ambos** pueden trabajar en paralelo en API REST (diferentes controladores)
- **Sincronización incremental (Dev B)** requiere que webhooks básicos (Dev A) estén funcionando

---

## ✅ SIGUIENTE PASO INMEDIATO

### **Dev A (Tú):**
1. Crear entidades `Commit` y `Branch`
2. Crear repositorios JPA
3. Implementar evento `push` en `WebhookService`

### **Dev B (Isabella):**
1. Crear entidades `PullRequest` y `GithubIssue`
2. Crear repositorios JPA
3. Implementar evento `pull_request` en `WebhookService`

**Después de esto, ambos pueden avanzar en paralelo sin bloquearse** ✅

---

## 📞 Comunicación

**Compartir avances:**
- Commits diarios con mensajes claros
- Notificar cuando completen una tarea que desbloquea al otro
- Revisar Pull Requests antes de merge

**Evitar conflictos:**
- No modificar archivos del otro sin coordinarse
- Si necesitan tocar archivo compartido, comunicarse primero
- Trabajar en branches separados: `dev-a-webhooks`, `dev-b-webhooks`

---

¿Están listos para continuar? 🚀

