package com.antgroup.oss.gitprepushhook.faas;

import com.antgroup.oss.gitprepushhook.faas.po.BlockCheckItem;
import com.antgroup.oss.gitprepushhook.faas.po.BlockRule;
import com.antgroup.oss.gitprepushhook.faas.po.RiskItem;

import java.util.Map;

public class EqualBlockRuleValidator extends BlockRuleValidator {
    @Override
    public RiskItem match(BlockRule blockRule, BlockCheckItem item) {
        if (item.getContent().contains(blockRule.getMatchValue())) {
            return new RiskItem(blockRule.getCode(), blockRule.getName(), Map.of("hitWord", blockRule.getMatchValue()));

        }
        return null;
    }
}
