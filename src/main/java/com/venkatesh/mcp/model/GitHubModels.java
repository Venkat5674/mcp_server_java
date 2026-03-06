package com.venkatesh.mcp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

public class GitHubModels {
    
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PushFileRequest {
        private String message;
        private String content; // Base64
        private String sha; // nullable
        private String branch; // nullable
    }

    @Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown=true)
    public static class PushFileResponse {
        private CommitInfo commit;
        private FileInfo content;

        @Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown=true)
        public static class CommitInfo {
            private String sha;
            private String message;
            @JsonProperty("html_url")
            private String htmlUrl;
        }

        @Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown=true)
        public static class FileInfo {
            private String name;
            private String path;
            private String sha;
            @JsonProperty("html_url")
            private String htmlUrl;
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown=true)
    public static class RepoContentItem {
        private String name;
        private String path;
        private String type; // "file" or "dir"
        private String sha;
        private Long size;
        @JsonProperty("html_url")
        private String htmlUrl;
        @JsonProperty("download_url")
        private String downloadUrl;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown=true)
    public static class GitHubError {
        private String message;
        @JsonProperty("documentation_url")
        private String documentationUrl;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OperationResult {
        private boolean success;
        private String message;
        private String fileUrl;
        private String commitUrl;
        private List<RepoContentItem> items;
    }
}
