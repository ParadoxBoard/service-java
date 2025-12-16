# 🚀 ParadoxBoard - Guía de Integración Frontend

---


**Después del setup:**
```
Commit en GitHub → Webhook Java → Notifica C# → WebSocket → UI actualiza (< 1s) ⚡
```

---

## 📋 **ÍNDICE**

1. [Instalación de GitHub App](#instalación-de-github-app)
2. [Componentes Frontend (Next.js)](#componentes-frontend-nextjs)
3. [API Endpoints de Java](#api-endpoints-de-java)
4. [Endpoint C# para Notificaciones](#endpoint-c-para-notificaciones)
5. [Flujo Completo](#flujo-completo)

---

## 🔧 **INSTALACIÓN DE ParadoxBoard APP**

### **Lógica del Botón de Instalación:**

#### **1. Hook para verificar instalación:**

```typescript
// hooks/useInstallation.ts
import useSWR from 'swr';

export function useInstallation() {
  const { data: installation, isLoading } = useSWR('/api/user/installation');
  
  return {
    hasInstallation: !!installation,
    installation,
    isLoading
  };
}
```

#### **2. Flujo cuando hace click en "Install App":**

```typescript
// components/InstallAppDialog.tsx (extracto)

const installApp = () => {
  // PASO 1: Redirigir a GitHub para instalar la app
  const githubAppUrl = 'https://github.com/apps/paradoxboard/installations/new';
  
  // PASO 2: GitHub mostrará pantalla de instalación donde el usuario:
  //   - Selecciona organización/cuenta personal
  //   - Selecciona repos (All o Select)
  //   - Click "Install"
  
  window.location.href = githubAppUrl;
  
  // PASO 3: GitHub redirige AUTOMÁTICAMENTE a tu app después de instalar
  // URL de callback: https://paradoxboard.com/dashboard/installation-callback
};
```

#### **3. Página de Callback (después de instalar) para redirigir:**

**Crear esta página en Next.js:**

```typescript
// app/dashboard/installation-callback/page.tsx
'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Loader2, CheckCircle2, XCircle } from 'lucide-react';

export default function InstallationCallbackPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');

  useEffect(() => {
    const processInstallation = async () => {
      try {
        // GitHub envía estos query params después de instalar:
        const installationId = searchParams.get('installation_id');
        const setupAction = searchParams.get('setup_action'); // 'install' o 'update'

        if (!installationId) {
          throw new Error('Installation ID not found');
        }

        console.log('Installation received:', { installationId, setupAction });

        // IMPORTANTE: Java Service ya guardó la instalación vía webhook
        // Solo esperamos un momento para que el webhook se procese
        await new Promise(resolve => setTimeout(resolve, 2000));

        // Revalidar cache de SWR para obtener nueva instalación
        await fetch('/api/user/installation', { 
          cache: 'no-store' 
        });

        setStatus('success');

        // Redirigir al dashboard después de 2 segundos
        setTimeout(() => {
          router.push('/dashboard');
        }, 2000);

      } catch (error) {
        console.error('Error processing installation:', error);
        setStatus('error');
      }
    };

    processInstallation();
  }, [searchParams, router]);

  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="text-center space-y-4">
        {status === 'loading' && (
          <>
            <Loader2 className="h-12 w-12 animate-spin mx-auto text-primary" />
            <h2 className="text-2xl font-bold">Setting up your installation...</h2>
            <p className="text-muted-foreground">
              Configuring ParadoxBoard GitHub App
            </p>
          </>
        )}

        {status === 'success' && (
          <>
            <CheckCircle2 className="h-12 w-12 mx-auto text-green-500" />
            <h2 className="text-2xl font-bold text-green-600">Installation Successful!</h2>
            <p className="text-muted-foreground">
              Redirecting to dashboard...
            </p>
          </>
        )}

        {status === 'error' && (
          <>
            <XCircle className="h-12 w-12 mx-auto text-red-500" />
            <h2 className="text-2xl font-bold text-red-600">Installation Failed</h2>
            <p className="text-muted-foreground">
              Please try again or contact support
            </p>
            <button 
              onClick={() => router.push('/dashboard')}
              className="mt-4 px-4 py-2 bg-primary text-white rounded"
            >
              Go to Dashboard
            </button>
          </>
        )}
      </div>
    </div>
  );
}
```

#### **4. Configurar Setup URL en GitHub App:**

```
https://paradoxboard.com/dashboard/installation-callback
```

**Esto hace que GitHub redirija automáticamente después de instalar.**

---

### **Flujo Completo con Redirecciones:**

```
┌─────────────────────────────────────────────────────────┐
│ Usuario click "Install GitHub App"                      │
└────────────┬────────────────────────────────────────────┘
             │
             │ window.location.href = 'github.com/apps/...'
             │
┌────────────▼────────────────────────────────────────────┐
│ GitHub muestra pantalla de instalación:                │
│ ┌─────────────────────────────────────┐                │
│ │ Install ParadoxBoard                │                │
│ │ Where: ○ Personal  ● Organization   │                │
│ │ Repos: ● All  ○ Select              │                │
│ │ [Install]                           │                │
│ └─────────────────────────────────────┘                │
└────────────┬────────────────────────────────────────────┘
             │
             │ Usuario click [Install]
             │
┌────────────▼────────────────────────────────────────────┐
│ GitHub:                                                  │
│ 1. Crea installation (installation_id: 97878002)        │
│ 2. Envía webhook a Java:                                │
│    POST /webhooks/github                                │
│    Event: "installation.created"                        │
│ 3. Java guarda en BD                                    │
└────────────┬────────────────────────────────────────────┘
             │
             │ GitHub REDIRIGE automáticamente
             │
┌────────────▼────────────────────────────────────────────┐
│ https://paradoxboard.com/dashboard/installation-callback│
│ ?installation_id=97878002&setup_action=install          │
└────────────┬────────────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────────────┐
│ Frontend (InstallationCallbackPage):                    │
│ 1. Obtiene installation_id de URL                       │
│ 2. Espera 2s para que webhook se procese               │
│ 3. Revalida /api/user/installation                     │
│ 4. Muestra "✅ Installation Successful!"                │
│ 5. Redirige a /dashboard después de 2s                 │
└────────────┬────────────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────────────┐
│ Dashboard:                                               │
│ - useInstallation() ahora devuelve la instalación      │
│ - Botón cambia a "Connect Repository"                  │
│ - Muestra selector de repos                            │
└──────────────────────────────────────────────────────────┘
```

---


## 💻 **COMPONENTES FRONTEND (Next.js)**

### **1. Botón de Conectar Repositorio **

```typescript
// components/ConnectRepoButton.tsx
import { useState } from 'react';
import { useInstallation } from '@/hooks/useInstallation';

export function ConnectRepoButton({ boardId }) {
  const [showDialog, setShowDialog] = useState(false);
  const { hasInstallation, installation, isLoading } = useInstallation();

  if (isLoading) return <Button disabled>Loading...</Button>;

  // Lógica principal: ¿Tiene instalación?
  const DialogComponent = hasInstallation ? RepoSelectorDialog : InstallAppDialog;
  
  return (
    <>
      <Button onClick={() => setShowDialog(true)}>
        Connect Repository
      </Button>
      <DialogComponent 
        boardId={boardId}
        installationId={installation?.installationId}
        open={showDialog}
        onClose={() => setShowDialog(false)}
      />
    </>
  );
}
```

**Lógica:**
- `GET /api/user/installation`
- Si `null` → InstallAppDialog (pide instalar)
- Si existe → RepoSelectorDialog (lista repos)
        open={showDialog}
        onClose={() => setShowDialog(false)}
      />
    </>
  );
}
```

---

### **2. Modal de Instalación (redirige a GitHub)**

```typescript
// components/InstallAppDialog.tsx
export function InstallAppDialog({ open, onClose }) {
  const installApp = () => {
    // Redirigir a GitHub para instalar
    window.location.href = 'https://github.com/apps/paradoxboard/installations/new';
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Install GitHub App</DialogTitle>
          <DialogDescription>
            Connect repositories to enable real-time sync
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-2 text-sm">
          <p>✅ Sync commits automatically</p>
          <p>✅ Monitor Pull Requests & Issues</p>
          <p>✅ Real-time task updates</p>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button onClick={installApp}>Install App</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
```

**Acción:**
- Click "Install App" → `window.location.href = 'github.com/apps/paradoxboard/installations/new'`
- GitHub muestra pantalla de instalación
- Usuario selecciona org/repos → Click "Install"
- GitHub redirige a callback
**Acción:**
- Click "Install App" → `window.location.href = 'github.com/apps/paradoxboard/installations/new'`
- GitHub muestra pantalla de instalación
- Usuario selecciona org/repos → Click "Install"
- GitHub redirige a callback

---

### **3. Selector de Repositorios (lista repos disponibles)**

```typescript
// components/RepoSelectorDialog.tsx
import { useState } from 'react';
import useSWR from 'swr';

export function RepoSelectorDialog({ boardId, installationId, open, onClose }) {
  const [selectedRepos, setSelectedRepos] = useState([]);
  
  // Obtener repos desde Java Service
  const { data: repos, isLoading } = useSWR(
    open ? `/api/installations/${installationId}/repos` : null
  );

  const connectRepos = async () => {
    // POST a C# Service para vincular repos al board
    const response = await fetch(`/api/boards/${boardId}/repos`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ repoIds: selectedRepos }),
    });

    if (response.ok) {
      toast.success('Repositories connected!');
      onClose();
    }
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Select Repositories</DialogTitle>
        </DialogHeader>

        {isLoading ? (
          <Loader />
        ) : (
          <ScrollArea>
            {repos?.map(repo => (
              <Checkbox
                key={repo.id}
                label={repo.fullName}
                checked={selectedRepos.includes(repo.id)}
                onChange={() => toggleRepo(repo.id)}
              />
            ))}
          </ScrollArea>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button onClick={connectRepos} disabled={selectedRepos.length === 0}>
            Connect ({selectedRepos.length})
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
```

**Lógica:**
- `GET /api/installations/{installationId}/repos` → Lista repos
- Usuario selecciona repos
- `POST /api/boards/{boardId}/repos` → Vincula al board (C# Service)

---

### **4. Banner Condicional (Dashboard)**

```typescript
// components/InstallationBanner.tsx
import { useInstallation } from '@/hooks/useInstallation';

export function InstallationBanner() {
  const { hasInstallation, isLoading } = useInstallation();

  if (isLoading || hasInstallation) return null;

  const installApp = () => {
    window.location.href = 'https://github.com/apps/paradoxboard/installations/new';
  };

  return (
    <Alert>
      <AlertCircle />
      <AlertTitle>GitHub App Required</AlertTitle>
      <AlertDescription>
        To connect repositories, please install the ParadoxBoard GitHub App.
        <Button onClick={installApp} size="sm">Install Now</Button>
      </AlertDescription>
    </Alert>
  );
}
```

**Muestra banner solo si NO tiene instalación.**

---

## 📡 **API ENDPOINTS DE JAVA**

### **Base URL:** `http://116.202.108.237:8080`

### **1. Verificar Instalación:**

```typescript
// GET /api/user/installation
const response = await fetch('/api/user/installation', {
  headers: {
    'Authorization': `Bearer ${userToken}`
  }
});

// Response: InstallationResponse | null
{
  "id": "uuid",
  "installationId": 97878002,
  "accountType": "Organization",
  "accountLogin": "ParadoxBoard",
  "repositoryCount": 5,
  "active": true,
  "createdAt": "2025-12-14T20:00:00Z",
  "updatedAt": "2025-12-14T20:00:00Z"
}
```

### **2. Listar Repositorios de Instalación:**

```typescript
// GET /api/installations/{installationId}/repos
const response = await fetch(`/api/installations/${installationId}/repos`, {
  headers: {
    'Authorization': `Bearer ${userToken}`
  }
});

// Response: Repository[]
[
  {
    "id": "uuid",
    "fullName": "ParadoxBoard/service-java",
    "name": "service-java",
    "description": "Java Backend Service",
    "private": false,
    "stars": 10,
    "forks": 2,
    "htmlUrl": "https://github.com/ParadoxBoard/service-java"
  }
]
```

### **3. Verificación Rápida (Boolean):**

```typescript
// GET /api/user/has-installation
const response = await fetch('/api/user/has-installation', {
  headers: {
    'Authorization': `Bearer ${userToken}`
  }
});

// Response: boolean
true
```

---

## 🔔 **ENDPOINT C# PARA NOTIFICACIONES**

### **Endpoint que debe implementar C# Service:**

```csharp
// Controllers/TasksController.cs
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.SignalR;

[ApiController]
[Route("api/tasks")]
public class TasksController : ControllerBase
{
    private readonly ITaskService _taskService;
    private readonly IHubContext<TaskHub> _taskHub;
    private readonly ILogger<TasksController> _logger;

    public TasksController(
        ITaskService taskService, 
        IHubContext<TaskHub> taskHub,
        ILogger<TasksController> logger)
    {
        _taskService = taskService;
        _taskHub = taskHub;
        _logger = logger;
    }

    /// <summary>
    /// Endpoint para recibir notificaciones de Java Service
    /// </summary>
    [HttpPost("sync")]
    public async Task<IActionResult> SyncTasks([FromBody] GitHubEventDto githubEvent)
    {
        _logger.LogInformation(
            "Received GitHub event: {Event} for repo {RepoId}", 
            githubEvent.Event, 
            githubEvent.RepoId
        );

        try
        {
            // 1. Parsear tipo de evento
            var (eventType, action) = ParseEvent(githubEvent.Event);

            // 2. Buscar tareas relacionadas
            List<Task> affectedTasks = eventType switch
            {
                "commit" => await FindTasksByCommitMessage(
                    githubEvent.Message, 
                    githubEvent.RepoId
                ),
                "pull_request" => await FindTasksByPR(
                    githubEvent.PrNumber, 
                    githubEvent.RepoId
                ),
                "issue" => await FindTasksByIssue(
                    githubEvent.IssueNumber, 
                    githubEvent.RepoId
                ),
                _ => new List<Task>()
            };

            // 3. Actualizar estado de tareas
            foreach (var task in affectedTasks)
            {
                await UpdateTaskStatus(task, githubEvent, action);
            }

            // 4. Emitir evento WebSocket a Frontend
            await _taskHub.Clients.All.SendAsync("TasksUpdated", new
            {
                TaskIds = affectedTasks.Select(t => t.Id),
                Event = githubEvent.Event,
                Timestamp = DateTime.UtcNow
            });

            _logger.LogInformation(
                "Successfully updated {Count} tasks", 
                affectedTasks.Count
            );

            return Ok(new
            {
                Success = true,
                TasksAffected = affectedTasks.Count,
                Message = $"Successfully processed {githubEvent.Event}"
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing GitHub event");
            return StatusCode(500, new
            {
                Success = false,
                Message = ex.Message
            });
        }
    }

    /// <summary>
    /// Busca tareas por mensaje de commit (usando regex #TASK-123)
    /// </summary>
    private async Task<List<Task>> FindTasksByCommitMessage(string message, string repoId)
    {
        // Regex para encontrar referencias a tareas: #TASK-123
        var regex = new Regex(@"#TASK-(\d+)", RegexOptions.IgnoreCase);
        var matches = regex.Matches(message);

        if (!matches.Any()) return new List<Task>();

        var taskIds = matches.Select(m => m.Groups[1].Value).ToList();
        
        return await _taskService.FindTasksByIdsAndRepo(taskIds, repoId);
    }

    /// <summary>
    /// Busca tareas por número de PR
    /// </summary>
    private async Task<List<Task>> FindTasksByPR(int? prNumber, string repoId)
    {
        if (!prNumber.HasValue) return new List<Task>();
        
        return await _taskService.FindTasksByPRNumber(prNumber.Value, repoId);
    }

    /// <summary>
    /// Busca tareas por número de issue
    /// </summary>
    private async Task<List<Task>> FindTasksByIssue(int? issueNumber, string repoId)
    {
        if (!issueNumber.HasValue) return new List<Task>();
        
        return await _taskService.FindTasksByIssueNumber(issueNumber.Value, repoId);
    }

    /// <summary>
    /// Actualiza el estado de una tarea según el evento
    /// </summary>
    private async Task UpdateTaskStatus(Task task, GitHubEventDto githubEvent, string action)
    {
        switch (action)
        {
            case "commit.created":
                // Commit creado -> Marcar tarea como "Done"
                task.Status = TaskStatus.Done;
                task.CompletedAt = DateTime.UtcNow;
                task.CompletedBy = githubEvent.Author;
                task.RelatedCommitSha = githubEvent.CommitSha;
                break;

            case "pull_request.opened":
                // PR abierto -> Marcar como "In Review"
                task.Status = TaskStatus.InReview;
                task.PullRequestNumber = githubEvent.PrNumber;
                break;

            case "pull_request.merged":
                // PR mergeado -> Marcar como "Done"
                task.Status = TaskStatus.Done;
                task.CompletedAt = DateTime.UtcNow;
                task.PullRequestNumber = githubEvent.PrNumber;
                break;

            case "issue.closed":
                // Issue cerrado -> Marcar como "Done"
                task.Status = TaskStatus.Done;
                task.CompletedAt = DateTime.UtcNow;
                break;
        }

        await _taskService.UpdateTask(task);
    }

    /// <summary>
    /// Parsea el evento de GitHub
    /// </summary>
    private (string eventType, string action) ParseEvent(string eventString)
    {
        var parts = eventString.Split('.');
        return parts.Length == 2 
            ? (parts[0], eventString) 
            : (eventString, eventString);
    }
}

/// <summary>
/// DTO para eventos de GitHub desde Java Service
/// </summary>
public class GitHubEventDto
{
    public string Event { get; set; }          // "commit.created", "pull_request.opened", etc.
    public string RepoId { get; set; }         // UUID del repositorio
    public string? CommitSha { get; set; }     // SHA del commit (si es commit)
    public string? Message { get; set; }       // Mensaje del commit (si es commit)
    public string? Author { get; set; }        // Autor del commit
    public int? PrNumber { get; set; }         // Número de PR (si es PR)
    public int? IssueNumber { get; set; }      // Número de issue (si es issue)
    public string? State { get; set; }         // Estado: "open", "closed", "merged"
    public string? BranchName { get; set; }    // Nombre del branch (si es branch)
    public string? Sha { get; set; }           // SHA del branch
    public long Timestamp { get; set; }        // Timestamp del evento
}
```

---

## 🔄 **FLUJO COMPLETO**

### **1. Primera Vez (Sin App Instalada):**

```
Usuario crea tablero
  ↓
Click "Connect Repository"
  ↓
Frontend: GET /api/user/installation
  ↓
Response: null
  ↓
Mostrar: "Install GitHub App" [Modal]
  ↓
Usuario click "Install"
  ↓
Redirige a: github.com/apps/paradoxboard/installations/new
  ↓
Usuario selecciona org/repos y acepta
  ↓
GitHub envía webhook "installation.created"
  ↓
Java guarda en BD (installation_id: 97878002)
  ↓
GitHub redirige: paradoxboard.com/dashboard?installation_id=97878002
  ↓
Frontend muestra selector de repos
  ↓
Usuario selecciona repos y click "Connect"
  ↓
POST /api/boards/{boardId}/repos (C# Service)
  ↓
✅ Tablero vinculado a repos
```

---

### **2. Segunda Vez (Ya Tiene App):**

```
Usuario crea OTRO tablero
  ↓
Click "Connect Repository"
  ↓
Frontend: GET /api/user/installation
  ↓
Response: { installationId: 97878002, ... }
  ↓
Mostrar: Selector de repos DIRECTAMENTE
  ↓
Usuario selecciona repos y conecta
  ↓
✅ Listo (sin reinstalar)
```

---

### **3. Tiempo Real (Commit → Frontend):**

```
Commit en GitHub
  ↓
Webhook a Java: POST /webhooks/github
  ↓
Java guarda en BD (tabla commits)
  ↓
Java notifica: POST http://csharp:5000/api/tasks/sync
  {
    "event": "commit.created",
    "repoId": "uuid",
    "commitSha": "abc123",
    "message": "Fix bug #TASK-123",
    "author": "adrian"
  }
  ↓
C# busca tarea TASK-123
  ↓
C# actualiza: status = "Done"
  ↓
C# emite WebSocket: TaskHub.Clients.All.SendAsync("TaskUpdated")
  ↓
Frontend recibe evento:
  connection.on("TaskUpdated", (data) => {
    setTasks(prev => updateTask(prev, data));
  });
  ↓
✅ UI actualiza AUTOMÁTICAMENTE (< 1 segundo)
```

---

## 📦 **DEPENDENCIAS NECESARIAS**

### **Frontend (Next.js):**

```bash
npm install swr @microsoft/signalr sonner
npm install @radix-ui/react-dialog @radix-ui/react-checkbox
npm install lucide-react
```

### **Backend (C#):**

```bash
dotnet add package Microsoft.AspNetCore.SignalR
```

---


### **Simular notificación a C#:**

```bash
curl -X POST http://localhost:4001/api/tasks/sync \
  -H "Content-Type: application/json" \
  -d '{
    "event": "commit.created",
    "repoId": "uuid",
    "commitSha": "abc123",
    "message": "Fix bug #TASK-123",
    "author": "paradoxboard",
    "timestamp": 1702512345678
  }'
```
