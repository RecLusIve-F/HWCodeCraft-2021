package com.huawei.java.pojo;

public class VirtualMachine {
    private String name;
    private int coreNums;
    private int memorySize;
    private int deploymentMode;

    public VirtualMachine(String name, int coreNums, int memorySize, int deploymentMode) {
        this.name = name;
        this.coreNums = coreNums;
        this.memorySize = memorySize;
        this.deploymentMode = deploymentMode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCoreNums() {
        return coreNums;
    }

    public void setCoreNums(int coreNums) {
        this.coreNums = coreNums;
    }

    public int getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }

    public int getDeploymentMode() {
        return deploymentMode;
    }

    public void setDeploymentMode(int deploymentMode) {
        this.deploymentMode = deploymentMode;
    }
}
