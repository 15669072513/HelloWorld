//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.alipay.antscanlib.sdk;

import com.alibaba.common.lang.StringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.antscanlib.algo.AlgoResultItem;
import com.alipay.antscanlib.algo.HitData;
import com.alipay.antscanlib.component.service.ResultTraverse;
import com.alipay.antscanlib.data.KeymapResult;
import com.alipay.antscanlib.data.KeymapResultDetail;
import com.alipay.antscanlib.sdk.util.ShardUtil;
import com.alipay.antscanlib.util.CryptUtils;
import com.alipay.antscanlib.util.StringUtils;
import com.alipay.tianqian.service.OBService;
import com.alipay.tianqiansecret.recognizers.HighAccAccessKeyRecognizer;
import com.alipay.tianqiansecret.scanner.StcSecretScanner;

import java.util.*;
import java.util.concurrent.*;

public class KeymapFastRecognizer {
    StcSecretScanner akPemPasswordScanner;
    OBService urlEmailScanner;
    HighAccAccessKeyRecognizer highAccAccessKeyRecognizer;
    private ExecutorService executorService;
    long timeout;
    private boolean isShard;
    String scenCode;

    public boolean init(Set<String> sensCodeSet, long timeout) throws Exception {
        this.highAccAccessKeyRecognizer = new HighAccAccessKeyRecognizer();
        this.timeout = timeout;
        return true;
    }

    public boolean init(String scenCode, long timeout) throws Exception {
        this.scenCode = scenCode;
        if (StringUtil.equals(scenCode, "HIGH_ACC")) {
            this.highAccAccessKeyRecognizer = new HighAccAccessKeyRecognizer();
            return true;
        } else {
            this.akPemPasswordScanner = new StcSecretScanner();
            this.urlEmailScanner = new OBService();
            this.timeout = timeout;
            return true;
        }
    }

    public void shutdown() {
        if (this.executorService != null) {
            this.executorService.shutdown();
        }

    }

    public List<KeymapResult> evaluateArray(List<String> content, Map<String, Object> meta) {
        return null;
    }

    public List<KeymapResult> evaluateText(final String text, final Map<String, Object> meta) throws Exception {
        if (!meta.containsKey("url")) {
            meta.put("url", "a.java");
        }

        Future<List<KeymapResult>> future = this.executorService.submit(new Callable<List<KeymapResult>>() {
            public List<KeymapResult> call() throws Exception {
                return KeymapFastRecognizer.this.doEvaluateText(text, meta);
            }
        });
        List<KeymapResult> keymapResults = (List)future.get(this.timeout, TimeUnit.MILLISECONDS);
        return keymapResults;
    }
    public List<KeymapResult> doEvaluateText(String fileName, String text) {
        return doEvaluateText(text, Map.of("url", fileName));
    }
    private List<KeymapResult> doEvaluateText(String text, Map<String, Object> meta) {
        if (!this.isShard) {
            List<AlgoResultItem> algoResultItemList = this.doOneEvaluate(text, meta);
            return ResultTraverse.algoReusltToKeymapResult(this.resultFiltered(algoResultItemList));
        } else {
            Content content = new Content();
            content.setData(text);
            content.setOffset(0L);
            Map<String, KeymapResult> keymapResultMap = new HashMap();
            List<Content> contentList = ShardUtil.contentShard(content);
            Iterator var6 = contentList.iterator();

            while(var6.hasNext()) {
                Content oneShard = (Content)var6.next();
                List<AlgoResultItem> algoResultItemList = this.doOneEvaluate(oneShard.getData(), meta);
                List<KeymapResult> keymapResults = ResultTraverse.algoReusltToKeymapResult(this.resultFiltered(algoResultItemList));
                ShardUtil.resultMerge(keymapResultMap, keymapResults, oneShard);
            }

            return new ArrayList(keymapResultMap.values());
        }
    }

    private List<AlgoResultItem> doOneEvaluate(String text, Map<String, Object> meta) {
        List<AlgoResultItem> result = new ArrayList();
        List results;
        List algoResultItemList;
        if (StringUtil.equals(this.scenCode, "HIGH_ACC")) {
            results = this.highAccAccessKeyRecognizer.recognize(text);
            algoResultItemList = this.highAccAccessKeyRecognizer.formatResult(results);
            result.addAll(algoResultItemList);
        } else {
            results = this.akPemPasswordScanner.scan((JSONObject)JSONObject.toJSON(meta), text);
            algoResultItemList = this.akPemPasswordScanner.formatResult(results, 0.8);
            result.addAll(algoResultItemList);
            results = this.urlEmailScanner.recognizer("", text);
            result.addAll(results);
        }

        return result;
    }

    public List<KeymapResult> akVerification(final List<KeymapResult> keymapResults) throws Exception {
        Future<List<KeymapResult>> future = this.executorService.submit(new Callable<List<KeymapResult>>() {
            public List<KeymapResult> call() throws Exception {
                return KeymapFastRecognizer.this.doAkVerification(keymapResults);
            }
        });
        List<KeymapResult> keymapResultList = (List)future.get(this.timeout, TimeUnit.MILLISECONDS);
        return keymapResultList != null ? keymapResultList : keymapResults;
    }

    public List<KeymapResult> doAkVerification(List<KeymapResult> keymapResults) {
        Iterator var2 = keymapResults.iterator();

        while(true) {
            KeymapResult keymapResult;
            do {
                if (!var2.hasNext()) {
                    return keymapResults;
                }

                keymapResult = (KeymapResult)var2.next();
            } while(!StringUtils.equals(keymapResult.getRuleCode(), "AK"));

            Iterator var4 = keymapResult.getDetail().iterator();

            while(var4.hasNext()) {
                KeymapResultDetail keymapResultDetail = (KeymapResultDetail)var4.next();
                if (keymapResultDetail.getData().containsKey("id") && keymapResultDetail.getData().containsKey("key")) {
                    String akId = (String)keymapResultDetail.getData().get("id");
                    String akKey = CryptUtils.aesDecrypt((String)keymapResultDetail.getData().get("key"), "keymap");
                    keymapResultDetail.getData().put("validCheck", AkVerification.checkAk(akId, akKey));
                }
            }
        }
    }

    public String resultFiltered(List<AlgoResultItem> algoResultItems) {
        if (algoResultItems != null && !algoResultItems.isEmpty()) {
            JSONArray jsonArray = new JSONArray();

            for(int i = 0; i < algoResultItems.size(); ++i) {
                try {
                    AlgoResultItem algoResultItem = (AlgoResultItem)algoResultItems.get(i);
                    if (algoResultItem.getHitData() != null) {
                        Iterator var5 = algoResultItem.getHitData().iterator();

                        HitData hitData;
                        while(var5.hasNext()) {
                            hitData = (HitData)var5.next();
                            if (hitData.getData().containsKey("key")) {
                                String orgKey = (String)hitData.getData().get("key");
                                hitData.getData().put("key", CryptUtils.aesEncrypt(orgKey, "keymap"));
                                hitData.getData().put("desens_key", CryptUtils.desensKey(orgKey));
                            }
                        }

                        var5 = algoResultItem.getHitData().iterator();

                        while(var5.hasNext()) {
                            hitData = (HitData)var5.next();
                            this.mapTruncate(hitData.getData());
                            this.mapTruncate(hitData.getDetail());
                        }

                        if (algoResultItem.getHitData().size() > 20) {
                            algoResultItem.setHitData(algoResultItem.getHitData().subList(0, 20));
                        }

                        if (JSONObject.toJSONString(algoResultItem).length() > 32768) {
                            algoResultItem.getHitData().clear();
                        }
                    }

                    jsonArray.add(JSONObject.toJSON(algoResultItem));
                } catch (Exception var8) {
//                    LogUtil.error(var8, LOGGER, "try put data error. ", new Object[0]);
                }
            }

            return jsonArray.toJSONString();
        } else {
            return "[]";
        }
    }

    private void mapTruncate(Map<String, Object> objectMap) {
        if (objectMap != null) {
            Iterator var2 = objectMap.entrySet().iterator();

            while(var2.hasNext()) {
                Map.Entry<String, Object> entry = (Map.Entry)var2.next();
                if (entry.getValue() instanceof String && String.valueOf(entry.getValue()).length() > 2048) {
                    String value = (String)entry.getValue();
                    entry.setValue(value.substring(0, 2048));
                }
            }
        }

    }

    public KeymapFastRecognizer() {
        this.executorService = new ThreadPoolExecutor(5, 20, 120L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque(20));
        this.timeout = 600000L;
        this.isShard = false;
        this.scenCode = "HIGH_ACC";
    }

    public StcSecretScanner getAkPemPasswordScanner() {
        return this.akPemPasswordScanner;
    }

    public OBService getUrlEmailScanner() {
        return this.urlEmailScanner;
    }

    public HighAccAccessKeyRecognizer getHighAccAccessKeyRecognizer() {
        return this.highAccAccessKeyRecognizer;
    }

    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    public long getTimeout() {
        return this.timeout;
    }

    public boolean isShard() {
        return this.isShard;
    }

    public String getScenCode() {
        return this.scenCode;
    }

    public void setAkPemPasswordScanner(StcSecretScanner akPemPasswordScanner) {
        this.akPemPasswordScanner = akPemPasswordScanner;
    }

    public void setUrlEmailScanner(OBService urlEmailScanner) {
        this.urlEmailScanner = urlEmailScanner;
    }

    public void setHighAccAccessKeyRecognizer(HighAccAccessKeyRecognizer highAccAccessKeyRecognizer) {
        this.highAccAccessKeyRecognizer = highAccAccessKeyRecognizer;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setShard(boolean isShard) {
        this.isShard = isShard;
    }

    public void setScenCode(String scenCode) {
        this.scenCode = scenCode;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof KeymapFastRecognizer)) {
            return false;
        } else {
            KeymapFastRecognizer other = (KeymapFastRecognizer)o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                label79: {
                    Object this$akPemPasswordScanner = this.getAkPemPasswordScanner();
                    Object other$akPemPasswordScanner = other.getAkPemPasswordScanner();
                    if (this$akPemPasswordScanner == null) {
                        if (other$akPemPasswordScanner == null) {
                            break label79;
                        }
                    } else if (this$akPemPasswordScanner.equals(other$akPemPasswordScanner)) {
                        break label79;
                    }

                    return false;
                }

                Object this$urlEmailScanner = this.getUrlEmailScanner();
                Object other$urlEmailScanner = other.getUrlEmailScanner();
                if (this$urlEmailScanner == null) {
                    if (other$urlEmailScanner != null) {
                        return false;
                    }
                } else if (!this$urlEmailScanner.equals(other$urlEmailScanner)) {
                    return false;
                }

                Object this$highAccAccessKeyRecognizer = this.getHighAccAccessKeyRecognizer();
                Object other$highAccAccessKeyRecognizer = other.getHighAccAccessKeyRecognizer();
                if (this$highAccAccessKeyRecognizer == null) {
                    if (other$highAccAccessKeyRecognizer != null) {
                        return false;
                    }
                } else if (!this$highAccAccessKeyRecognizer.equals(other$highAccAccessKeyRecognizer)) {
                    return false;
                }

                label58: {
                    Object this$executorService = this.getExecutorService();
                    Object other$executorService = other.getExecutorService();
                    if (this$executorService == null) {
                        if (other$executorService == null) {
                            break label58;
                        }
                    } else if (this$executorService.equals(other$executorService)) {
                        break label58;
                    }

                    return false;
                }

                if (this.getTimeout() != other.getTimeout()) {
                    return false;
                } else if (this.isShard() != other.isShard()) {
                    return false;
                } else {
                    Object this$scenCode = this.getScenCode();
                    Object other$scenCode = other.getScenCode();
                    if (this$scenCode == null) {
                        if (other$scenCode != null) {
                            return false;
                        }
                    } else if (!this$scenCode.equals(other$scenCode)) {
                        return false;
                    }

                    return true;
                }
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof KeymapFastRecognizer;
    }

    public int hashCode() {
        boolean PRIME = true;
        int result = 1;
        Object $akPemPasswordScanner = this.getAkPemPasswordScanner();
        result = result * 59 + ($akPemPasswordScanner == null ? 43 : $akPemPasswordScanner.hashCode());
        Object $urlEmailScanner = this.getUrlEmailScanner();
        result = result * 59 + ($urlEmailScanner == null ? 43 : $urlEmailScanner.hashCode());
        Object $highAccAccessKeyRecognizer = this.getHighAccAccessKeyRecognizer();
        result = result * 59 + ($highAccAccessKeyRecognizer == null ? 43 : $highAccAccessKeyRecognizer.hashCode());
        Object $executorService = this.getExecutorService();
        result = result * 59 + ($executorService == null ? 43 : $executorService.hashCode());
        long $timeout = this.getTimeout();
        result = result * 59 + (int)($timeout >>> 32 ^ $timeout);
        result = result * 59 + (this.isShard() ? 79 : 97);
        Object $scenCode = this.getScenCode();
        result = result * 59 + ($scenCode == null ? 43 : $scenCode.hashCode());
        return result;
    }

    public String toString() {
        return "KeymapFastRecognizer(akPemPasswordScanner=" + this.getAkPemPasswordScanner() + ", urlEmailScanner=" + this.getUrlEmailScanner() + ", highAccAccessKeyRecognizer=" + this.getHighAccAccessKeyRecognizer() + ", executorService=" + this.getExecutorService() + ", timeout=" + this.getTimeout() + ", isShard=" + this.isShard() + ", scenCode=" + this.getScenCode() + ")";
    }
}
