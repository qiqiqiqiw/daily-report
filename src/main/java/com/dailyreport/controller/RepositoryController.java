package com.dailyreport.controller;

import com.dailyreport.model.GitRepository;
import com.dailyreport.repository.GitRepositoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repositories")
public class RepositoryController {

    private final GitRepositoryRepository repository;

    public RepositoryController(GitRepositoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<GitRepository> listAll() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<GitRepository> create(@RequestBody GitRepository repo) {
        GitRepository saved = repository.save(repo);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
