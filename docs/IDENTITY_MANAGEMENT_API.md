# Identity Management API Documentation

API untuk mengelola users, roles, dan groups.

## Base URL

```
/api/idm
```

## Authentication

Semua endpoint memerlukan:
- **Authorization**: Bearer Token (JWT)
- **Required Authority**: `GROUP_bo-admin`
- **Required Role**: `manage_roles` (role/group) atau `manage_users` (user)

---

## Roles & Groups Endpoints

### 1. Get All Groups

```
GET /api/idm/groups
```

**Response:**
```json
[
  { "id": "e02b2d6d-...", "name": "bo-admin" },
  { "id": "bdd3f7bf-...", "name": "bo-operator" }
]
```

---

### 2. Get All Roles

```
GET /api/idm/roles
```

**Response:**
```json
[
  {
    "id": "9f645c50-...",
    "name": "manage_users",
    "description": "view, add, edit, delete",
    "clientRole": true,
    "composite": true,
    "containerId": "7dd46a5e-..."
  }
]
```

---

### 3. Get Roles by Group

```
GET /api/idm/groups/{groupId}/roles
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `groupId` | string | UUID group |

**Response:** List roles assigned to group

---

### 4. Assign Role to Group

```
POST /api/idm/groups/{groupId}/roles/{roleName}
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `groupId` | string | UUID group |
| `roleName` | string | Nama role |

**Response:**
```json
{ "status": "success", "message": "Role '...' assigned to group '...'" }
```

---

### 5. Unassign Role from Group

```
DELETE /api/idm/groups/{groupId}/roles/{roleName}
```

**Response:**
```json
{ "status": "success", "message": "Role '...' removed from group '...'" }
```

---

## User Management Endpoints

> **Required Role**: `manage_users`

### 6. Create User

Membuat user di Keycloak dan database.

```
POST /api/idm/users
```

**Request:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "fullname": "John Doe",
  "password": "securePassword123",
  "roles": ["bo-operator"]
}
```

**Response:**
```json
{
  "status": "success",
  "message": "User created successfully",
  "providerUserId": "uuid-from-keycloak"
}
```

---

### 7. Change Password

```
PUT /api/idm/users/password
```

**Request:**
```json
{
  "email": "john@example.com",
  "password": "newPassword123"
}
```

**Response:**
```json
{ "status": "success", "message": "Password berhasil diubah" }
```

---

### 8. Set User Status

```
PUT /api/idm/users/{email}/status/{status}
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `email` | string | Email user |
| `status` | boolean | `true`=aktif, `false`=nonaktif |

**Response:**
```json
{ "status": "success", "message": "Status pengguna berhasil diubah" }
```

---

## Error Responses

| Code | Description |
|------|-------------|
| 401 | Unauthorized |
| 403 | Forbidden (missing role) |
| 404 | Not Found |
| 500 | Internal Server Error |

---

## Summary

| Method | Endpoint | Role Required |
|--------|----------|---------------|
| GET | `/groups` | manage_roles |
| GET | `/roles` | manage_roles |
| GET | `/groups/{groupId}/roles` | manage_roles |
| POST | `/groups/{groupId}/roles/{roleName}` | manage_roles |
| DELETE | `/groups/{groupId}/roles/{roleName}` | manage_roles |
| POST | `/users` | manage_users |
| PUT | `/users/password` | manage_users |
| PUT | `/users/{email}/status/{status}` | manage_users |

Semua endpoint memerlukan authority `GROUP_bo-admin`.
