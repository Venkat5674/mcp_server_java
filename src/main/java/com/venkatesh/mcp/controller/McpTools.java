package com.venkatesh.mcp.controller;

import com.venkatesh.mcp.model.GitHubModels.OperationResult;
import com.venkatesh.mcp.model.GitHubModels.RepoContentItem;
import com.venkatesh.mcp.service.GitHubService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class McpTools {

    private final GitHubService gitHubService;

    public McpTools(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @Tool(name = "github_push_solution", description = "Push a LeetCode solution or any code file to a specific folder in a GitHub repository. Claude should use this when asked to save, upload, or push code. Returns URLs to the committed file.")
    public String githubPushSolution(
            @ToolParam(description = "'owner/repo' or full GitHub URL") String repo,
            @ToolParam(description = "target folder, empty string for root") String folder,
            @ToolParam(description = "file name with extension") String filename,
            @ToolParam(description = "full solution code") String code,
            @ToolParam(description = "optional, auto-generated if blank") String commitMessage) {
        try {
            String[] parts = gitHubService.parseOwnerRepo(repo);
            OperationResult result = gitHubService.pushFile(parts[0], parts[1], folder, filename, code, commitMessage);
            if (result.isSuccess()) {
                return "✅ Solution pushed!\nFile URL: " + result.getFileUrl() + "\nCommit URL: "
                        + result.getCommitUrl();
            } else {
                return "❌ " + result.getMessage();
            }
        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }

    @Tool(name = "github_list_files", description = "List folders and files in a GitHub repository folder.")
    public String githubListFiles(
            @ToolParam(description = "'owner/repo' or full GitHub URL") String repo,
            @ToolParam(description = "folder path, empty string for root") String folder) {
        try {
            String[] parts = gitHubService.parseOwnerRepo(repo);
            OperationResult result = gitHubService.listFiles(parts[0], parts[1], folder);
            if (result.isSuccess()) {
                StringBuilder sb = new StringBuilder("Contents of folder '" + folder + "':\n");
                for (RepoContentItem item : result.getItems()) {
                    if ("dir".equals(item.getType())) {
                        sb.append("📁 ").append(item.getName()).append(" - ").append(item.getHtmlUrl()).append("\n");
                    } else {
                        sb.append("📄 ").append(item.getName()).append(" - ").append(item.getHtmlUrl()).append("\n");
                    }
                }
                return sb.toString();
            } else {
                return "❌ " + result.getMessage();
            }
        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }

    @Tool(name = "github_create_folder", description = "Create a new folder in a GitHub repository. Creates a .gitkeep file to achieve this.")
    public String githubCreateFolder(
            @ToolParam(description = "'owner/repo' or full GitHub URL") String repo,
            @ToolParam(description = "new folder path to create") String folder) {
        try {
            String[] parts = gitHubService.parseOwnerRepo(repo);
            OperationResult result = gitHubService.createFolder(parts[0], parts[1], folder);
            if (result.isSuccess()) {
                return "✅ Folder created successfully!";
            } else {
                return "❌ " + result.getMessage();
            }
        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }
}
