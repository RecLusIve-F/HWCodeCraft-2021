package com.huawei.java.solver;

import com.huawei.java.main.IOUtils;
import com.huawei.java.pojo.*;

import java.util.*;

public class SolverBaseline {
    private long hardwareCost;
    private long dailyCost;

    private final List<Server> activatedServers;
    private final Map<String,Integer> hostOrders;
    private final Map<Integer, Pair<Integer, String>> requestMap;
    private final Map<Host, Double> hostWeightMap;
    private final List<Request> sortedRequests;
    private final IOUtils ioUtils;
    private int virtualMachineNums;
    private int currentDay;
    private final int totalDay;
    private final boolean isTest;
    private final boolean isCheckServer;

    public SolverBaseline(IOUtils ioUtils, int totalDay, boolean isTest, boolean isCheckServer) {
        this.ioUtils = ioUtils;
        this.totalDay = totalDay;
        this.isTest = isTest;
        this.isCheckServer = isCheckServer;

        hardwareCost = 0;
        dailyCost = 0;

        activatedServers = new ArrayList<>();
        hostOrders = new HashMap<>();
        requestMap = new HashMap<>();
        hostWeightMap = new HashMap<>();
        sortedRequests = new ArrayList<>();
    }

    public void dailyRoutine(int day) {
        currentDay = day;
        updateHostWeight();
        distribute();
    }

    public void distribute() {
        List<Request> requests = ioUtils.getAllRequests().get(currentDay);
        int previousDayServerNums = activatedServers.size();
        for (Request request: requests) {
            if (request.getOperand().equals("add")) {
                Label:
                while (true) {
                    for (Server server: activatedServers) {
                        String deploymentMode = fitServer(request, server);
                        if (!deploymentMode.equals("False")) {
                            server.getVirtualMachineIDs().add(request.getVirtualMachineID());
                            requestMap.put(request.getVirtualMachineID(), new Pair<>(server.getID(), deploymentMode));
                            virtualMachineNums++;
                            break Label;
                        }
                    }
                    orderHost(ioUtils.getVirtualMachineMap().get(request.getVirtualMachineName()));
                }
            } else {
                Pair<Integer, String> pair = requestMap.get(request.getVirtualMachineID());
                Server server = activatedServers.get(pair.getE1());
                VirtualMachine virtualMachine = ioUtils.getVirtualMachineMap().get(request.getVirtualMachineName());
                Pair<Integer, Integer> nodeA = server.getNodeA();
                Pair<Integer, Integer> nodeB = server.getNodeB();
                int coreNums = virtualMachine.getCoreNums();
                int memorySize = virtualMachine.getMemorySize();

                if (pair.getE2().equals("AB")) {
                    coreNums /= 2;
                    memorySize /= 2;
                    server.updateHostState(nodeA, nodeA.getE1() + coreNums, nodeA.getE2() + memorySize);
                    server.updateHostState(nodeB, nodeB.getE1() + coreNums, nodeB.getE2() + memorySize);
                } else if (pair.getE2().equals("A")) {
                    server.updateHostState(nodeA, nodeA.getE1() + coreNums, nodeA.getE2() + memorySize);
                } else {
                    server.updateHostState(nodeB, nodeB.getE1() + coreNums, nodeB.getE2() + memorySize);
                }
                virtualMachineNums--;
                server.getVirtualMachineIDs().remove(request.getVirtualMachineID());
                requestMap.remove(request.getVirtualMachineID());
            }
        }

        int currentServerNums = activatedServers.size();
        int currentID = previousDayServerNums;
        if (!isTest)
            System.out.println("(purchase, " + hostOrders.size()+")");
        for (int i = 0; i < currentServerNums ; i++) {
            Server server = activatedServers.get(i);
            // 重映射
            if (server.getRemappedId() == -1) {
                int num = hostOrders.get(server.getHost().getName());
                if (!isTest)
                    System.out.println("("+server.getHost().getName()+", " + num + ")");
                hardwareCost += (long) server.getHost().getHardwareCost() * num;
                for (int j = i; j < currentServerNums ; j++) {
                    if (server.getHost().getName().equals(activatedServers.get(j).getHost().getName())) {
                        activatedServers.get(j).setRemappedId(currentID++);
                    }
                }
            }
        }

        hostOrders.clear();

        if(!isTest)
            System.out.println("(migration, 0)");

        for (Request request: requests) {
            if (request.getOperand().equals("add")) {
                Pair<Integer,String> pair = requestMap.get(request.getVirtualMachineID());
                Server server = activatedServers.get(pair.getE1());
                String deploymentMode = pair.getE2();
                if (!isTest)
                    System.out.print("("+server.getRemappedId());
                if (deploymentMode.equals("AB") && !isTest) {
                    System.out.println(")");
                }else if (deploymentMode.equals("A") && !isTest) {
                    System.out.println(", A)");
                }else if(!isTest){
                    System.out.println(", B)");
                }
            }
        }

        for (Server server : activatedServers) {
            if (!server.getVirtualMachineIDs().isEmpty()) {
                dailyCost += server.getHost().getDailyCost();
            }
            if (isCheckServer) {
                int getCpuFromNode = server.getNodeA().getE1() + server.getNodeB().getE1();
                int getMemoryFromNode = server.getNodeA().getE2() + server.getNodeB().getE2();
                int getCpuFromVirtualMachineIDs = 0;
                int getMemoryFromVirtualMachineIDs = 0;
                int fixedCpu = server.getHost().getCoreNums();
                int fixedMemory = server.getHost().getMemorySize();

                for (int virtualMachineID: server.getVirtualMachineIDs()) {
                    VirtualMachine virtualMachine = ioUtils.getVirtualMachineMap().get(ioUtils.getVirtualMachineIDMap().get(virtualMachineID));
                    getCpuFromVirtualMachineIDs += virtualMachine.getCoreNums();
                    getMemoryFromVirtualMachineIDs += virtualMachine.getMemorySize();
                }

                if (fixedCpu - getCpuFromNode != getCpuFromVirtualMachineIDs || fixedMemory - getMemoryFromNode != getMemoryFromVirtualMachineIDs)
                    System.out.println(server.getID());
            }
        }
    }

    private void handleSortedRequests() {
        Label:
        for (Request request: sortedRequests) {
            for (Server server: activatedServers) {
                String deploymentMode = fitServer(request, server);
                if (!deploymentMode.equals("False")) {
                    server.getVirtualMachineIDs().add(request.getVirtualMachineID());
                    requestMap.put(request.getVirtualMachineID(), new Pair<>(server.getID(), deploymentMode));
                    virtualMachineNums++;
                    continue Label;
                }
            }
            orderHost(ioUtils.getVirtualMachineMap().get(request.getVirtualMachineName()));
            Server newServer = activatedServers.get(activatedServers.size() - 1);
            String deploymentMode = fitServer(request, newServer);
            newServer.getVirtualMachineIDs().add(request.getVirtualMachineID());
            requestMap.put(request.getVirtualMachineID(), new Pair<>(newServer.getID(), deploymentMode));
            virtualMachineNums++;
        }
        sortedRequests.clear();
    }

    private void orderHost(VirtualMachine virtualMachine) {
//        Map<Host, Double> resultMap = sortByValue(hostWeightMap);
//        Host host = null;
//        for (Host host1: resultMap.keySet()) {
//            if (host1.isFit(virtualMachine)) {
//                host = host1;
//                break;
//            }
//        }
        Host host = ioUtils.getHosts().get(13);
        assert host != null;
        hostOrders.put(host.getName(), hostOrders.getOrDefault(host.getName(), 0) + 1);

        activatedServers.add(activatedServers.size(), new Server(host, activatedServers.size()));
    }

    public String fitServer(Request request, Server server) {
        VirtualMachine virtualMachine = ioUtils.getVirtualMachineMap().get(ioUtils.getVirtualMachineIDMap().get(request.getVirtualMachineID()));
        int coreNums = virtualMachine.getCoreNums();
        int memorySize = virtualMachine.getMemorySize();
        Pair<Integer, Integer> nodeA = server.getNodeA();
        Pair<Integer, Integer> nodeB = server.getNodeB();
        int deploymentMode = virtualMachine.getDeploymentMode();

        if (deploymentMode == 1) {
            coreNums /= 2;
            memorySize /= 2;

            if (server.isFit(nodeA, coreNums, memorySize) && server.isFit(nodeB, coreNums, memorySize)) {
                server.updateHostState(nodeA, nodeA.getE1() - coreNums, nodeA.getE2() - memorySize);
                server.updateHostState(nodeB, nodeB.getE1() - coreNums, nodeB.getE2() - memorySize);
                return "AB";
            }
        } else {
            if (server.isFit(nodeA, coreNums, memorySize)) {
                server.updateHostState(nodeA, nodeA.getE1() - coreNums, nodeA.getE2() - memorySize);
                return "A";
            } else if(server.isFit(nodeB, coreNums, memorySize)) {
                server.updateHostState(nodeB, nodeB.getE1() - coreNums, nodeB.getE2() - memorySize);
                return "B";
            }
        }
        return "False";
    }

    public long displayCost() {
        if (!isTest)
            System.out.println("hardwareCost::"+hardwareCost+" dailyCost::"+dailyCost+ " totalCost::" + (hardwareCost+dailyCost));
        return (hardwareCost+dailyCost);
    }

    private double getHostWeight(Host host) {
        return host.getCoreNums() * 0.75 + host.getMemorySize() * 0.22 + host.getHardwareCost() * 0.01 +
                host.getDailyCost() * (totalDay - currentDay) * 0.01;
    }

    private void updateHostWeight() {
        for (Host host: ioUtils.getHosts())
            hostWeightMap.put(host, getHostWeight(host));
    }

    public <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        Map<K, V> result = new LinkedHashMap<>();
        map.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }
}
