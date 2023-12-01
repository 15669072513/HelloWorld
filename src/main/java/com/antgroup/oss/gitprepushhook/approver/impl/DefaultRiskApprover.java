package com.antgroup.oss.gitprepushhook.approver.impl;

import com.alipay.antscanlib.data.KeymapResult;
import com.antgroup.oss.gitprepushhook.approver.RiskApprover;

public class DefaultRiskApprover implements RiskApprover {
    private static final RiskApprover INSTANCE = new DefaultRiskApprover();
    @Override
    public boolean approve(KeymapResult risk) {
        return false;
    }

    @Override
    public boolean approve(String hitWord) {
        return false;
    }

    public static RiskApprover getInstance() {
        return INSTANCE;
    }
}
