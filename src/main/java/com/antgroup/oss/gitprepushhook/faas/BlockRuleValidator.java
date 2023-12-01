package com.antgroup.oss.gitprepushhook.faas;

import com.antgroup.oss.gitprepushhook.faas.po.BlockCheckItem;
import com.antgroup.oss.gitprepushhook.faas.po.BlockRule;
import com.antgroup.oss.gitprepushhook.faas.po.RiskItem;

public abstract class BlockRuleValidator{
    public abstract RiskItem match(BlockRule blockRule, BlockCheckItem item);
}
