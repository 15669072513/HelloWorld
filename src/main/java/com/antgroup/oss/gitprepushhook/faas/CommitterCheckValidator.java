package com.antgroup.oss.gitprepushhook.faas;

import com.antgroup.oss.gitprepushhook.bo.AuthorCommitInfo;
import com.antgroup.oss.gitprepushhook.faas.po.CommitterCheck;
import com.antgroup.oss.gitprepushhook.faas.po.RiskItem;

public interface CommitterCheckValidator {
     RiskItem match(AuthorCommitInfo commid, String rurl, CommitterCheck committerCheck);
}
