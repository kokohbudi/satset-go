#!/bin/bash
# Generate MCP config from references
# Usage: ./generate-mcp.sh > .mcp.json

REFERENCES_DIR="$(dirname "$0")/references"

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
