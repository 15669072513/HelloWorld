package com.antgroup.oss.gitprepushhook.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class AuthorCommitInfo{
    String commitId;
    String userEmail;
}