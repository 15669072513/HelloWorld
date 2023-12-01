package com.antgroup.oss.gitprepushhook.faas;

import com.alibaba.fastjson.JSONObject;
import com.antgroup.oss.gitprepushhook.PrePushHook;
import com.antgroup.oss.gitprepushhook.bo.AuthorCommitInfo;
import com.antgroup.oss.gitprepushhook.faas.po.CommitterCheck;
import com.antgroup.oss.gitprepushhook.faas.po.RiskItem;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 提交者邮箱校验
 */
public class CommiterEmailValidator implements  CommitterCheckValidator {

    /**
     * 邮箱校验：
     * 1.特定的仓库地址不校验邮箱
     * 2，特定的邮箱地址不校验
     * 3，增加几个邮箱后缀
     * @return
     */
    @Override
    public RiskItem match(AuthorCommitInfo commitInfo, String remoteUrl, CommitterCheck committerCheck) {
        String strandEmailPattern = committerCheck.getValue();
        Map<String, List<String>> whiteValue = committerCheck.getWhiteValue();
        if(StringUtils.isBlank(strandEmailPattern)){
            PrePushHook.logDebug("faas email为空：");
            return null;
        }
        String emailAddress =commitInfo.getUserEmail();
        List<String> emailValidWhiteEmail = whiteValue.get("email");
        List<String> emailValidWhiteRepo = whiteValue.get("repo");

        boolean emailValid = validemail(emailValidWhiteEmail,emailAddress);
        boolean repoValid = validRepo(emailValidWhiteRepo,remoteUrl);

        //2个任意一个符合就返回。
        if(emailValid || repoValid){
            PrePushHook.logDebug("email白名单校验通过：" + JSONObject.toJSONString(emailValidWhiteEmail));
            PrePushHook.logDebug("repo白名单校验通过：" + JSONObject.toJSONString(emailValidWhiteRepo));
            return null;
        }

        //开始校验,List里面的内容参考#EMAIL_PATTERN
        List<String> emailBlockList = Arrays.stream(strandEmailPattern.split(",")).toList();
        RiskItem riskItem = new RiskItem("committerEmailCheck",committerCheck.getName(), new HashMap<>());
        String result = "您当前的提交邮箱为："+emailAddress+",请修改为公司邮箱。";
        riskItem.setHitWord(result);

        if(StringUtils.isNotBlank(emailAddress)){
            int start = emailAddress.indexOf("@");
            if(start==-1){
                PrePushHook.logDebug("邮箱格式没有@");
                return riskItem;
            }
            String emailSufix = emailAddress.substring(start);
            if(!emailBlockList.contains(emailSufix)){
                return riskItem;
            }
        }
        //提交者的邮箱是空的
        else{
            PrePushHook.logDebug("邮箱为空");
            riskItem.setHitWord(result);
            return riskItem;
        }
        PrePushHook.logDebug("返回end");
        return null;
    }

    private boolean validRepo(List<String> emailValidWhiteRepo, String remoteUrl) {
        if(emailValidWhiteRepo==null || emailValidWhiteRepo.isEmpty()){
            PrePushHook.logDebug("repo白名单为空");
            return false;
        }
        List<String> repoWhitesCommon = emailValidWhiteRepo.stream().map(this::getRepoCommon).toList();
        String repoCommon = getRepoCommon(remoteUrl);
        return repoWhitesCommon.contains(repoCommon);
    }

    /**
     * 把仓库的通用前缀去掉，这样可以兼容git@和https:// 2种写法
     * @param remoteAddress
     */
    private String getRepoCommon(String remoteAddress) {
        if(StringUtils.isEmpty(remoteAddress)){
            PrePushHook.logDebug("remoteAddress为空");
            return "";
        }
        if (remoteAddress.startsWith("git@")) {
            return remoteAddress.replace("git@","").replace(":","/").replace(".git","");
        }else{
            return remoteAddress.replace("http://","").replace("https://","").replace(".git","");
        }
    }


    private boolean validemail(List<String> emailValidWhiteEmail, String emailAddress) {
        if(emailValidWhiteEmail==null || emailValidWhiteEmail.isEmpty()){
            PrePushHook.logDebug("邮箱白名单为空");
            return false;
        }
        return emailValidWhiteEmail.contains(emailAddress);
    }
}
