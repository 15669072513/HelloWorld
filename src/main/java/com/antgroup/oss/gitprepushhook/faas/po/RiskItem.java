package com.antgroup.oss.gitprepushhook.faas.po;


import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class RiskItem {
    private String ruleCode;
    private String hitWord;
    private String ruleName;

    private BlockRule.MatchMode matchMode;
    Map<String, Object> data;

    public RiskItem() {
    }

    public RiskItem(String ruleCode, String ruleName, Map<String, Object> data) {
        this.ruleCode = ruleCode;
        this.hitWord = data.values().stream().map(Object::toString).collect(Collectors.joining());
        this.ruleName = ruleName;
        this.data = data;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getHitWord() {
        return hitWord;
    }

    public void setHitWord(String hitWord) {
        this.hitWord = hitWord;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    @Override
    public String toString() {
        return ruleCode + ':' + hitWord;
    }

    public BlockRule.MatchMode getMatchMode() {
        return matchMode;
    }

    public void setMatchMode(BlockRule.MatchMode matchMode) {
        this.matchMode = matchMode;
    }
}
