package com.antgroup.oss.gitprepushhook.approver;

import com.antgroup.oss.gitprepushhook.approver.impl.DefaultRiskApprover;
import com.antgroup.oss.gitprepushhook.approver.impl.EmailWhiteListApprover;
import com.antgroup.oss.gitprepushhook.approver.impl.UrlWhiteListApprover;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

public final class RiskApproverFactory {
    private Map<String, RiskApprover> approverMap;
    RiskApprover defaultApprover = DefaultRiskApprover.getInstance();
    public RiskApproverFactory(Map<String, List<String>> rules){
        //暂时只支持url
        List<String> whiteList = rules.get("url");
        if (!CollectionUtils.isEmpty(whiteList)) {
            approverMap = Map.of("email" , new EmailWhiteListApprover(null),
                    "url", new UrlWhiteListApprover(whiteList));
        }else{
            approverMap = Map.of("email" , new EmailWhiteListApprover(null));
        }
    }
    public RiskApprover getInstance(String ruleCode){
        RiskApprover riskApprover = approverMap.get(ruleCode);
        return riskApprover == null ? defaultApprover : riskApprover;
    }
}
