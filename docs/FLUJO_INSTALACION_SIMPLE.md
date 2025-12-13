# 🎯 FLUJO SIMPLE - Instalación GitHub App

## 📱 Lo que ve el Usuario (Paso a Paso)

### **PASO 1: En tu aplicación web**

```
┌─────────────────────────────────────┐
│   ParadoxBoard - Tu Aplicación      │
│                                     │
│   🚀 Gestiona tus proyectos         │
│       con GitHub                    │
│                                     │
│   [ Conectar con GitHub ]  ←─ Click│
│                                     │
└─────────────────────────────────────┘
```

---

### **PASO 2: GitHub le pide autorización**

```
┌─────────────────────────────────────────────────┐
│  🔐 ParadoxBoard quiere acceder a tus repos    │
│                                                 │
│  Esta app podrá:                                │
│  ✓ Ver tus repositorios                         │
│  ✓ Leer y escribir código                       │
│  ✓ Crear y modificar issues                     │
│  ✓ Gestionar pull requests                      │
│                                                 │
│  📂 ¿Dónde instalar?                            │
│                                                 │
│  ○ Todos los repositorios                       │
│  ● Solo repositorios seleccionados              │
│     ☑ ParadoxBoard/service-java                 │
│     ☑ ParadoxBoard/frontend-app                 │
│     ☐ ParadoxBoard/otro-repo                    │
│                                                 │
│  [Cancelar]  [Instalar]  ←───── Click aquí     │
└─────────────────────────────────────────────────┘
```

---

### **PASO 3: GitHub procesa la instalación**

```
┌─────────────────────────────────────┐
│  ⏳ Instalando ParadoxBoard...      │
│                                     │
│  ✓ Configurando permisos            │
│  ✓ Conectando repositorios          │
│  ✓ Activando webhooks               │
│                                     │
└─────────────────────────────────────┘
```

**GitHub hace internamente:**
1. Crea `installation_id = 97878002`
2. Asocia con la organización "ParadoxBoard"
3. Guarda repos seleccionados
4. Envía webhook a tu servidor ⚡

---

### **PASO 4: Usuario regresa a tu app**

```
URL: https://tu-app.com/callback?installation_id=97878002
                                 ↑
                          GitHub lo agrega automáticamente

┌─────────────────────────────────────┐
│   ✅ Instalación Exitosa            │
│                                     │
│   Configurando tu cuenta...         │
│   ⏳ Sincronizando repositorios     │
│                                     │
└─────────────────────────────────────┘
```

---

### **PASO 5: Usuario ve su dashboard**

```
┌─────────────────────────────────────────────────┐
│   ParadoxBoard Dashboard                        │
│   👤 ParadoxBoard                               │
│                                                 │
│   📂 Tus Repositorios Conectados (2)           │
│                                                 │
│   🟢 service-java                   [Privado]  │
│      ├─ 3 branches                              │
│      ├─ 45 commits                              │
│      └─ 2 PRs abiertos                          │
│                                                 │
│   🟢 frontend-app                   [Público]  │
│      ├─ 2 branches                              │
│      ├─ 23 commits                              │
│      └─ 1 issue abierto                         │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## 🔧 Lo que sucede en el Backend

### **Momento 1: Usuario hace click en "Instalar"**

```
GitHub
  │
  │ Envía Webhook ⚡
  ↓
POST /webhooks/github
Event: installation (created)
  │
  ↓
WebhookService
  ├─ ✅ Valida firma HMAC
  ├─ 💾 Guarda log en BD
  ├─ 🏗️ Crea Installation en BD
  └─ 📝 Log: "Installation 97878002 created"
```

### **Momento 2: Usuario llega a tu app con installation_id**

```
Frontend captura: installation_id=97878002
  │
  ↓
POST /auth/github/register
Body: {"installationId": 97878002}
  │
  ↓
AuthController.registerWithInstallation()
  │
  ├─ 1️⃣ GET GitHub API: /app/installations/97878002
  │     ↓
  │     Info de la instalación (owner, permisos, etc.)
  │
  ├─ 2️⃣ InstallationService.createOrUpdateFromGitHub()
  │     ↓
  │     💾 Guarda/actualiza Installation en BD
  │
  ├─ 3️⃣ SyncService.syncInitial()
  │     ├─ Obtiene installation token
  │     ├─ GET GitHub API: /installation/repositories
  │     └─ 💾 Guarda cada repo en BD
  │
  ├─ 4️⃣ UserService.createOrUpdateFromGithub()
  │     ↓
  │     💾 Crea/actualiza user en BD
  │     💾 Vincula user ↔ installation
  │
  └─ 5️⃣ Retorna JSON:
        {
          "user": {...},
          "installation": {...},
          "syncedRepositories": [...]
        }
```

---

## 🎨 Diagrama Visual Completo

```
    USUARIO                  GITHUB              TU BACKEND           TU FRONTEND
       │                        │                    │                    │
       │  1. "Conectar GitHub"  │                    │                    │
       ├───────────────────────>│                    │                    │
       │                        │                    │                    │
       │  2. Pantalla permisos  │                    │                    │
       │<───────────────────────┤                    │                    │
       │                        │                    │                    │
       │  3. "Instalar"         │                    │                    │
       ├───────────────────────>│                    │                    │
       │                        │                    │                    │
       │                        │  4. Webhook        │                    │
       │                        │  (installation)    │                    │
       │                        ├───────────────────>│                    │
       │                        │                    │                    │
       │                        │               [Guarda Installation]     │
       │                        │               [Guarda repos]            │
       │                        │                    │                    │
       │  5. Redirect con ID    │                    │                    │
       │<───────────────────────┤                    │                    │
       │  (installation_id)     │                    │                    │
       │                        │                    │                    │
       │  6. Abre app                                │                    │
       ├────────────────────────────────────────────────────────────────>│
       │                        │                    │                    │
       │                        │                    │  7. POST /register │
       │                        │                    │<───────────────────┤
       │                        │                    │  {installationId}  │
       │                        │                    │                    │
       │                        │  8. GET /app/...   │                    │
       │                        │<───────────────────┤                    │
       │                        │  (con JWT)         │                    │
       │                        │                    │                    │
       │                        │  9. Installation   │                    │
       │                        │     data           │                    │
       │                        ├───────────────────>│                    │
       │                        │                    │                    │
       │                        │ 10. GET /install/  │                    │
       │                        │     repositories   │                    │
       │                        │<───────────────────┤                    │
       │                        │                    │                    │
       │                        │ 11. Repos data     │                    │
       │                        ├───────────────────>│                    │
       │                        │                    │                    │
       │                        │              [Sincroniza todo]          │
       │                        │              [Crea user]                │
       │                        │                    │                    │
       │                        │                    │ 12. Response       │
       │                        │                    │    + JWT           │
       │                        │                    ├───────────────────>│
       │                        │                    │                    │
       │ 13. Dashboard con repos                     │                    │
       │<────────────────────────────────────────────────────────────────┤
       │                        │                    │                    │
```

---

## 📊 Datos que Fluyen

### **Del Usuario → GitHub:**
```json
{
  "action": "install",
  "repositories": ["service-java", "frontend-app"],
  "permissions": "accepted"
}
```

### **De GitHub → Tu Backend (Webhook):**
```json
{
  "action": "created",
  "installation": {
    "id": 97878002,
    "account": {
      "login": "ParadoxBoard",
      "type": "Organization"
    }
  },
  "repositories": [...]
}
```

### **De GitHub → Tu Backend (API):**
```json
GET /app/installations/97878002
Response:
{
  "id": 97878002,
  "account": {...},
  "permissions": {...},
  "repository_selection": "selected"
}

GET /installation/repositories
Response:
{
  "total_count": 2,
  "repositories": [
    {"id": 123, "name": "service-java", ...},
    {"id": 456, "name": "frontend-app", ...}
  ]
}
```

### **De Tu Backend → Frontend:**
```json
{
  "user": {
    "id": "uuid",
    "username": "ParadoxBoard",
    "githubInstallationId": "97878002"
  },
  "installation": {
    "installationId": 97878002,
    "accountLogin": "ParadoxBoard"
  },
  "syncedRepositories": [
    {
      "id": "uuid",
      "name": "service-java",
      "fullName": "ParadoxBoard/service-java",
      "private": false
    }
  ]
}
```

---

## 🔑 Conceptos Clave

### **Installation ID**
- **Qué es:** ID único de la instalación (ej: 97878002)
- **Quién lo crea:** GitHub
- **Cuándo:** Al instalar la app
- **Para qué:** Identificar esa instalación específica

### **Installation Token**
- **Qué es:** Token temporal de acceso
- **Duración:** 1 hora
- **Para qué:** Acceder a repos de esa instalación
- **Cómo obtenerlo:** Con JWT de tu app

### **Repository Selection**
- **All:** Acceso a todos los repos (presentes y futuros)
- **Selected:** Solo repos específicos que el usuario eligió

---

## ✅ Checklist de Verificación

Para confirmar que la instalación funcionó:

```sql
-- 1. Verificar instalación en BD
SELECT * FROM installations WHERE installation_id = 97878002;

-- 2. Verificar repos sincronizados
SELECT name, full_name FROM repositories 
WHERE installation_id = (
  SELECT id FROM installations WHERE installation_id = 97878002
);

-- 3. Verificar usuario vinculado
SELECT username, github_installation_id FROM users
WHERE github_installation_id = '97878002';
```

**Deberías ver:**
✅ 1 instalación  
✅ N repositorios  
✅ 1 usuario vinculado  

---

## 🎯 Resumen Ultra-Rápido

```
Usuario → "Instalar app" → GitHub
                            ↓
                    Webhook a tu servidor
                            ↓
                    Guarda Installation
                            ↓
Usuario → Regresa a tu app con installation_id
                            ↓
Frontend → POST /auth/github/register
                            ↓
Backend → Sincroniza repos → Crea user
                            ↓
Usuario → Ve dashboard con repos ✅
```

---

¿Necesitas ayuda para implementar el frontend que capture automáticamente el `installation_id`? 🚀

