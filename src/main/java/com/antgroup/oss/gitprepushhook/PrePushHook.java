/*
 * Ant Group
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.antgroup.oss.gitprepushhook;

import java.io.IOException;

public class PrePushHook{

    private static final String VERSION = "0.1.20231129";

    //    @Override
    public static void main(String... args) throws IOException {
        System.out.println("PrePushHook version: " + VERSION);
    }

    public static void logDebug(String arg) {
        if (true) {
            System.out.println(arg);
        }
    }

}