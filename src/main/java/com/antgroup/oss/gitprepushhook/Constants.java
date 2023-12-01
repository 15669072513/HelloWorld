/*
 * Ant Group
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.antgroup.oss.gitprepushhook;

/**
 * @author khotyn
 * @version Constants.java, v 0.1 2022年12月12日 14:11 khotyn
 */
public class Constants {
    // the very beginning of the git tree.
    public static final String GIT_TAIL = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";
    // Some as GIT_TAIL, the pre-push hook use all zero ref(actually not exist) as the very beginning of the git tree.
    public static final String GIT_TAIL_ZERO = "0000000000000000000000000000000000000000";
}