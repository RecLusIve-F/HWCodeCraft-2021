package com.huawei.java.pojo;

public class Host {
    private String name;
    private int coreNums;
    private int memorySize;
    private int hardwareCost;
    private int dailyCost;

    public Host(String name, int coreNums, int memorySize, int hardwareCost, int dailyCost) {
        this.name = name;
        this.coreNums = coreNums;
        this.memorySize = memorySize;
        this.hardwareCost = hardwareCost;
        this.dailyCost = dailyCost;
    }
    
    public boolean isFit(VirtualMachine virtualMachine) {
        return virtualMachine.getDeploymentMode() == 1? virtualMachine.getMemorySize() / 2 <= memorySize / 2 &&
                virtualMachine.getCoreNums() / 2 <= coreNums / 2: virtualMachine.getMemorySize() <= memorySize / 2 &&
                virtualMachine.getCoreNums() <= coreNums / 2;
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

    public int getHardwareCost() {
        return hardwareCost;
    }

    public void setHardwareCost(int hardwareCost) {
        this.hardwareCost = hardwareCost;
    }

    public int getDailyCost() {
        return dailyCost;
    }

    public void setDailyCost(int dailyCost) {
        this.dailyCost = dailyCost;
    }

}
