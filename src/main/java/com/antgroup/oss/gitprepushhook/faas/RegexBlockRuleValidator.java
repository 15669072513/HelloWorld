package com.antgroup.oss.gitprepushhook.faas;

import com.antgroup.oss.gitprepushhook.faas.po.BlockCheckItem;
import com.antgroup.oss.gitprepushhook.faas.po.BlockRule;
import com.antgroup.oss.gitprepushhook.faas.po.RiskItem;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexBlockRuleValidator extends BlockRuleValidator {
    @Override
    public RiskItem match(BlockRule blockRule, BlockCheckItem item) {
        Pattern pattern = Pattern.compile(blockRule.getMatchValue());
        Matcher matcher = pattern.matcher(item.getContent());
        if (matcher.find()) {
            return new RiskItem(blockRule.getCode(), blockRule.getName(), Map.of("hitWord", matcher.group()));
        }
        return null;
    }
}
