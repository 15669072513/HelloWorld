package com.antgroup.oss.gitprepushhook.faas;

import com.antgroup.oss.gitprepushhook.faas.po.BlockRule;

import java.util.List;
import java.util.Map;

public class BlockRuleFactory {



    private static final EqualBlockRuleValidator equalBlockRule = new EqualBlockRuleValidator();
    private static final RegexBlockRuleValidator regexBlockRule = new RegexBlockRuleValidator();

    public static BlockRuleValidator getInstance(BlockRule.MatchMode matchMode){
        switch (matchMode) {
            case EQUAL -> {
                return equalBlockRule;
            }
            case REGEX -> {
                return regexBlockRule;
            }
            default -> {
                return null;
            }
        }


    }

    class BlockItems {

        /**
         * 匹配模式,默认相等(既包含)
         */
        private BlockOptions blockOptions;
        /**
         * json字符串
         */
        private String value;
        /**
         * json字符串
         */
        private Map<String,List<String>> whiteValue;

        private String name;

        public BlockItems() {
        }

        public BlockItems(BlockOptions blockOptions, String value, Map<String, List<String>> whiteValue, String name) {
            this.blockOptions = blockOptions;
            this.value = value;
            this.whiteValue = whiteValue;
            this.name = name;
        }

        public BlockOptions getBlockOptions() {
            return blockOptions;
        }

        public void setBlockOptions(BlockOptions blockOptions) {
            this.blockOptions = blockOptions;
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

        public enum BlockOptions{
            EMAIL
        }


    }

}
