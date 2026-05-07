package com.dailyreport.service;

import com.dailyreport.model.dto.GitCommitDTO;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitService {

    public List<GitCommitDTO> getCommits(String repoPath, LocalDate date) {
        File repoDir = new File(repoPath);
        if (!repoDir.exists()) {
            throw new RuntimeException("仓库路径不存在: " + repoPath);
        }

        File gitDir = findGitDir(repoDir);
        if (gitDir == null) {
            throw new RuntimeException("该路径不是git仓库: " + repoPath);
        }

        List<GitCommitDTO> commits = new ArrayList<>();

        try (Repository repository = new FileRepositoryBuilder()
                .setGitDir(gitDir)
                .build();
             Git git = new Git(repository)) {

            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(23, 59, 59);

            ZoneId zoneId = ZoneId.systemDefault();
            int startSec = (int) dayStart.atZone(zoneId).toEpochSecond();
            int endSec = (int) dayEnd.atZone(zoneId).toEpochSecond();

            Iterable<RevCommit> log = git.log().call();

            for (RevCommit commit : log) {
                int commitTime = commit.getCommitTime();
                if (commitTime < startSec) {
                    break;
                }
                if (commitTime <= endSec) {
                    List<String> changedFiles = getChangedFiles(repository, git, commit);
                    LocalDateTime commitDateTime = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochSecond(commitTime), zoneId);

                    GitCommitDTO dto = new GitCommitDTO(
                            commit.getName().substring(0, 8),
                            commit.getFullMessage().trim(),
                            commit.getAuthorIdent().getName(),
                            commitDateTime,
                            changedFiles
                    );
                    commits.add(dto);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("读取git日志失败: " + e.getMessage(), e);
        }

        // 按时间正序排列
        commits.sort((a, b) -> a.getCommitTime().compareTo(b.getCommitTime()));
        return commits;
    }

    private List<String> getChangedFiles(Repository repository, Git git, RevCommit commit) {
        List<String> files = new ArrayList<>();
        try {
            if (commit.getParentCount() == 0) {
                // 初始提交，所有文件都是新增
                try (ObjectReader reader = repository.newObjectReader()) {
                    CanonicalTreeParser treeParser = new CanonicalTreeParser();
                    treeParser.reset(reader, commit.getTree());
                    while (!treeParser.eof()) {
                        files.add(treeParser.getEntryPathString());
                        treeParser.next();
                    }
                }
                return files;
            }

            RevCommit parent = commit.getParent(0);
            try (DiffFormatter diffFormatter = new DiffFormatter(new ByteArrayOutputStream())) {
                diffFormatter.setRepository(repository);
                AbstractTreeIterator oldTree = getTreeIterator(repository, parent);
                AbstractTreeIterator newTree = getTreeIterator(repository, commit);
                List<DiffEntry> diffs = diffFormatter.scan(oldTree, newTree);
                for (DiffEntry entry : diffs) {
                    String path = entry.getNewPath().equals("/dev/null")
                            ? entry.getOldPath()
                            : entry.getNewPath();
                    files.add(path);
                }
            }
        } catch (Exception e) {
            // 忽略差异解析错误，返回空列表
        }
        return files;
    }

    private AbstractTreeIterator getTreeIterator(Repository repository, RevCommit commit) throws IOException {
        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        try (ObjectReader reader = repository.newObjectReader()) {
            ObjectId treeId = commit.getTree().getId();
            treeParser.reset(reader, treeId);
        }
        return treeParser;
    }

    private File findGitDir(File dir) {
        File gitDir = new File(dir, ".git");
        if (gitDir.exists() && gitDir.isDirectory()) {
            return gitDir;
        }
        // 也支持bare仓库
        if (new File(dir, "HEAD").exists()) {
            return dir;
        }
        return null;
    }
}
