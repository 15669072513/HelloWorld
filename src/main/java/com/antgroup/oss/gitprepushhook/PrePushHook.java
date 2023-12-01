/*
 * Ant Group
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.antgroup.oss.gitprepushhook;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.antscanlib.data.KeymapResult;
import com.alipay.antscanlib.data.KeymapResultDetail;
import com.alipay.antscanlib.sdk.KeymapFastRecognizer;
import com.antgroup.oss.gitprepushhook.approver.RiskApprover;
import com.antgroup.oss.gitprepushhook.approver.RiskApproverFactory;
import com.antgroup.oss.gitprepushhook.bo.AuthorCommitInfo;
import com.antgroup.oss.gitprepushhook.bo.ContentKey;
import com.antgroup.oss.gitprepushhook.bo.GitDiffResult;
import com.antgroup.oss.gitprepushhook.faas.*;
import com.antgroup.oss.gitprepushhook.faas.po.*;
import com.antgroup.oss.gitprepushhook.scanignore.GitIgnore;
import com.antgroup.oss.gitprepushhook.util.NetworkUtil;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.util.CollectionUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.antgroup.oss.gitprepushhook.faas.FaasClients.APPROVE_RULES_FAAS_CLIENT;

/**
 * @author khotyn
 * @version PrePushHook.java, v 0.1 2022年12月09日 16:58 khotyn
 */
public class PrePushHook{
    // Use last pwd as Git working directory. Current PWD is the enclosing folder of pre-push hook, last is the directory where `git push` runs.
    private static final String workingDir = System.getProperty("user.dir");
    private static final String BAN_LOG_FILE = "git-pre-push-hook-ban.log";
    private static final String WARN_LOG_FILE = "git-pre-push-hook-warn.log";

    private static  RemoteAddressValidator remoteAddressValidator = new RemoteAddressValidator();
    private static final GitDiffer gitDiffer = new GitDiffer();
    private static final KeymapFastRecognizer keymapFastRecognizer = new KeymapFastRecognizer();

    private static final Set<String> banCodes = new HashSet<>();
    private static final Scanner scanner = new Scanner(System.in);

    private static final boolean debugMode = "debug".equals(System.getenv("hook_mode"));

    private static final String VERSION = "0.1.20231129";

    private static final GitIgnore gitIgnore = new GitIgnore();

    private static final PrePushHookLog HOOK_LOG = new PrePushHookLog();

    private static final ThreadPoolExecutor RISK_DETECTOR_SERVICE = new ThreadPoolExecutor(3, 7, 1000,TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(100)
            ,new ThreadPoolExecutor.CallerRunsPolicy());
    private static final ThreadPoolExecutor LOG_FILE_SERVICE = new ThreadPoolExecutor(1, 1, 0,TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(10000)
            ,new ThreadPoolExecutor.AbortPolicy());

    private static List<BlockRule> blockRuleList = List.of();
    private static List<CommitterCheck> committerChecks = List.of();
    private static RiskApproverFactory riskApproverFactory = null;

    @Getter
    private static Map<String, List<String>> approveRuleList;

    public static void init() {
        try {
            keymapFastRecognizer.init("OSS", 200000);
        } catch (Exception e) {
            System.err.println("Keymap initialize failed.");
            e.printStackTrace();
            System.exit(-1);
        }
        //初始化放行规则,拦截规则
        try {
            //放行规则
            approveRuleList= APPROVE_RULES_FAAS_CLIENT.invoke();
            riskApproverFactory = new RiskApproverFactory(approveRuleList);
            committerChecks = FaasClients.COMMITTER_CHECK_FAAS_CLIENT.invoke();
            blockRuleList = FaasClients.BLOCK_RULES_FAAS_CLIENT.invoke();
            banCodes.addAll(blockRuleList.stream().filter(BlockRule::isBlock).map(BlockRule::getCode).toList());
        } catch (Exception e) {
            System.err.println("获取faas规则失败,跳过.");
        }
        banCodes.add("AK");
        banCodes.add("password");
        banCodes.add("PemFile");
        banCodes.add("committerEmailCheck");
    }

    //    @Override
    public static void main(String... args) throws IOException {

        System.out.println("PrePushHook version: " );
        init();

    }



    public static void logDebug(String arg) {
        if (true) {
            System.out.println(arg);
        }
    }

}