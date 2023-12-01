package com.antgroup.oss.gitprepushhook.faas.po;

@SuppressWarnings("unused")
public  class BlockRule {
    /**
     * 是否拦截,否的话只警告
     */
    private boolean block = true;
    /**
     * 匹配模式,默认相等(既包含)
     */
    private MatchMode matchMode = MatchMode.EQUAL;

    /**
     * 匹配的值
     */
    private String matchValue;

    private String code;
    private String name;

    public BlockRule() {
    }

    public BlockRule(String matchValue) {
        this.matchValue = matchValue;
    }

    public BlockRule(String matchValue, MatchMode matchMode) {
        this.matchMode = matchMode;
        this.matchValue = matchValue;
    }

    public BlockRule(String matchValue, MatchMode matchMode, boolean block) {
        this.block = block;
        this.matchMode = matchMode;
        this.matchValue = matchValue;
    }


    public boolean isBlock() {
        return block;
    }

    public void setBlock(boolean block) {
        this.block = block;
    }

    public MatchMode getMatchMode() {
        return matchMode;
    }

    public void setMatchMode(MatchMode matchMode) {
        this.matchMode = matchMode;
    }

    public String getMatchValue() {
        return matchValue;
    }

    public void setMatchValue(String matchValue) {
        this.matchValue = matchValue;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public enum MatchMode{
        EQUAL,REGEX,COMMITTER_EMAIL
    }
}
