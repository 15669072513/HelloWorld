package com.antgroup.oss.gitprepushhook.faas.po;

import org.eclipse.jgit.revwalk.RevCommit;

@SuppressWarnings("unused")
public class BlockCheckItem {
    private String fileName;
    private String content;
    private RevCommit commit;
    private String remoteUrl;

    public BlockCheckItem() {
    }

    public BlockCheckItem(String fileName, String content) {
        this.fileName = fileName;
        this.content = content;
    }
    public BlockCheckItem(String fileName, String content,RevCommit commit,String remoteUrl) {
        this.fileName = fileName;
        this.content = content;
        this.commit = commit;
        this.remoteUrl = remoteUrl;
    }
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return fileName + ':' + content;
    }

    public RevCommit getCommit() {
        return commit;
    }

    public void setCommit(RevCommit commit) {
        this.commit = commit;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }
}

