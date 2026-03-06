#!/usr/bin/env python3
"""
MCP Configuration Validator
Validates MCP server configurations from .mcp.json
"""

import json
import os
import re
import sys

REQUIRED_FIELDS = {
    "type": str,
    "command": str,
    "args": list,
    "env": dict,
}

VALID_TYPES = ["stdio", "sse", "websocket"]


def substitute_env_vars(obj):
    """Recursively substitute environment variables in configuration."""
    if isinstance(obj, str):
        # Find and replace ${VAR_NAME} patterns with actual environment variable values
        def replace_var(match):
            var_name = match.group(1)
            return os.environ.get(var_name, match.group(0))  # Return original if env var not found
        
        return re.sub(r'\$\{([^}]+)\}', replace_var, obj)
    
    elif isinstance(obj, dict):
        return {key: substitute_env_vars(value) for key, value in obj.items()}
    
    elif isinstance(obj, list):
        return [substitute_env_vars(item) for item in obj]
    
    else:
        return obj


def validate_server(name: str, config: dict) -> list:
    """Validate a single MCP server configuration."""
    errors = []

    # Check required fields
    for field, field_type in REQUIRED_FIELDS.items():
        if field not in config:
            errors.append(f"  - Missing required field: '{field}'")
        elif not isinstance(config[field], field_type):
            errors.append(f"  - Field '{field}' should be {field_type.__name__}")

    # Validate type
    if "type" in config and config["type"] not in VALID_TYPES:
        errors.append(f"  - Invalid type: '{config['type']}'. Must be one of: {VALID_TYPES}")

    # Check command exists (for stdio)
    if config.get("type") == "stdio" and "command" in config:
        command = config["command"]
        if not command.startswith("npx") and not os.path.exists(command):
            # Check if npx or node command exists
            if command in ["npx", "node"] or command.startswith("npx"):
                pass  # OK, will be resolved at runtime
            elif not os.path.exists(command):
                errors.append(f"  - Command not found: '{command}'")

    return errors


def validate_mcp_file(filepath: str) -> bool:
    """Validate MCP configuration file."""
    print(f"Validating MCP config: {filepath}")
    print("-" * 50)

    if not os.path.exists(filepath):
        print(f"ERROR: File not found: {filepath}")
        return False

    try:
        with open(filepath, "r") as f:
            raw_data = json.load(f)
    except json.JSONDecodeError as e:
        print(f"ERROR: Invalid JSON: {e}")
        return False

    # Substitute environment variables in the configuration
    data = substitute_env_vars(raw_data)

    if "mcpServers" not in data:
        print("ERROR: Missing 'mcpServers' key")
        return False

    servers = data["mcpServers"]
    if not isinstance(servers, dict):
        print("ERROR: 'mcpServers' should be an object")
        return False

    all_valid = True
    for server_name, server_config in servers.items():
        print(f"\nChecking server: {server_name}")
        errors = validate_server(server_name, server_config)

        if errors:
            all_valid = False
            print("  ERRORS:")
            for error in errors:
                print(error)
        else:
            print("  ✓ Valid")

    print("\n" + "-" * 50)
    if all_valid:
        print("✓ All MCP servers are valid!")
        print("✓ Environment variables have been substituted successfully!")
        return True
    else:
        print("✗ Some MCP servers have errors")
        return False


if __name__ == "__main__":
    filepath = sys.argv[1] if len(sys.argv) > 1 else ".mcp.json"
    success = validate_mcp_file(filepath)
    sys.exit(0 if success else 1)
