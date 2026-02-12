---
name: mcp-builder
description: MCP (Model Context Protocol) server building principles. Tool design, resource patterns, best practices.
user-invocable: true
argument-hint: [tool | resource | server | prompt]
allowed-tools: Read, Glob, Grep, Write, Edit
---

# /mcp-builder - MCP Server Builder

$ARGUMENTS

---

## Purpose

Guide building MCP (Model Context Protocol) servers including tool design, resource patterns, prompt templates, and security practices.

---

## Before Starting

Read and apply knowledge from:
- `.agent/skills/mcp-builder/SKILL.md` for MCP building principles
- `.agent/agents/backend-specialist.md` for backend expertise

---

## Behavior

When `/mcp-builder` is triggered:

1. **Understand requirements**
   - What capabilities (Tools, Resources, Prompts)?
   - What data sources to integrate?
   - Security requirements?

2. **Design**
   - Server architecture
   - Tool design principles
   - Resource patterns (static/dynamic/template)

3. **Implement**
   - Error handling
   - Multimodal handling
   - Security principles

4. **Test and validate**
   - Tool invocation testing
   - Resource access testing
   - Error cases

---

## Examples

```
/mcp-builder create MCP server for database access
/mcp-builder design tool for file search
/mcp-builder add resource pattern for API data
/mcp-builder security review for MCP server
```
