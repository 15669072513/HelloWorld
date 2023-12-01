/*
 * Ant Group
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.antgroup.oss.gitprepushhook;

import com.antgroup.oss.gitprepushhook.bo.AuthorCommitInfo;
import com.antgroup.oss.gitprepushhook.bo.ContentKey;
import com.antgroup.oss.gitprepushhook.bo.GitDiffResult;
import com.antgroup.oss.gitprepushhook.scanignore.GitIgnore;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.antgroup.oss.gitprepushhook.PrePushHook.logDebug;

/**
 * @author khotyn
 * @version GitDiffer.java, v 0.1 2022年12月10日 11:41 khotyn
 */
//@Component
public class GitDiffer {
    /**
     * Get the diff of two commits.
     *
     * @param gitDir       A git directory, ends with ".git"
     * @param oldRevCommit The older revision commit.
     * @param newRevCommit The new revision commit.
     * @throws IOException Exception while fetching .git metadata.
     */
    public GitDiffResult deepDiff(String gitDir, String oldRevCommit, String newRevCommit, String remoteUrl, Consumer<ContentKey> diffConsumer, GitIgnore gitIgnore) throws IOException {
        GitDiffResult gitDiffResult = new GitDiffResult();
        gitDiffResult.setRemoteUrl(remoteUrl);
        if (StringUtils.isEmpty(gitDir)) {
            throw new IllegalArgumentException("Git directory should not be empty");
        }

        File gitDirFile = new File(gitDir);

        if (!gitDirFile.exists()) {
            throw new IllegalArgumentException("Git directory " + gitDir + " does not exist");
        }


        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(gitDirFile).readEnvironment().findGitDir().build()) {
            ObjectId commitId = ObjectId.fromString(newRevCommit);
            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(commitId);
                revWalk.markStart(commit);
                logDebug("beginCommit:"+commit.getId().getName());
                revWalk.sort(RevSort.TOPO);
                // 获取远程分支信息
                Map<String, String> urlNameMap = remoteUrlNameMap(repository);
                String prefix = "refs/remotes/" + urlNameMap.get(remoteUrl);
                logDebug("prefix:"+prefix);
                List<Ref> refs = repository.getRefDatabase().getRefsByPrefix(prefix);
                commitLoop:
                for (RevCommit revCommit : revWalk) {
                    //首次checkout -b的分支,匹配到任意远程分支,则视前面的commit为修改内容
                    logDebug("oldRevCommit:"+oldRevCommit);
                    if (oldRevCommit.equals(Constants.GIT_TAIL)||oldRevCommit.equals(Constants.GIT_TAIL_ZERO)){
                        logDebug("checkout -b: true");
                        for (Ref ref : refs) {
                            logDebug("match?:"+ref.getObjectId().getName().equals(revCommit.getId().getName()));
                            if (ref.getObjectId().getName().equals(revCommit.getId().getName())) {
                                logDebug("matched remote ref, break!");
                                break commitLoop;
                            }
                        }
                    }
                    if (revCommit.getId().getName().equals(oldRevCommit)) {
                        logDebug("当前提交commmitid一样");
                        break;
                    }
                    gitDiffResult.getAllChangeCommit().add(new AuthorCommitInfo(revCommit.getId().getName(),
                            revCommit.getAuthorIdent().getEmailAddress()));

                    if (revCommit.getParents().length > 0) {
                        logDebug("parent:");
                        for (RevCommit parentCommit : revCommit.getParents()) {
                            doDiff(repository, parentCommit.getId().getName(), revCommit, diffConsumer, gitIgnore,remoteUrl,gitDiffResult);
                        }
                    } else {
                        logDebug("not parent:");
                        doDiff(repository, Constants.GIT_TAIL, revCommit, diffConsumer, gitIgnore,remoteUrl,gitDiffResult);
                    }
                }
            }catch (Exception e){
                logDebug("RevWalk revWalk = new RevWalk(repository):"+e.getMessage());
            }
        }catch (Exception e){
            logDebug("builder.setGitDir(gitDirFile).readEnvironment().findGitDir().build():"+e.getMessage());
        }
        return gitDiffResult;
    }

    private void doDiff(Repository repository, String oldRevCommit, RevCommit newRevCommit, Consumer<ContentKey> diffConsumer, GitIgnore gitIgnore, String remoteUrl, GitDiffResult gitDiffResult) throws IOException {
        ObjectReader reader = repository.newObjectReader();
        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        ObjectId newTree = repository.resolve(newRevCommit.getId().getName() + "^{tree}");
        newTreeIter.reset(reader, newTree);

        if (StringUtils.equals(oldRevCommit, Constants.GIT_TAIL)) { // Just walk the tree
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(newTree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                String fileName = treeWalk.getPathString();
                if (gitIgnore.isExcluded(fileName)) {
                    logDebug("ignore: "+ fileName);
                    continue;
                }
                if (treeWalk.isSubtree()) {
                    logDebug("enterSubtree");
                    treeWalk.enterSubtree();
                } else {
                    ObjectId blobId = treeWalk.getObjectId(0);
                    try (ObjectReader objectReader = repository.newObjectReader()) {
                        ObjectLoader objectLoader = objectReader.open(blobId);
                        byte[] bytes = objectLoader.getBytes();
                        if (RawText.isBinary(bytes)) { // Skip binary file
                            logDebug("File " + fileName + " is binary");
                            continue;
                        }
                        // TODO Not every file is UTF-8
                        diffConsumer.accept(new ContentKey(newRevCommit, fileName, List.of(1), new String(bytes, StandardCharsets.UTF_8),remoteUrl,newRevCommit.getId().getName()));
                    }catch (Exception e){
                        logDebug("newObjectReader失败");
                    }
                }
            }
        } else { // Diff between new commit and old commit.
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            ObjectId oldTree = repository.resolve(oldRevCommit + "^{tree}");
            oldTreeIter.reset(reader, oldTree);


            DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream());
            df.setRepository(repository);
            List<DiffEntry> entries = df.scan(oldTreeIter, newTreeIter);

            //这里改成全部的，addedLines还是为空，不影响原有逻辑
//            List<DiffEntry> addModifyEntries = entries.stream().filter(diffEntry -> !diffEntry.getChangeType().equals(DiffEntry.ChangeType.DELETE) && !diffEntry.getChangeType().equals(DiffEntry.ChangeType.RENAME)).toList();
            entries.forEach(entry -> {
                String fileName = entry.getNewPath();
                if (gitIgnore.isExcluded(fileName)) {
                    logDebug("ignore: "+ fileName);
                    return;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (AddContentCollectDiffFormatter formatter = new AddContentCollectDiffFormatter(baos)) {
                    formatter.setRepository(repository);
                    formatter.format(entry);
                    SortedMap<Integer, String> addedLines = formatter.getAddedLines();
                    //统计代码行和size
                    SortedMap<Integer, String> changeLines = formatter.getAllChangeLines();
                    gitDiffResult.getAllChangeLines().addAndGet(changeLines.size());
                    gitDiffResult.getAllChangeSize().addAndGet(changeLines.values().stream().mapToLong(String::length).sum());
                    Map<List<Integer>, String> sectionMap = groupBySection(addedLines);
                    logDebug("sectionMap："+sectionMap.size());
                    for (Map.Entry<List<Integer>, String> codeSectionEntry : sectionMap.entrySet()) {
                        diffConsumer.accept(new ContentKey(newRevCommit, fileName, codeSectionEntry.getKey(), codeSectionEntry.getValue(),remoteUrl,newRevCommit.getId().getName()));
                    }
                } catch (Exception e) {
                    System.err.println("Get diff entry of file " + fileName + " error.");
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * 将连续行视为代码段,进行分组.
     */
    public Map<List<Integer>, String> groupBySection(SortedMap<Integer, String> lineMap) {
        if (lineMap.size()==0) {
            return Map.of();
        }
        if (lineMap.size()==1) {
            Integer line = lineMap.firstKey();
            return Map.of(List.of(line),lineMap.get(line));
        }
        Map<List<Integer>, String> result = new HashMap<>();
        List<Integer> lineList = lineMap.keySet().stream().sorted().toList();
        List<Integer> temp = new ArrayList<>();
        StringBuilder content = new StringBuilder();
        int prev = -1;

        for (int curr : lineList) {
            if (prev != -1 && curr != prev + 1) {
                result.put(temp, content.substring(1));
                temp = new ArrayList<>();
                content = new StringBuilder();
            }
            temp.add(curr);
            content.append("\n").append(lineMap.get(curr));
            prev = curr;
        }
        result.put(temp, content.substring(1));
        return result;
    }

    /**
     * 获取远程url与name的map,
     */
    @SneakyThrows
    public static Map<String, String> remoteUrlNameMap(Repository repository){
        return RemoteConfig.getAllRemoteConfigs(repository.getConfig()).stream()
                .collect(Collectors.toMap(
                        remoteConfig -> remoteConfig.getURIs().get(0).toString(),
                        RemoteConfig::getName
                ));
    }
}