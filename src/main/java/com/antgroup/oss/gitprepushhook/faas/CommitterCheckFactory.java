package com.antgroup.oss.gitprepushhook.faas;

import com.antgroup.oss.gitprepushhook.PrePushHook;
import com.antgroup.oss.gitprepushhook.faas.po.CommitterCheck;

public class CommitterCheckFactory {

    private static final CommiterEmailValidator commiterEmailBlockRule = new CommiterEmailValidator();
    private static final DefaultCheckValidator defaultCheckValidator = new DefaultCheckValidator();

    public static CommitterCheckValidator getInstance(CommitterCheck.CommitterType matchMode){
        if(matchMode==null){
            PrePushHook.logDebug("mode为空");
            return defaultCheckValidator;
        }
        switch (matchMode) {
            case EMAIL -> {
                PrePushHook.logDebug("commiterEmailBlockRule");
                return commiterEmailBlockRule;
            }
            default -> {
                PrePushHook.logDebug("默认committer validator");
                return defaultCheckValidator ;
            }
        }
    }

}
