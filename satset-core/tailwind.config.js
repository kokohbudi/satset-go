/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
        "./src/main/resources/templates/**/*.html",
        "./src/main/resources/static/js/**/*.js"
    ],
    theme: {
        extend: {
            colors: {
                // Semantic tokens used by landing page
                'ink':  '#1e293b',
                'mist': '#f8fafc',
                'line': '#e2e8f0',
                'muted': '#475569',
                'faint': '#64748b',
                // Custom orange theme colors
                'brand-orange': {
                    50: '#fff7ed',
                    100: '#ffedd5',
                    200: '#fed7aa',
                    300: '#fdba74',
                    400: '#fb923c',
                    500: '#f97316',
                    600: '#ea580c',
                    700: '#c2410c',
                    800: '#9a3412',
                    900: '#7c2d12',
                }
            },
            fontFamily: {
                sans: ['Inter', 'ui-sans-serif', 'system-ui', '-apple-system', 'sans-serif'],
            },
        },
    },
    plugins: [
        require('daisyui'),
    ],
    daisyui: {
        themes: [
            {
                // Custom light theme with orange primary
                "omnip-light": {
                    "primary": "#ea580c",
                    "primary-focus": "#f97316",
                    "primary-content": "#ffffff",
                    "secondary": "#64748b",
                    "secondary-focus": "#475569",
                    "secondary-content": "#ffffff",
                    "accent": "#f59e0b",
                    "accent-focus": "#d97706",
                    "accent-content": "#ffffff",
                    "neutral": "#1f2937",
                    "neutral-focus": "#111827",
                    "neutral-content": "#ffffff",
                    "base-100": "#ffffff",
                    "base-200": "#f8fafc",
                    "base-300": "#e2e8f0",
                    "base-content": "#1e293b",
                    "info": "#0ea5e9",
                    "success": "#22c55e",
                    "warning": "#f59e0b",
                    "error": "#ef4444",
                },
                // Custom dark theme with orange primary
                "omnip-dark": {
                    "primary": "#ea580c",
                    "primary-focus": "#f97316",
                    "primary-content": "#ffffff",
                    "secondary": "#64748b",
                    "secondary-focus": "#94a3b8",
                    "secondary-content": "#ffffff",
                    "accent": "#f59e0b",
                    "accent-focus": "#fbbf24",
                    "accent-content": "#000000",
                    "neutral": "#1e293b",
                    "neutral-focus": "#0f172a",
                    "neutral-content": "#f1f5f9",
                    "base-100": "#1e293b",
                    "base-200": "#0f172a",
                    "base-300": "#334155",
                    "base-content": "#f1f5f9",
                    "info": "#38bdf8",
                    "success": "#4ade80",
                    "warning": "#fbbf24",
                    "error": "#f87171",
                },
            },
            "autumn",    // Fallback light theme
            "halloween", // Fallback dark theme
        ],
        darkTheme: "omnip-dark",
        base: true,
        styled: true,
        utils: true,
        logs: false,
    },
}
