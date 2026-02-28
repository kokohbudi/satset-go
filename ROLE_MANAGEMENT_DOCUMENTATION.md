# Role Management System Documentation

## Overview
Sistem role management yang komprehensif untuk aplikasi server pulsa UMKM dengan integrasi Keycloak. Sistem ini memungkinkan pengelolaan role dan menu secara dinamis dengan kontrol akses berbasis role.

## Architecture

### Database Entities

#### 1. Menus Entity
- **Purpose**: Menyimpan informasi menu dan permission aplikasi
- **Features**: 
  - Hierarchical structure (parent-child)
  - Soft delete support
  - Auditing fields
  - UUID primary key

#### 2. Roles Entity  
- **Purpose**: Menyimpan informasi role aplikasi
- **Features**:
  - Many-to-many relationship dengan Menus
  - Soft delete support
  - Auditing fields
  - UUID primary key

#### 3. UserRoles Entity
- **Purpose**: Mapping table antara Users dan Roles
- **Features**:
  - Assignment tracking (assignedBy, assignedAt, notes)
  - Auditing fields
  - Composite key (userId + roleId)

### Service Layer

#### 1. MenuService
- CRUD operations untuk menu management
- Default menu initialization
- Menu hierarchy management
- Caching support

#### 2. RoleService
- CRUD operations untuk role management
- Menu assignment to roles
- Default role initialization
- Role validation

#### 3. UserRoleService
- Assign/unassign roles to users
- Permission checking
- Accessible menu retrieval
- Multi-role support per user

#### 4. RoleIntegrationService
- Keycloak-Application synchronization
- Permission validation
- Role-based access control bridge

#### 5. DataInitializationService
- Automatic initialization of default data
- Runs on application startup
- Creates default menus, roles, and assignments

### Controller Layer

#### RoleManagementController
- **Web UI Endpoints**:
  - `/admin/roles` - Role management page
  - `/admin/assign-role` - Role assignment page
  
- **REST API Endpoints**:
  - `GET /api/roles` - Get all roles
  - `POST /api/roles` - Create new role
  - `PUT /api/roles/{id}` - Update role
  - `DELETE /api/roles/{id}` - Delete role
  - `POST /api/roles/{id}/menus` - Assign menus to role
  - `GET /api/users/search` - Search users
  - `POST /api/users/{userId}/roles` - Assign role to user
  - `DELETE /api/users/{userId}/roles/{roleId}` - Remove role from user

### Security Configuration

#### RoleBasedSecurityConfig
- Spring Security configuration
- Role-based access control using SpEL expressions
- Integration with RoleIntegrationService
- Endpoint protection based on roles

### UI Components

#### MenuUtils
- Utility class for Thymeleaf templates
- Role-based menu filtering
- Permission checking helpers
- Access level determination

#### Templates
1. **role-management.html** - Role management interface
2. **assign-role.html** - Role assignment interface
3. **sidebar.html** - Updated with role-based navigation

## Role Structure

### 1. omnip-admin (Super Admin)
- **Description**: Administrator dengan akses penuh
- **Permissions**: 
  - Semua menu management
  - User management
  - Role management
  - System settings

### 2. omnip-operator (Operator)
- **Description**: Operator sistem dengan akses operasional
- **Permissions**:
  - Transaction management
  - Voucher management
  - Price management
  - Customer management

### 3. omnip-store-admin (Store Admin)
- **Description**: Pemilik toko dengan kemampuan manajemen
- **Permissions**:
  - User management (dalam scope toko)
  - Role assignment (untuk sub-users)
  - Price management
  - Transaction management
  - Customer management

### 4. omnip-store-operator (Store Operator)
- **Description**: Operator toko dengan akses terbatas
- **Permissions**:
  - Transaction management (view/create)
  - Customer management (view)

## Default Menu Structure

```
DASHBOARD
├── Transaction Management
├── Voucher Management
├── Price Management
├── Customer Management
├── User Management
├── Role Management
└── System Settings
```

## API Usage Examples

### 1. Create New Role
```http
POST /api/roles
Content-Type: application/json

{
  "roleCode": "omnip-custom-role",
  "roleName": "Custom Role",
  "description": "Custom role description"
}
```

### 2. Assign Menus to Role
```http
POST /api/roles/{roleId}/menus
Content-Type: application/json

{
  "menuIds": ["menu-id-1", "menu-id-2", "menu-id-3"]
}
```

### 3. Assign Role to User
```http
POST /api/users/{userId}/roles
Content-Type: application/json

{
  "roleId": "role-id",
  "notes": "Assignment notes"
}
```

### 4. Search Users
```http
GET /api/users/search?query=john@example.com
```

## Thymeleaf Usage

### Check Menu Access
```html
<div th:if="${@menuUtils.hasMenuAccess(#authentication, 'ROLE_MANAGEMENT')}">
    <!-- Content for users with role management access -->
</div>
```

### Check Role
```html
<div th:if="${@menuUtils.hasRole(#authentication, 'omnip-admin')}">
    <!-- Content for admin users -->
</div>
```

### Check Admin Status
```html
<div th:if="${@menuUtils.isAdmin(#authentication)}">
    <!-- Content for admin users (omnip-admin or omnip-store-admin) -->
</div>
```

### Get User Access Level
```html
<span th:text="${@menuUtils.getUserAccessLevelName(#authentication)}">Access Level</span>
```

## Configuration

### Application Properties
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_URL}/realms/omnip
      client:
        registration:
          keycloak:
            client-id: omnip-client
            client-secret: ${KEYCLOAK_CLIENT_SECRET}
            scope: openid,profile,email,roles
        provider:
          keycloak:
            issuer-uri: ${KEYCLOAK_URL}/realms/omnip
```

### Environment Variables
- `KEYCLOAK_URL`: Keycloak server URL
- `KEYCLOAK_CLIENT_SECRET`: Client secret for Keycloak integration

## Database Migration

### Required Tables
1. `menus` - Menu and permission storage
2. `roles` - Role definitions
3. `user_roles` - User-role mapping
4. `users` - User information (existing)

### Initialization
- Default data automatically created on application startup
- No manual database setup required

## Security Features

### 1. Role-Based Access Control
- Method-level security using `@PreAuthorize`
- URL-level security in Spring Security configuration
- Template-level security using MenuUtils

### 2. Audit Trail
- All entities include audit fields (createdBy, createdAt, updatedBy, updatedAt)
- User role assignments tracked with assignedBy and assignedAt
- Soft delete for data integrity

### 3. Multi-Role Support
- Users can have multiple roles simultaneously
- Permission aggregation from all assigned roles
- Flexible role assignment and removal

## Performance Optimizations

### 1. Caching
- Repository queries cached using Caffeine
- Menu and role data cached for performance
- Cache eviction on data updates

### 2. Lazy Loading
- JPA relationships configured for optimal loading
- Fetch strategies optimized for common use cases

## Testing

### Integration Testing
- Test role assignment and permission checking
- Verify Keycloak integration
- Test UI workflows

### Unit Testing
- Service layer testing
- Repository testing
- Utility class testing

## Deployment Considerations

### 1. Database Setup
- Ensure PostgreSQL is configured
- Run application to auto-create tables and default data

### 2. Keycloak Configuration
- Configure realm and client in Keycloak
- Set up role mappings in Keycloak
- Configure client credentials

### 3. Environment Variables
- Set required environment variables
- Configure application properties for target environment

## Troubleshooting

### Common Issues

1. **Role not found in Keycloak**
   - Ensure role exists in Keycloak realm
   - Check role name spelling and case sensitivity

2. **Permission denied errors**
   - Verify user has required role assigned
   - Check menu permissions in database
   - Verify RoleIntegrationService configuration

3. **Menu not visible in UI**
   - Check if user has menu access permission
   - Verify MenuUtils is properly configured
   - Check Thymeleaf template syntax

### Logging
- Enable DEBUG logging for `com.omnip.services` package
- Monitor RoleIntegrationService logs for synchronization issues
- Check Spring Security logs for authentication issues

## Future Enhancements

1. **Dynamic Menu Creation**: Allow runtime menu creation through UI
2. **Role Templates**: Pre-defined role templates for quick setup
3. **Bulk Operations**: Bulk user role assignment
4. **Audit Dashboard**: UI for viewing audit trails
5. **Permission Matrix**: Visual permission matrix for roles
6. **Role Inheritance**: Hierarchical role structure with inheritance

## Conclusion

Sistem role management ini menyediakan solusi lengkap untuk kontrol akses berbasis role dengan integrasi Keycloak. Sistem ini fleksibel, scalable, dan mudah digunakan dengan UI yang intuitif dan API yang komprehensif.
