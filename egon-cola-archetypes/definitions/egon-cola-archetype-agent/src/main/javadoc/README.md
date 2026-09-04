# Egon-COLA Agent Archetype

This classifier documents the `egon-cola-archetype-agent` Maven Archetype distribution.
It generates a Java 21, Spring Boot six-module Deep Research Agent project with a fixed
Agent Flow and one authenticated Server-Sent Events command. The generated project is
process-local and intentionally has no durable state or infrastructure-specific
defaults; model and MCP credentials are supplied by the consuming deployment.
