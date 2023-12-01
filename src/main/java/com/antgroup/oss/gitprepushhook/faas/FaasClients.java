package com.antgroup.oss.gitprepushhook.faas;

import com.alibaba.fastjson.TypeReference;
import com.antgroup.oss.gitprepushhook.faas.po.*;

import java.util.List;
import java.util.Map;

public final class FaasClients {
    public static final FaasClient<List<RiskItem>,Map<String, Boolean>> APPROVER_FAAS_CLIENT = new FaasClient<>(
            "myjf.common.faasmiddleware.prehookchecker.ossriskchecker.ossriskapprover",
            "1700015",
            "PROD",
            new TypeReference<>() {}
    );

    /**
     *
     */
    public static final FaasClient<List<BlockCheckItem>,Map<String, RiskItem>> BLOCKER_FAAS_CLIENT = new FaasClient<>(
            "myjf.common.faasmiddleware.prehookchecker.ossriskchecker.ossriskblocker",
            "1700015",
            "PROD",
            new TypeReference<>() {}
    );
    public static final FaasClient<PrePushHookLog, Long> LOG_FAAS_CLIENT = new FaasClient<>(
            "myjf.common.faasmiddleware.prehookchecker.ossriskchecker.prehooklogger",
            "1700015",
            "PROD",
            new TypeReference<>() {}
    );

    public static final FaasClient<List<OssRiskItem>,String> SAVER_FAAS_CLIENT = new FaasClient<>(
            "myjf.common.faasmiddleware.prehookchecker.ossriskchecker.ossrisksaver",
            "1700015",
            "PROD",
            new TypeReference<>() {}
    );
    public static final FaasClient<Void,List<BlockRule>> BLOCK_RULES_FAAS_CLIENT = new FaasClient<>(
            "myjf.common.faasmiddleware.prehookchecker.ossriskchecker.ossriskblockrules",
            "1700015",
            "PROD",
            new TypeReference<>() {}
    );
    public static final FaasClient<Void, Map<String,List<String>>> APPROVE_RULES_FAAS_CLIENT = new FaasClient<>(
            "myjf.common.faasmiddleware.prehookchecker.ossriskchecker.ossriskapproverules",
            "1700015",
            "PROD",
            new TypeReference<>() {}
    );

    //阻塞规则
    public static final FaasClient<Void, List<CommitterCheck>> COMMITTER_CHECK_FAAS_CLIENT = new FaasClient<>(
            "myjf.common.faasmiddleware.prehookchecker.ossriskchecker.osscommittercheck",
            "1700015",
            "PROD",
            new TypeReference<>() {}
    );


}
