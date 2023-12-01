package com.antgroup.oss.gitprepushhook.faas.po;

import lombok.Data;

@Data
public class PrePushHookLog {

    //preHookLog id
    private Long id;
    // git用户名
    private String userName;
    // git邮箱
    private String userEmail;
    // 仓库远程地址
    private String repoRemoteUrl;
    // 分支
    private String branch;
    // 代码量
    private Long codeSize;
    // 扫描耗时
    private Long timeConsume;
    // 敏感数据数量
    private Integer scanRiskCount;
    // 用户提交
    private Boolean isPushed;
    // scanignore
    private String scanignore;
    /**
     * CPU架构/型号
     */
    private String hookVersion;
    /**
     * 平台信息：linux等
     */
    private String system;
    /**
     * CPU架构
     */
    private String arch;
    /**
     * 代码行数
     */
    private Long codeLines;


}
