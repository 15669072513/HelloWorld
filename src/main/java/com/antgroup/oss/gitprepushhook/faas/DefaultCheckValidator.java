package com.antgroup.oss.gitprepushhook.faas;

import com.antgroup.oss.gitprepushhook.bo.AuthorCommitInfo;
import com.antgroup.oss.gitprepushhook.faas.po.CommitterCheck;
import com.antgroup.oss.gitprepushhook.faas.po.RiskItem;

public class DefaultCheckValidator  implements CommitterCheckValidator{

    /**
     * 默认方法
     * @param rurl
     * @param committerCheck
     * @return
     */
    @Override
    public RiskItem match(AuthorCommitInfo revCommit, String rurl, CommitterCheck committerCheck){
        return null;
    }

}
