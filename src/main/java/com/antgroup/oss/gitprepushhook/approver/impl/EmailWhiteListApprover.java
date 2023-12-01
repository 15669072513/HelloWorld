package com.antgroup.oss.gitprepushhook.approver.impl;

import com.alipay.antscanlib.data.KeymapResult;
import com.antgroup.oss.gitprepushhook.approver.RiskApprover;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class EmailWhiteListApprover implements RiskApprover {
    private List<String> whiteList;

    public boolean approve(KeymapResult risk) {
        return true;
    }
    public boolean approve(String hitWord) {
        return true;
    }
}
