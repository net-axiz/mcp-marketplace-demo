package com.repoexplainer.orchestration;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LegacyApiController {

    private final RepoExplainerService repoExplainerService;

    public LegacyApiController(RepoExplainerService repoExplainerService) {
        this.repoExplainerService = repoExplainerService;
    }

    @GetMapping("/legacy-explain")
    public ResponseEntity<String> explain(@RequestParam("url") String githubUrl) {
        try {
            String explanation = repoExplainerService.explainRepo(githubUrl);
            return ResponseEntity.ok(explanation);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Hata: " + e.getMessage());
        }
    }
}
