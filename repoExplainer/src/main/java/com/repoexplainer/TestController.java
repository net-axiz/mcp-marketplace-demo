package com.repoexplainer;

import com.repoexplainer.orchestration.RepoExplainerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final RepoExplainerService service;

    public TestController(RepoExplainerService service) {
        this.service = service;
    }

    @GetMapping("/test-explain")
    public String testExplain(@RequestParam String url) {
        return service.explainRepo(url);
    }
}
