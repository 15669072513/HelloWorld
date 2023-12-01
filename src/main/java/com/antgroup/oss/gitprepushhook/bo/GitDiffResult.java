package com.antgroup.oss.gitprepushhook.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GitDiffResult {
    String remoteUrl;
    List<AuthorCommitInfo> allChangeCommit = new ArrayList<>();
    AtomicLong allChangeLines = new AtomicLong(0L);
    AtomicLong allChangeSize = new AtomicLong(0L);
}
