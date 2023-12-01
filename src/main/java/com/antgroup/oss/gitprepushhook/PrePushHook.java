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

        System.out.println("PrePushHook version: " + VERSION);
        init();
        String remoteAddress = args[0];
        String localRef = args[1];
        String remoteRef = args[2];
        for (String arg : args) {
            logDebug(arg);
        }

        // aaaaa
        if (StringUtils.equals(remoteRef, Constants.GIT_TAIL_ZERO)) {
            remoteRef = Constants.GIT_TAIL;
        }

        //提交者白名单和repo白名单
        if (remoteAddressValidator.validate(remoteAddress)) {
            System.exit(0);
        }else{
            Long preHookLogId = doLog(remoteAddress, localRef);
            //logDebug("doLog成功返回 id:" + preHookLogId);
            boolean doRunResult = doRun(remoteAddress, localRef, remoteRef);
            //logDebug("doRunResult成功 result:" + doRunResult);
            if (preHookLogId !=null){
                //logDebug("开始执行updateLog id:" + preHookLogId);
                updateLog(preHookLogId);
            }

            if (doRunResult) {
                System.err.println("代码中含有数据安全问题，请改造后再 Push。");
                System.exit(-1);
            } else {
                System.exit(0);
            }

        }


    }

    public static void logDebug(String arg) {
        if (debugMode) {
            System.out.println(arg);
        }
    }

    protected static boolean doRun(String remoteAddress, String localRef, String remoteRef) throws IOException {
        Long startScanTime = System.currentTimeMillis();
        boolean result = isPushContainsSuspiciousContent(PrePushHook.workingDir + File.separator + ".git", remoteRef, localRef, remoteAddress);
        Long endScanTime = System.currentTimeMillis();
        Long timeConsume = (endScanTime-startScanTime)/1000;
        HOOK_LOG.setTimeConsume(timeConsume);


        return result;
    }

    private static final Map<ContentKey, List<KeymapResult>> BAN_MAP = new ConcurrentHashMap<>();
    private static final Map<ContentKey, List<KeymapResult>> WARN_MAP = new ConcurrentHashMap<>();
    private static final AtomicLong CODE_SIZE = new AtomicLong();
    private static final AtomicLong CODE_LINE = new AtomicLong();
    private static final AtomicInteger BAN_COUNT = new AtomicInteger();
    private static final AtomicInteger WARN_COUNT = new AtomicInteger();
    private static BufferedWriter BAN_WRITER;
    private static BufferedWriter WARN_WRITER;

    protected static boolean isPushContainsSuspiciousContent(String gitDir, String remoteRef, String localRef, String remoteAddress) throws IOException {
        boolean isFullPush = fullPush(gitDir, remoteRef, remoteAddress);

        System.out.print("Scanning .");
        GitDiffResult gitDiffResult = gitDiffer.deepDiff(gitDir, remoteRef, localRef, remoteAddress, PrePushHook::handleDiff, gitIgnore);
        //邮箱校验，
        committerChecks(gitDiffResult);
        System.out.println();
        waitForThreadPools();

        //代码行数变更，变更代码大小设置
        HOOK_LOG.setCodeSize(gitDiffResult.getAllChangeSize().get());
        HOOK_LOG.setCodeLines(gitDiffResult.getAllChangeLines().get());
        logDebug("代码行数："+HOOK_LOG.getCodeLines());
        logDebug("代码大小："+HOOK_LOG.getCodeSize());

        System.out.println();
        HOOK_LOG.setScanRiskCount(BAN_COUNT.get()+ WARN_COUNT.get());

        //读取 .scanignore文件内容
        String scanIgnoreContent = gitIgnore.readScanignoreFile();
        HOOK_LOG.setScanignore(scanIgnoreContent);

        if (!BAN_MAP.isEmpty()) {
            System.err.printf("发现包含以下 %s 条信息 等 共 %s 处 违反数据安全内容,详见 %s，请改造之后再 push 到外部的开源仓库中：%n", Math.min(BAN_COUNT.get(),10), BAN_COUNT.get(), BAN_LOG_FILE);
            BAN_MAP.forEach((key, keymapResults) -> System.err.println(formatDetectResult(key, keymapResults, ! isFullPush)));
            System.err.println("修改完成代码之后，请在 push 之前运行 git rebase -i " + remoteRef + " 来将 Commit 进行压缩，否则存在数据安全问题的代码会依旧存在在提交历史中。");
            return true;
        } else if (!WARN_MAP.isEmpty()) {
            System.err.printf("发现以下 %s 条信息 等 共 %s 处 疑似违反数据安全内容的代码,详见 %s：%n", Math.min(WARN_COUNT.get(),10), WARN_COUNT.get(), WARN_LOG_FILE);
            WARN_MAP.forEach((key, keymapResults) -> System.err.println(formatDetectResult(key, keymapResults, ! isFullPush)));
            System.out.print("请确认以上的疑似数据安全问题是否存在风险，如有风险，请输入 YES，如无风险，请输入 NO：");
            String answer = scanner.nextLine();
            boolean cancelPush = !"NO".equalsIgnoreCase(answer);
            HOOK_LOG.setIsPushed(!cancelPush);
            if (cancelPush) {
                System.err.println("修改完成代码之后，请在 push 之前运行 git rebase -i " + remoteRef + " 来将 Commit 进行压缩，否则存在数据安全问题的代码会依旧存在在提交历史中。");
                return true;
            } else {
                System.out.println("开始风险上报");
                boolean faasNotAccessible = false;
                List<OssRiskItem> riskList = new ArrayList<>(WARN_MAP.size());
                String macAddress = NetworkUtil.getMacAddress();
                for (Map.Entry<ContentKey, List<KeymapResult>> entry : WARN_MAP.entrySet()) {
                    ContentKey key = entry.getKey();
                    List<KeymapResult> keyMapResults = entry.getValue();
                    OssRiskItem riskItem = new OssRiskItem();
                    riskItem.setRepoRemoteUrl(remoteAddress);
                    riskItem.setCommitId(key.getCommit().getId().getName());
                    riskItem.setFileName(key.getFileName());
                    riskItem.setGitAuthor(key.getCommit().getAuthorIdent().getName());
                    riskItem.setGitEmail(key.getCommit().getAuthorIdent().getEmailAddress());
                    riskItem.setRiskContent(keyMapResultsToString("", keyMapResults));
                    riskItem.setMacAddress(macAddress);
                    riskList.add(riskItem);
                }
                try {
                    if (debugMode) {
                        logDebug("上报风险:" + JSON.toJSONString(riskList));
                    }
                    FaasClients.SAVER_FAAS_CLIENT.invoke(riskList);
                } catch (Exception e) {
                    faasNotAccessible = true;
                    System.err.println("faas无法链接,跳过风险上报");
                }
                if (! faasNotAccessible) {
                    System.out.println("风险上报成功");
                }
                return false;
            }
        } else {
            System.out.println("代码中暂无发现数据安全问题。");
        }

        try {
            if (BAN_WRITER != null) {
                BAN_WRITER.close();
            }
            if (WARN_WRITER != null) {
                WARN_WRITER.close();
            }
        } catch (IOException ignored) {

        }

        return false;
    }

    /**
     * 提交者信息校验
     * @param gitDiffResult
     */
    private static void committerChecks(GitDiffResult gitDiffResult) {
        logDebug("committercheck信息："+ JSONObject.toJSONString(committerChecks));
        //committerCheck
        committerChecks.forEach(committerCheck -> {
            if(committerCheck==null){
                PrePushHook.logDebug("committerCheck为空");
                return;
            }
            RISK_DETECTOR_SERVICE.submit(()->{
                try {
                    doCommitterCheck(gitDiffResult,committerCheck);
                }catch (Exception e){
                    PrePushHook.logDebug(" doCommitterCheck异常："+e.getMessage());
                }
            });

        });
    }

    private static void handleDiff(ContentKey diffContent){
        String fileName = diffContent.getFileName();
        String content = diffContent.getContent();
        RISK_DETECTOR_SERVICE.submit(() -> {
            //faas上的违规检查
            RiskItem riskItem = faasBlockCheck(new BlockCheckItem(fileName, content,diffContent.getCommit(),diffContent.getRemoteUrl()));
            if(riskItem !=null){
                KeymapResult faasResult = new KeymapResult();
                faasResult.setRuleCode(riskItem.getRuleCode());
                faasResult.setRuleName(riskItem.getRuleName());
                KeymapResultDetail detail = new KeymapResultDetail();
                detail.setData(Collections.singletonMap("hitWord", content));
                faasResult.setDetail(new ArrayList<>(List.of(detail)));
                recordRisk(diffContent, new ArrayList<>(List.of(faasResult)));
            }

        });
        //原有的违规检查
        RISK_DETECTOR_SERVICE.submit(() -> {
            detectContent(diffContent);
        });
        System.out.print(".");
    }

    /**
     * 提交者邮箱校验和记录信息
     * @param gitDiffResult
     * @param committerCheck
     */
    private static void doCommitterCheck(GitDiffResult gitDiffResult, CommitterCheck committerCheck) {
        logDebug("开始执行：doCommitterCheck");
        CommitterCheckValidator instance = CommitterCheckFactory.getInstance(committerCheck.getCommitterType());
        List<AuthorCommitInfo> allChangeCommit = gitDiffResult.getAllChangeCommit();
        allChangeCommit.forEach(commitInfo ->{
            RiskItem riskItem = instance.match(commitInfo,gitDiffResult.getRemoteUrl(), committerCheck);
            if(riskItem!=null){
                ContentKey emailContentKey = new ContentKey();
                emailContentKey.setLines(new ArrayList<>());
                emailContentKey.setContent("对于提交到github的commit信息，强制要求使用公司邮箱，比如："+committerCheck.getValue());
                emailContentKey.setCommitId(commitInfo.getCommitId());
                KeymapResult faasResult = new KeymapResult();
                faasResult.setRuleCode(riskItem.getRuleCode());
                faasResult.setRuleName(riskItem.getRuleName());
                KeymapResultDetail detail = new KeymapResultDetail();
                detail.setData(Collections.singletonMap("hitWord", riskItem.getHitWord()));
                faasResult.setDetail(new ArrayList<>(List.of(detail)));
                recordRisk(emailContentKey,  new ArrayList<>(List.of(faasResult)));
            }
        });
    }


    private static void recordRisk(ContentKey diffContent, List<KeymapResult> riskList){
        for (KeymapResult keymapResult : riskList) {
            //白名单放行规则
            if (ossRiskApproveFilter(diffContent,keymapResult)) {
                PrePushHook.logDebug("白名单放行规则");
                continue;
            }
            boolean isBan = banCodes.contains(keymapResult.getRuleCode());
            if (isBan) {
                int banCount = BAN_COUNT.addAndGet(1);
                //最多一次显示10条违规,防止内存过大
                if (banCount <= 10) {
                    BAN_MAP.merge(diffContent, Collections.synchronizedList(new ArrayList<>(List.of(keymapResult))), (list1, list2) -> {
                        list1.addAll(list2);
                        return list1;
                    });
                }
            }else{
                int warnCount = WARN_COUNT.addAndGet(1);
                int banCount = BAN_COUNT.get();
                //最多一次显示10条经过,防止内存过大. 如果存在违规则不显示警告,无需记录
                if (banCount<=0 && warnCount <= 10 ) {
                    WARN_MAP.merge(diffContent, Collections.synchronizedList(new ArrayList<>(List.of(keymapResult))), (list1, list2) -> {
                        list1.addAll(list2);
                        return list1;
                    });
                }
            }
            logRiskFile(diffContent,keymapResult,isBan);
        }
    }

    @SneakyThrows
    private static void logRiskFile(ContentKey diffContent, KeymapResult keymapResult, boolean isBan) {
        LOG_FILE_SERVICE.submit(() -> {
            try {
                BufferedWriter writer;
                if (isBan) {
                    writer = getBanWriter();
                } else {
                    writer = getWarnWriter();
                }
                writer.append(formatDetectResult(diffContent, List.of(keymapResult), true));
                writer.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SneakyThrows
    private static void waitForThreadPools(){
        RISK_DETECTOR_SERVICE.shutdown();
        while (! RISK_DETECTOR_SERVICE.isTerminated()) {
            System.out.println("扫描剩余任务数：" + (RISK_DETECTOR_SERVICE.getActiveCount()+RISK_DETECTOR_SERVICE.getQueue().size()));
            boolean ignored = RISK_DETECTOR_SERVICE.awaitTermination(1, TimeUnit.SECONDS);
        }
        System.out.println("扫描完成");
    }

    @SneakyThrows
    private static synchronized BufferedWriter getBanWriter() {
        if (BAN_WRITER == null) {
            BAN_WRITER = new BufferedWriter(new FileWriter(BAN_LOG_FILE));
            BAN_WRITER.flush();
        }
        return BAN_WRITER;
    }
    @SneakyThrows
    private static synchronized BufferedWriter getWarnWriter() {
        if (WARN_WRITER == null) {
            WARN_WRITER = new BufferedWriter(new FileWriter(WARN_LOG_FILE));
            WARN_WRITER.flush();
        }
        return WARN_WRITER;
    }

    /**
     * 根据从faas上获取的方形规则,判断是否放行
     * 暂时只支持url白名单判断
     */
    private static boolean ossRiskApproveFilter(ContentKey diffContent, KeymapResult risk) {
        if (riskApproverFactory == null) {
            return false;
        }
        RiskApprover approver = riskApproverFactory.getInstance(risk.getRuleCode());
        return approver.approve(risk);
    }

    private static String formatDetectResult(ContentKey contentKey, List<KeymapResult> keymapResults, boolean outputContent) {
        StringBuilder sb = new StringBuilder();
        String space = "    ";
        String lb = "\n";
        sb.append("风险内容位置：").append(lb);
        sb.append(space).append("CommitId: ").append(contentKey.getCommitId()).append(lb);
        if(StringUtils.isNotEmpty(contentKey.getFileName())){
            sb.append(space).append("文件名：").append(contentKey.getFileName()).append(lb);
        }
        if(!contentKey.getLines().isEmpty()){
            if (contentKey.getLines().size() == 1) {
                sb.append(space).append("文件行：").append(contentKey.getLines().get(0) + 1).append(lb);
            } else {
                sb.append(space).append("文件行：").append(contentKey.getLines().get(0) + 1).append("~").append(contentKey.getLines().get(contentKey.getLines().size() - 1) + 1).append(lb);
            }
        }

        if(outputContent){
            sb.append("风险内容：").append(lb);
            sb.append((space + contentKey.getContent()).replaceAll("\\n", "\n" + space)).append(lb);
        }
        sb.append("风险细节：").append(lb);
        sb.append(keyMapResultsToString(space, keymapResults));
        return sb.toString();
    }

    private static String keyMapResultsToString(String prefix, List<KeymapResult> keymapResults) {
        if(keymapResults.isEmpty()){
            return "无";
        }
        StringBuilder sb = new StringBuilder();
        keymapResults.forEach(keymapResult -> {
            sb.append(prefix).append("敏感类型：").append(keymapResult.getRuleName()).append("(").append(keymapResult.getRuleCode()).append(")\n");
            sb.append(prefix).append(("敏感细节："));
            keymapResult.getDetail().forEach(detail -> sb.append(detail.getData()).append("\n"));
        });
        return sb.toString();
    }

    private static RiskItem faasBlockCheck(BlockCheckItem item){
        if (blockRuleList.isEmpty()) {
            return null;
        }
        for (BlockRule rule : blockRuleList) {
            if(rule==null || rule.getMatchMode() == null){
                continue;
            }
            BlockRuleValidator validator = BlockRuleFactory.getInstance(rule.getMatchMode());
            if(validator == null){
                continue;
            }
            RiskItem match = validator.match(rule,item);
            if (match != null){
                return match;
            }
        }
        return null;
    }

    private static void detectContent(ContentKey diffContent) {
        try {
            List<KeymapResult> keymapResults = keymapFastRecognizer.doEvaluateText(diffContent.getFileName(),diffContent.getContent());
            if (keymapResults != null && !keymapResults.isEmpty()) {
                recordRisk(diffContent, keymapResults);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public static Long doLog(String remoteAddress, String localRef) {
        String gitDir = PrePushHook.workingDir + File.separator + ".git";
        if (StringUtils.isEmpty(gitDir)) {
            return null;
        }
        File gitDirFile = new File(gitDir);
        if (!gitDirFile.exists()) {
            return null;
        }
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(gitDirFile).readEnvironment().findGitDir().build()) {
            ObjectId commitId = ObjectId.fromString(localRef);
            RevCommit commit = repository.parseCommit(commitId);
            HOOK_LOG.setRepoRemoteUrl(remoteAddress);
            HOOK_LOG.setUserName(commit.getAuthorIdent().getName());
            HOOK_LOG.setUserEmail(commit.getAuthorIdent().getEmailAddress());
            HOOK_LOG.setBranch(repository.getBranch());
            logDebug("PrePushHookLog params:"+ JSON.toJSONString(HOOK_LOG));
            return FaasClients.LOG_FAAS_CLIENT.invoke(HOOK_LOG);

        }catch (Exception e){
            System.out.println(e.getMessage());
            //not care
        }
        return null;

    }

    /**
     * 增加：CPU架构: aarch64
     *  system操作系统：MacOS Linux等
     * @param preHookLogId ID
     */
    @SneakyThrows
    public static void updateLog(Long preHookLogId) {
        //获取平台信息和版本号
        HOOK_LOG.setHookVersion(VERSION);
        HOOK_LOG.setArch(System.getProperty("os.arch"));
        HOOK_LOG.setSystem(System.getProperty("os.name"));
        HOOK_LOG.setId(preHookLogId);
        try {
            FaasClients.LOG_FAAS_CLIENT.invoke(HOOK_LOG);
        }catch (Exception e){
            logDebug("更新失败："+e.getMessage());
        }
    }


    public static boolean fullPush(String gitDir, String oldRevCommit, String remoteUrl){
        if (! oldRevCommit.equals(Constants.
                GIT_TAIL) && !oldRevCommit.equals(Constants.GIT_TAIL_ZERO)){
            return false;
        }
        File gitDirFile = new File(gitDir);
        if (!gitDirFile.exists()) {
            return false;
        }
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder.setGitDir(gitDirFile).readEnvironment().findGitDir().build();
            // 获取远程分支信息
            Map<String, String> urlNameMap = GitDiffer.remoteUrlNameMap(repository);
            String prefix = "refs/remotes/" + urlNameMap.get(remoteUrl);
            PrePushHook.logDebug("prefix:"+prefix);
            List<Ref> refs = repository.getRefDatabase().getRefsByPrefix(prefix);
            return CollectionUtils.isEmpty(refs);
        } catch (IOException e) {
            return false;
        }
    }

}