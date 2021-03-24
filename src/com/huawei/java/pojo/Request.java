package com.huawei.java.pojo;

public class Request {
    private final String operand;
    private final String virtualMachineName;
    private final int virtualMachineID;

    public String getOperand() {
        return operand;
    }

    public String getVirtualMachineName() {
        return virtualMachineName;
    }

    public int getVirtualMachineID() {
        return virtualMachineID;
    }

    public Request(String operand, String virtualMachineName, int virtualMachineID) {
        this.operand = operand;
        this.virtualMachineName = virtualMachineName;
        this.virtualMachineID = virtualMachineID;
    }
}
