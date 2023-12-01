package com.antgroup.oss.gitprepushhook.faas.po;


/**
 * @author yinzhennan
 */
@SuppressWarnings("unused")
public class OssRiskItem {
    private String repoRemoteUrl;
    private String commitId;
    private String fileName;
    private String gitAuthor;
    private String gitEmail;
    private String riskContent;
    private String macAddress;

    public String getRepoRemoteUrl() {
        return repoRemoteUrl;
    }

    public void setRepoRemoteUrl(String repoRemoteUrl) {
        this.repoRemoteUrl = repoRemoteUrl;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getGitAuthor() {
        return gitAuthor;
    }

    public void setGitAuthor(String gitAuthor) {
        this.gitAuthor = gitAuthor;
    }

    public String getGitEmail() {
        return gitEmail;
    }

    public void setGitEmail(String gitEmail) {
        this.gitEmail = gitEmail;
    }

    public String getRiskContent() {
        return riskContent;
    }

    public void setRiskContent(String riskContent) {
        this.riskContent = riskContent;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}