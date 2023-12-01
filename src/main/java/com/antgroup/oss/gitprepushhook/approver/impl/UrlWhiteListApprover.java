package com.antgroup.oss.gitprepushhook.approver.impl;

import com.antgroup.oss.gitprepushhook.approver.RiskApprover;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * 验证url是否匹配白名单
 * 白名单配置见:
 */
@AllArgsConstructor
public class UrlWhiteListApprover implements RiskApprover {
    private List<String> whiteList;

    @Override
    public boolean approve(String url){
        URI uri;
        try {
            //URI按字节解析,性能优于正则
            if(StringUtils.isNotEmpty(url) && !url.contains("://")){
                url = "https://"+url;
            }
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return false;
        }
        String host = uri.getHost();
        if (host == null) {
            // URL不合法
            return false;
        }

        //IP地址的正则表达式(不带端口号)
        String ipRegex = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
        if (host.matches(ipRegex)) {
            return true;
        }

        // 验证URL的域名是否与允许的域名匹配
        return whiteList.stream()
                .anyMatch(allowedDomain -> isDomainMatch(host, allowedDomain));
    }

    public static boolean isDomainMatch(String domain, String allowedDomain) {
        if (allowedDomain.equals(domain)) {
            // 完全匹配
            return true;
        }

        if (allowedDomain.startsWith("*.")) {
            // 通配符匹配
            String wildcardDomain = allowedDomain.substring(2);
            return domain.endsWith(wildcardDomain);
        }

        return false;
    }
}
