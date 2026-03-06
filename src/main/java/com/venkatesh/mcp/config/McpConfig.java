package com.venkatesh.mcp.config;

import com.venkatesh.mcp.controller.McpTools;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider leetcodeGithubTools(McpTools mcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(mcpTools)
                .build();
    }
}
