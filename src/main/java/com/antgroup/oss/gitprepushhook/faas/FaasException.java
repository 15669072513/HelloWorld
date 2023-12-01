package com.antgroup.oss.gitprepushhook.faas;

public class FaasException extends RuntimeException{
    public FaasException(String message){
        super(message);
    }
}
