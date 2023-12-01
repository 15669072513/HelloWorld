package com.antgroup.oss.gitprepushhook.faas.po;

import java.util.List;
import java.util.Map;

/**
 * 阻塞配置
 */
@SuppressWarnings("unused")
public class CommitterCheck {

    /**
     * 匹配模式,默认相等(既包含)
     */
    private CommitterType committerType;
    /**
     * 通用的配置信息
     */
    private String value;
    /**
     * 白名单
     */
    private Map<String,List<String>> whiteValue;

    /**
     * 名字
     */
    private String name;

    public CommitterCheck() {
    }

    public CommitterCheck(CommitterType committerType, String value, Map<String, List<String>> whiteValue, String name) {
        this.committerType = committerType;
        this.value = value;
        this.whiteValue = whiteValue;
        this.name = name;
    }

    public CommitterType getCommitterType() {
        return committerType;
    }

    public void setCommitterType(CommitterType committerType) {
        this.committerType = committerType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Map<String, List<String>> getWhiteValue() {
        return whiteValue;
    }

    public void setWhiteValue(Map<String, List<String>> whiteValue) {
        this.whiteValue = whiteValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public enum CommitterType{
        EMAIL
    }


}