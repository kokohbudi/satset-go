---
name: thymeleaf-patterns
description: Best practices for Thymeleaf template engine with Spring Boot. Covers layout dialects, fragments, security integration, and utility objects.
---

# Thymeleaf Patterns & Best Practices

This skill defines the standard for building server-side rendered (SSR) frontends using Thymeleaf and Spring Boot.

## 核心 Philosophy

When using Thymeleaf, we aim for the same "Component-Based" reuse we enjoy in React, but achieved via server-side inclusion.

- **Layouts over Includes**: Use `thymeleaf-layout-dialect` (decorator pattern) instead of old-school `th:include` everywhere.
- **Fragments as Components**: Treat `th:fragment` as reusable UI components.
- **Semantic HTML**: Keep templates valid HTML5 that can be previewed in a browser (Natural Templates).
- **Utility-First CSS**: Use Tailwind CSS just like in React apps.

## 1. Project Structure

Standard directory organization for maintainable templates:

```text
src/main/resources/templates/
├── layouts/                # Base layouts (decorator pattern)
│   ├── base.html          # Main application shell
│   └── auth.html          # Login/Register shell
├── components/             # Reusable UI fragments
│   ├── navbar.html
│   ├── footer.html
│   ├── buttons.html       # Parametrized button mixins
│   └── cards.html
├── pages/                  # Page-specific content
│   ├── dashboard/
│   │   └── index.html
│   ├── auth/
│   │   ├── login.html
│   │   └── register.html
│   └── home.html
└── email/                  # Email templates
    └── welcome.html
```

## 2. Layouts (The Decorator Pattern)

Use `nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect` (standard in Spring Boot).

**`layouts/base.html` (The Shell):**
```html
<!DOCTYPE html>
<html lang="en" 
      xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout">
<head>
    <meta charset="UTF-8">
    <title layout:title-pattern="$CONTENT_TITLE - AppName">Home</title>
    <link rel="stylesheet" th:href="@{/css/output.css}" />
</head>
<body class="bg-gray-50 text-gray-900">
    <!-- Shared Header -->
    <div th:replace="~{components/navbar :: navbar}"></div>

    <!-- Page Content Injection -->
    <main layout:fragment="content" class="min-h-screen">
        <!-- Content goes here -->
    </main>

    <!-- Shared Footer -->
    <div th:replace="~{components/footer :: footer}"></div>
    
    <!-- Page Specific Scripts -->
    <th:block layout:fragment="scripts"></th:block>
</body>
</html>
```

**`pages/home.html` (The Page):**
```html
<!DOCTYPE html>
<html layout:decorate="~{layouts/base}"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout">
<head>
    <title>Dashboard</title>
</head>
<body>
    <div layout:fragment="content">
        <h1 class="text-3xl font-bold">Welcome Home</h1>
        <p>This is inserted into the main layout.</p>
        
        <!-- Using a fragment component -->
        <div th:replace="~{components/cards :: info-card(title='Stats', value='100%')}"></div>
    </div>
</body>
</html>
```

## 3. Fragments as Components (Props & Logic)

Create reusable "functions" using parameterized fragments.

**`components/ui.html`:**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>

    <!-- Primary Button Component -->
    <!-- Usage: <div th:replace="~{components/ui :: btn-primary(text='Save', type='submit')}"></div> -->
    <button th:fragment="btn-primary(text, type)" 
            th:type="${type ?: 'button'}"
            class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition">
        <span th:text="${text}">Action</span>
    </button>

    <!-- Alert Component -->
    <div th:fragment="alert(message, type)"
         th:classappend="${type == 'error'} ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'"
         class="p-4 rounded-md border">
        <span th:text="${message}">Alert Message</span>
    </div>

</body>
</html>
```

## 4. Modern Javascript Integration (Alpine.js / HTMX)

Since Thymeleaf is SSR, use lightweight JS specifically designed for HTML-first apps.

- **Alpine.js**: For dropdowns, modals, and local state.
- **HTMX**: For AJAX updates without full refreshes (server logic).

**Example (Dropdown):**
```html
<div x-data="{ open: false }" class="relative">
    <button @click="open = !open">Menu</button>
    <div x-show="open" @click.outside="open = false" class="absolute ...">
        <a th:href="@{/profile}">Profile</a>
    </div>
</div>
```

## 5. Spring Security Integration

Never hide elements with CSS. Use standard security dialect.

```html
<html xmlns:sec="http://www.thymeleaf.org/extras/spring-security">

<!-- Show only if logged in -->
<div sec:authorize="isAuthenticated()">
    Hello, <span sec:authentication="name">User</span>
</div>

<!-- Show if has specific role -->
<button sec:authorize="hasRole('ADMIN')">
    Delete User
</button>
</html>
```

## 6. URLs and Assets

Always use `th:href="@{...}"` to ensure context path is handled correctly.

```html
<!-- Static Resources -->
<link th:href="@{/css/main.css}" rel="stylesheet">
<img th:src="@{/img/logo.png}" alt="Logo">

<!-- Dynamic Links -->
<a th:href="@{/users/{id}(id=${user.id}, action='edit')}">Edit User</a>
<!-- Result: /users/123?action=edit -->
```

## 7. Common Pitfalls to Avoid

1.  **Logic in Views**: Avoid complex `th:if` logic. Calculate booleans in the Controller/DTO.
    *   ❌ Bad: `th:if="${user.orders.size() > 0 && user.active}"`
    *   ✅ Good: `th:if="${user.hasActiveOrders}"` (Logic in Model/DTO)

2.  **String Concatenation**: Use literal substitutions.
    *   ❌ Bad: `th:text="'Welcome ' + ${user.name} + '!'"`
    *   ✅ Good: `th:text="|Welcome ${user.name}!|"`

3.  **Inline Scripts**: Avoid standard `<script>` tags with injected values. prefer `th:data-*` attributes.
    *   ❌ Bad: `var id = [[${id}]];`
    *   ✅ Good: `<div id="app" th:data-id="${id}"></div>` + `app.dataset.id` in JS file.

## Checklist for Thymeleaf Tasks

- [ ] **Structure**: Used Layout Dialect (`layout:decorate`)?
- [ ] **Components**: Refactored repeated UI into `th:fragment`?
- [ ] **Security**: Used `sec:authorize` for sensitivity (not just hidden)?
- [ ] **URLs**: All links use `@{...}`?
- [ ] **Logic**: Complex logic pushed to Java DTOs?
- [ ] **Formatting**: Used `#temporals` for dates and `#numbers` for currency?
