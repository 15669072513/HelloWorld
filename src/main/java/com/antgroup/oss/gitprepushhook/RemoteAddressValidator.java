/*
 * Ant Group
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.antgroup.oss.gitprepushhook;

//import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author khotyn
 * @version RemoteAddressValidator.java, v 0.1 2022年12月10日 09:28 khotyn
 */
//@Component
public class RemoteAddressValidator {
    private final List<String> allowDomains = new ArrayList<>();

    public RemoteAddressValidator() {
        allowDomains.add("code.alipay.com");
        allowDomains.add("gitlab.alipay-inc.com");
    }

    public boolean validate(String remoteAddress) {
        return allowDomains.contains(getDomain(remoteAddress));
    }

    private String getDomain(String remoteAddress) {
        if (!remoteAddress.startsWith("git@")) {
            try {
                URI uri = new URI(remoteAddress);
                return uri.getHost();
            } catch (URISyntaxException e) {
                //never happened
            }
        }
        int atPosition = remoteAddress.indexOf("@");
        int colonPosition = remoteAddress.indexOf(":");
        return remoteAddress.substring(atPosition + 1, colonPosition);
    }
}