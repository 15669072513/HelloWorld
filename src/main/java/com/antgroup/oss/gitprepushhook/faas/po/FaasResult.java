package com.antgroup.oss.gitprepushhook.faas.po;

@SuppressWarnings("unused")
public class FaasResult<T> {
    private Integer cost;
    private T data;
    private String log;
    private Boolean success;
    private Boolean finish;
    private String msg;
    // getter and setter methods
    public Integer getCost() {
        return cost;
    }
    public void setCost(Integer cost) {
        this.cost = cost;
    }
    public T getData() {
        return data;
    }
    public void setData(T data) {
        this.data = data;
    }
    public String getLog() {
        return log;
    }
    public void setLog(String log) {
        this.log = log;
    }
    public boolean isSuccess() {
        return success;
    }
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public boolean isFinish() {
        return finish;
    }
    public void setFinish(boolean finish) {
        this.finish = finish;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}