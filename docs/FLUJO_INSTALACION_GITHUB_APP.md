# 🔄 FLUJO COMPLETO - Instalación de GitHub App

## 📖 Tabla de Contenidos
1. [Perspectiva del Usuario](#perspectiva-del-usuario)
2. [Qué Sucede en GitHub](#qué-sucede-en-github)
3. [Qué Recibimos Nosotros](#qué-recibimos-nosotros)
4. [Cómo lo Manejamos](#cómo-lo-manejamos)
5. [Flujo Completo Ilustrado](#flujo-completo-ilustrado)

---

## 🎯 Perspectiva del Usuario

### **Escenario 1: Instalación en Organización (Tu caso actual)**

#### **Paso 1: Usuario quiere usar tu app**

El usuario (por ejemplo, un developer de ParadoxBoard) necesita vincular su organización con tu aplicación.

#### **Paso 2: Acceder a la instalación**

**Opción A - Desde tu aplicación web:**
```
Usuario en tu app → Click en "Conectar con GitHub"
                 → Redirige a: https://github.com/apps/paradoxboard/installations/new
```

**Opción B - Directamente desde GitHub:**
```
GitHub → Settings → Applications → GitHub Apps
      → Buscar "ParadoxBoard" 
      → Click en "Install"
```

#### **Paso 3: Pantalla de instalación de GitHub**

GitHub muestra una pantalla como esta:

```
┌─────────────────────────────────────────────────┐
│  ParadoxBoard wants to access your repositories│
│                                                 │
│  ⚠️  This app would like to:                    │
│  ✓ Read access to metadata                     │
│  ✓ Read and write access to code               │
│  ✓ Read and write access to issues             │
│  ✓ Read and write access to pull requests      │
│                                                 │
│  📂 Where should this be installed?             │
│  ○ All repositories                             │
│  ● Only select repositories                     │
│    ☑ service-java                               │
│    ☑ frontend-app                               │
│    ☐ other-repo                                 │
│                                                 │
│  [Cancel]  [Install]                            │
└─────────────────────────────────────────────────┘
```

#### **Paso 4: Usuario hace click en "Install"**

---

### **Escenario 2: Instalación en Repositorio Personal**

Si el usuario tiene repos personales (no en org):

```
Usuario → GitHub → Settings → Applications → GitHub Apps
       → "ParadoxBoard" → Install
       → Selecciona: adr1ann32323/mi-proyecto-personal
       → Install
```

---

## 🔔 Qué Sucede en GitHub

### **1. GitHub crea la instalación**

```
GitHub interno:
  - Genera installation_id: 97878002
  - Asocia con: ParadoxBoard (organization)
  - Permisos: Los que configuraste en la app
  - Repos: Los que el usuario seleccionó
```

### **2. GitHub envía webhook a tu servidor**

**Inmediatamente después de que el usuario clickea "Install":**

```http
POST https://tu-servidor.com/webhooks/github
Content-Type: application/json
X-GitHub-Event: installation
X-Hub-Signature-256: sha256=abc123...
X-GitHub-Delivery: delivery-id-123

{
  "action": "created",
  "installation": {
    "id": 97878002,
    "account": {
      "login": "ParadoxBoard",
      "id": 245963795,
      "type": "Organization"
    },
    "repository_selection": "selected",
    "permissions": {
      "contents": "read",
      "issues": "write",
      "metadata": "read"
    },
    "events": ["push", "pull_request", "issues"]
  },
  "repositories": [
    {
      "id": 123456,
      "name": "service-java",
      "full_name": "ParadoxBoard/service-java"
    }
  ],
  "sender": {
    "login": "adr1ann32323",
    "id": 209031986
  }
}
```

### **3. GitHub redirige al usuario**

Después de instalar, GitHub redirige al usuario a:

```
Opción A - URL que configuraste en la app:
https://tu-app.com/installation-success?installation_id=97878002&setup_action=install

Opción B - Si no configuraste URL:
https://github.com/apps/paradoxboard
```

---

## 📥 Qué Recibimos Nosotros

### **1. Webhook de instalación (Automático)**

**Tu endpoint recibe:**
```
POST /webhooks/github
```

**Tu código procesa:**
```java
@PostMapping("/webhooks/github")
public ResponseEntity<?> handleWebhook(
    @RequestHeader("X-Hub-Signature-256") String signature,
    @RequestHeader("X-GitHub-Event") String eventType, // "installation"
    @RequestHeader("X-GitHub-Delivery") String deliveryId,
    @RequestBody String payload
) {
    // 1. Valida firma HMAC ✅
    webhookService.validateSignature(payload, signature);
    
    // 2. Guarda log en BD ✅
    WebhookLog log = webhookService.saveWebhookLog(...);
    
    // 3. Procesa evento ✅
    webhookService.handleInstallationEvent(payload);
    // → Crea Installation en BD
    // → Guarda repos en BD
}
```

### **2. Datos que extraemos del webhook**

```java
{
  "installation_id": 97878002,
  "account_login": "ParadoxBoard",
  "account_type": "Organization",
  "permissions": {...},
  "events": [...],
  "repositories": [...]
}
```

---

## 🔧 Cómo lo Manejamos

### **Flujo 1: Registro Manual (Tu endpoint actual)**

**Caso de uso:** Usuario ya instaló la app y ahora se registra en tu plataforma.

```
Usuario en tu app → Click "Registrarse con GitHub"
                 ↓
POST /auth/github/register {"installationId": 97878002}
                 ↓
1. AuthController.registerWithInstallation()
   - Obtiene info de instalación desde GitHub API
   - Crea/actualiza Installation en BD
   
2. SyncService.syncInitial()
   - Obtiene token de instalación
   - Llama /installation/repositories
   - Guarda todos los repos en BD
   
3. UserService.createOrUpdateFromGithub()
   - Crea/actualiza usuario
   - Vincula con github_installation_id
   
4. Retorna: user + installation + syncedRepositories
```

**¿De dónde saca el usuario el `installationId`?**

Opción A - Tu frontend lo obtiene de la URL de callback:
```javascript
// Después de instalar, GitHub redirige a:
// https://tu-app.com/callback?installation_id=97878002

const urlParams = new URLSearchParams(window.location.search);
const installationId = urlParams.get('installation_id');

// Frontend envía a backend:
POST /auth/github/register
Body: { installationId: 97878002 }
```

Opción B - Usuario lo encuentra en GitHub:
```
GitHub → Settings → Applications → ParadoxBoard → Ver detalles
Installation ID: 97878002
```

---

### **Flujo 2: Webhook Automático (Ya implementado)**

**Caso de uso:** Sistema detecta automáticamente la instalación.

```
Usuario instala app en GitHub
         ↓ (GitHub envía webhook)
POST /webhooks/github (evento: installation)
         ↓
WebhookService.handleInstallationEvent()
         ↓
InstallationService.createOrUpdateFromGitHub()
         ↓
Installation guardada en BD ✅
```

**Ventaja:** No requiere acción manual del usuario en tu app.

---

## 🎨 Flujo Completo Ilustrado

### **Diagrama de Secuencia**

```
Usuario          GitHub           Tu Backend         Tu Frontend
  |                |                  |                  |
  |--"Install app"->|                 |                  |
  |                |                  |                  |
  |  (Pantalla de permisos)           |                  |
  |                |                  |                  |
  |--"Click Install"|                 |                  |
  |                |                  |                  |
  |                |--Webhook-------->|                  |
  |                |  (installation   |                  |
  |                |   created)       |                  |
  |                |                  |                  |
  |                |            [Guarda Installation]    |
  |                |            [Guarda repos]           |
  |                |                  |                  |
  |<--Redirige-----|                  |                  |
  | (con installation_id)             |                  |
  |                |                  |                  |
  |--Abre tu app------------------------->|               |
  |                |                  |                  |
  |                |                  |<--GET /user-----|
  |                |                  |                  |
  |                |                  |--Verifica------->|
  |                |                  |  installation    |
  |                |                  |<-----------------|
  |                |                  |                  |
  |<---------Dashboard con repos conectados-------------|
```

---

## 📋 Casos de Uso Reales

### **Caso 1: Nueva Organización**

```
1. Admin de "NuevaOrg" instala ParadoxBoard app
   └─> GitHub: installation_id = 99999
   
2. GitHub envía webhook → Tu backend guarda Installation
   
3. Admin entra a tu app
   └─> Frontend detecta: installation_id=99999 en URL
   └─> Frontend llama: POST /auth/github/register {installationId: 99999}
   
4. Backend sincroniza:
   └─> Repos de NuevaOrg
   └─> Crea usuario admin
   └─> Vincula user ↔ installation
   
5. Admin ve dashboard con sus repos listos ✅
```

### **Caso 2: Usuario Personal**

```
1. Developer "john_doe" instala app en su cuenta personal
   └─> GitHub: installation_id = 88888
   
2. GitHub envía webhook → Backend guarda Installation
   
3. john_doe entra a tu app
   └─> Se registra con installation_id=88888
   
4. Backend sincroniza sus repos personales
   
5. john_doe ve sus proyectos personales en tu app ✅
```

### **Caso 3: Agregar más repositorios después**

```
1. Admin va a GitHub → ParadoxBoard app → Configure
   
2. Agrega más repos:
   ☑ nuevo-proyecto
   ☑ api-service
   
3. GitHub envía webhook: "installation_repositories" (added)
   
4. Tu WebhookService.handleInstallationRepositoriesEvent()
   └─> Sincroniza solo los repos nuevos
   
5. Usuario ve los nuevos repos en tu app automáticamente ✅
```

---

## 🔐 Permisos y Acceso

### **Lo que el usuario ve al instalar:**

```
ParadoxBoard necesita acceso a:

✅ Metadata (siempre requerido)
   - Información básica del repo (nombre, descripción)

✅ Contents (read/write)
   - Leer código
   - Crear archivos
   - Modificar archivos

✅ Issues (read/write)
   - Leer issues
   - Crear issues
   - Comentar
   - Cerrar/reabrir

✅ Pull Requests (read/write)
   - Leer PRs
   - Crear PRs
   - Comentar
   - Merge

✅ Webhooks (read/write)
   - Configurar webhooks automáticos
```

### **El usuario puede elegir:**

1. **All repositories** - Acceso a todos (presentes y futuros)
2. **Selected repositories** - Solo repos específicos

---

## 🔄 Eventos que Recibirás

### **Después de la instalación:**

```
installation (created)        → Primera vez que instalan
installation_repositories     → Agregan/remueven repos
push                         → Alguien hace push
pull_request                 → Crean/actualizan PR
issues                       → Crean/modifican issue
create/delete                → Crean/eliminan branch
```

**Todos llegan a:** `POST /webhooks/github`

---

## 💡 Recomendaciones para tu Frontend

### **1. Flujo de instalación sugerido:**

```javascript
// Página: /connect-github

<button onclick="installGitHubApp()">
  Conectar con GitHub
</button>

function installGitHubApp() {
  // Redirige a la página de instalación de tu app
  window.location.href = 
    'https://github.com/apps/paradoxboard/installations/new';
}
```

### **2. Callback después de instalar:**

```javascript
// Página: /auth/callback?installation_id=97878002

useEffect(() => {
  const installationId = new URLSearchParams(window.location.search)
    .get('installation_id');
  
  if (installationId) {
    // Llamar a tu backend
    registerWithInstallation(installationId);
  }
}, []);

async function registerWithInstallation(installationId) {
  const response = await fetch('/auth/github/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ installationId })
  });
  
  const data = await response.json();
  
  // Guardar token JWT
  localStorage.setItem('token', data.token);
  
  // Redirigir a dashboard
  navigate('/dashboard');
}
```

### **3. Mostrar repos conectados:**

```javascript
// Dashboard muestra los repos sincronizados

{data.syncedRepositories.map(repo => (
  <RepoCard key={repo.id}>
    <h3>{repo.fullName}</h3>
    <Badge>{repo.private ? 'Private' : 'Public'}</Badge>
    <button onClick={() => configureRepo(repo.id)}>
      Configurar
    </button>
  </RepoCard>
))}
```

---

## ✅ Resumen del Flujo

### **Vista Rápida:**

```
1. Usuario instala app en GitHub
   ↓
2. GitHub envía webhook → Backend guarda Installation
   ↓
3. GitHub redirige usuario a tu app (con installation_id)
   ↓
4. Frontend captura installation_id de URL
   ↓
5. Frontend llama POST /auth/github/register
   ↓
6. Backend sincroniza repos y crea/actualiza usuario
   ↓
7. Backend retorna JWT + datos
   ↓
8. Frontend guarda token y muestra dashboard ✅
```

### **Dos formas de obtener installation_id:**

**Automática (recomendada):**
- GitHub redirige con `installation_id` en URL
- Frontend lo captura y llama a tu backend

**Manual:**
- Usuario busca el ID en GitHub settings
- Lo copia y pega en tu app

---

## 🎯 Para tu Aplicación

### **Lo que ya tienes funcionando:**

✅ Webhook recibe instalación  
✅ Guarda Installation en BD  
✅ Endpoint de registro manual  
✅ Sincronización de repos  

### **Lo que deberías agregar en el frontend:**

1. Botón "Conectar con GitHub" que redirige a instalación
2. Página de callback que captura `installation_id`
3. Llamada automática a `/auth/github/register`
4. Dashboard que muestra repos sincronizados

---

## 📚 URLs Importantes

```bash
# Tu GitHub App:
https://github.com/apps/paradoxboard

# Página de instalación:
https://github.com/apps/paradoxboard/installations/new

# Configuración de instalación:
https://github.com/organizations/ParadoxBoard/settings/installations/97878002

# Tu webhook endpoint:
https://tu-servidor.com/webhooks/github
```

---

¿Quieres que te ayude a implementar el flujo completo del frontend con la captura automática del `installation_id`? 🚀

