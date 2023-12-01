package com.antgroup.oss.gitprepushhook.approver;

import com.alipay.antscanlib.data.KeymapResult;
import com.alipay.antscanlib.data.KeymapResultDetail;

import java.util.Iterator;
import java.util.stream.Collectors;

public interface RiskApprover {
    default boolean approve(KeymapResult risk) {
        Iterator<KeymapResultDetail> iterator = risk.getDetail().iterator();
        while (iterator.hasNext()) {
            KeymapResultDetail keymapResultDetail = iterator.next();
            String hitWord = keymapResultDetail.getData().values().stream().map(Object::toString).collect(Collectors.joining());
            if (approve(hitWord)) {
                iterator.remove();
            }
        }
        return risk.getDetail().isEmpty();
    }
    boolean approve(String hitWord);
}
