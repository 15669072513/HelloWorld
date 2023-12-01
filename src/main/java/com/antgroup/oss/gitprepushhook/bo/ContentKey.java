package com.antgroup.oss.gitprepushhook.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentKey {
    RevCommit commit;
    String fileName;
    List<Integer> lines;
    String content;
    String remoteUrl;
    String commitId;
}
