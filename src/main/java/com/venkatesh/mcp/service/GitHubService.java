package com.venkatesh.mcp.service;

import com.venkatesh.mcp.model.GitHubModels.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class GitHubService {
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private final WebClient webClient;

    public GitHubService(
            @Value("${github.token:#{null}}") String token,
            @Value("${github.api.base-url}") String baseUrl) {

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "GITHUB_TOKEN environment variable is not set. Please set it to a valid GitHub Personal Access Token.");
        }

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("User-Agent", "leetcode-github-mcp/1.0")
                .build();
    }

    public String[] parseOwnerRepo(String repoInput) {
        if (repoInput == null || repoInput.isBlank()) {
            throw new IllegalArgumentException("Invalid repository format. Expected 'owner/repo'.");
        }
        String parsed = repoInput.trim();
        if (parsed.startsWith("https://github.com/")) {
            parsed = parsed.substring("https://github.com/".length());
        }
        if (parsed.endsWith(".git")) {
            parsed = parsed.substring(0, parsed.length() - ".git".length());
        }
        String[] parts = parsed.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid repository format. Expected 'owner/repo' or 'https://github.com/owner/repo'.");
        }
        return parts;
    }

    public Optional<String> getFileSha(String owner, String repo, String filePath) {
        try {
            RepoContentItem item = webClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, filePath)
                    .retrieve()
                    .bodyToMono(RepoContentItem.class)
                    .block();
            return Optional.ofNullable(item).map(RepoContentItem::getSha);
        } catch (WebClientResponseException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error getting file SHA for {}/{}/{}", owner, repo, filePath, e);
            return Optional.empty();
        }
    }

    public OperationResult listFiles(String owner, String repo, String folderPath) {
        String path = folderPath == null ? "" : folderPath.trim();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        try {
            List<RepoContentItem> items = webClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .retrieve()
                    .bodyToFlux(RepoContentItem.class)
                    .collectList()
                    .block();
            return OperationResult.builder()
                    .success(true)
                    .items(items)
                    .build();
        } catch (WebClientResponseException.NotFound e) {
            return OperationResult.builder()
                    .success(false)
                    .message(
                            "Folder not found. Use github_create_folder to create it first. (Hint: Does your GITHUB_TOKEN have 'repo' scope?)")
                    .build();
        } catch (Exception e) {
            log.error("Error listing files", e);
            return OperationResult.builder()
                    .success(false)
                    .message("Error listing files: " + e.getMessage()
                            + ". Check your GITHUB_TOKEN and ensure it has 'repo' scope.")
                    .build();
        }
    }

    public OperationResult pushFile(String owner, String repo, String folderPath, String fileName, String fileContent,
            String commitMessage) {
        try {
            String path = folderPath == null || folderPath.isBlank() ? fileName : folderPath.trim();
            if (!path.equals(fileName) && !path.endsWith("/")) {
                path += "/" + fileName;
            } else if (!path.equals(fileName) && path.endsWith("/")) {
                path += fileName;
            }
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            String encodedContent = Base64.getEncoder().encodeToString(fileContent.getBytes(StandardCharsets.UTF_8));
            Optional<String> shaOpt = getFileSha(owner, repo, path);

            String message = (commitMessage == null || commitMessage.isBlank())
                    ? "Add " + fileName + " via LeetCode GitHub MCP"
                    : commitMessage;

            PushFileRequest request = PushFileRequest.builder()
                    .message(message)
                    .content(encodedContent)
                    .sha(shaOpt.orElse(null))
                    .build();

            PushFileResponse response = webClient.put()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PushFileResponse.class)
                    .block();

            if (response != null && response.getContent() != null && response.getCommit() != null) {
                return OperationResult.builder()
                        .success(true)
                        .fileUrl(response.getContent().getHtmlUrl())
                        .commitUrl(response.getCommit().getHtmlUrl())
                        .build();
            } else {
                return OperationResult.builder()
                        .success(false)
                        .message("Failed to push file, response was empty.")
                        .build();
            }
        } catch (WebClientResponseException e) {
            log.error("HTTP Error pushing file: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return OperationResult.builder()
                    .success(false)
                    .message("GitHub API error: " + e.getResponseBodyAsString()
                            + ". Ensure GITHUB_TOKEN has 'repo' scope.")
                    .build();
        } catch (Exception e) {
            log.error("Error pushing file", e);
            return OperationResult.builder()
                    .success(false)
                    .message("Error pushing file: " + e.getMessage() + ". Check your GITHUB_TOKEN.")
                    .build();
        }
    }

    public OperationResult createFolder(String owner, String repo, String folderPath) {
        String path = folderPath != null ? folderPath.trim() : "";
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.isBlank()) {
            return OperationResult.builder()
                    .success(false)
                    .message("Folder path cannot be empty.")
                    .build();
        }

        String gitkeepPath = path + "/.gitkeep";
        Optional<String> shaOpt = getFileSha(owner, repo, gitkeepPath);
        if (shaOpt.isPresent()) {
            return OperationResult.builder()
                    .success(false)
                    .message("Folder already exists.")
                    .build();
        }

        return pushFile(owner, repo, path, ".gitkeep", "", "Create folder " + path);
    }
}
