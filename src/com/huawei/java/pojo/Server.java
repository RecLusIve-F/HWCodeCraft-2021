package com.huawei.java.pojo;

import java.util.HashSet;
import java.util.Set;

public class Server {
    private int id; // 已购买的服务器中的ID
    private int remappedId; // 同型号批量购买序列中的id

    private Host host;
    private Set<Integer> virtualMachineIDs;
    private Pair<Integer,Integer> nodeA,nodeB;

    public Server(Host host, int id) {
        this.host = host;
        this.id = id;

        remappedId = -1;
        virtualMachineIDs = new HashSet<>();
        nodeA = new Pair<>(host.getCoreNums() / 2, host.getMemorySize() / 2);
        nodeB = new Pair<>(host.getCoreNums() / 2, host.getMemorySize() / 2);
    }

    public boolean isFit(Pair<Integer, Integer> node, int coreNums, int memorySize) {
        return node.getE2() >= memorySize && node.getE1() >= coreNums;
    }

    public void updateHostState(Pair<Integer, Integer> node, int coreNums, int memorySize) {
        node.setE1(coreNums);
        node.setE2(memorySize);
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public int getRemappedId() {
        return remappedId;
    }

    public void setRemappedId(int remappedId) {
        this.remappedId = remappedId;
    }

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public Set<Integer> getVirtualMachineIDs() {
        return virtualMachineIDs;
    }

    public void setVirtualMachineIDs(Set<Integer> virtualMachineIDs) {
        this.virtualMachineIDs = virtualMachineIDs;
    }

    public Pair<Integer, Integer> getNodeA() {
        return nodeA;
    }

    public void setNodeA(Pair<Integer, Integer> nodeA) {
        this.nodeA = nodeA;
    }

    public Pair<Integer, Integer> getNodeB() {
        return nodeB;
    }

    public void setNodeB(Pair<Integer, Integer> nodeB) {
        this.nodeB = nodeB;
    }

}
