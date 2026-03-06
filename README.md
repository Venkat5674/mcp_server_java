# LeetCode GitHub MCP Server

A Java Spring Boot MCP (Model Context Protocol) server project. This server allows Claude Desktop to push LeetCode solutions directly to a GitHub repository by calling the GitHub Contents REST API.

## Build

```bash
mvn clean package -DskipTests
```

## Run Locally

```bash
set GITHUB_TOKEN=ghp_your_token_here
java -jar target/leetcode-github-mcp-1.0.0.jar
```
