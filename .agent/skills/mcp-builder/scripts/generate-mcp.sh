#!/bin/bash
# Generate MCP config from references
# Usage: ./generate-mcp.sh > .mcp.json
# This script generates .mcp.json with variable placeholders that will be substituted at runtime

REFERENCES_DIR="$(dirname "$0")/../references"

echo "{"
echo '  "mcpServers": {'

first=true
for file in "$REFERENCES_DIR"/*.json; do
    if [ -f "$file" ]; then
        filename=$(basename "$file" .json)

        if [ "$first" = true ]; then
            first=false
        else
            echo ","
        fi

        # Read and indent the JSON content
        content=$(cat "$file")
        indented=$(echo "$content" | sed 's/^/    /')

        echo "    \"$filename\": $indented"
    fi
done

echo ""
echo "  }"
echo "}"

# Instructions for users
echo "" >&2
echo "Generated .mcp.json with variable placeholders." >&2
echo "Make sure to set the required environment variables in your .zshrc or equivalent:" >&2
echo "  DATABASE_URL" >&2
echo "  KEYCLOAK_URL, KEYCLOAK_ADMIN, KEYCLOAK_ADMIN_PASSWORD, KEYCLOAK_REALM" >&2
echo "  GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, OAUTH_PORT" >&2
echo "These variables will be substituted when the application reads the config." >&2
