package com.huawei.java.solver;

import com.huawei.java.main.IOUtils;
import com.huawei.java.pojo.*;

import java.util.*;

public class SolverMigrate {
    private long hardwareCost;
    private long dailyCost;

    private final List<Server> activatedServers;
    private final List<Request> sortedRequests;
    private final Map<String,Integer> hostOrders;
    private final Map<Integer, Pair<Integer, String>> requestMap;
    private final Map<Host, Double> hostWeightMap;
    private final IOUtils ioUtils;
    private int virtualMachineNums;
    private int migrationUpperbound;
    private int currentDay;
    private int migrateCount;
    private final int totalDay;
    private final boolean isTest;
    private final boolean isCheckServer;
    private StringBuilder migrationOutput;

    public SolverMigrate (IOUtils ioUtils, int totalDay, boolean isTest, boolean isCheckServer) {
        this.ioUtils = ioUtils;
        this.totalDay = totalDay;
        this.isTest = isTest;
        this.isCheckServer = isCheckServer;

        hardwareCost = 0;
        dailyCost = 0;

        activatedServers = new ArrayList<>();
        sortedRequests = new ArrayList<>();
        hostOrders = new HashMap<>();
        requestMap = new HashMap<>();
        hostWeightMap = new HashMap<>();
    }

    public void dailyRoutine(int day) {
        currentDay = day;
        migrationUpperbound = (int) Math.floor(virtualMachineNums / 1000.0 * 5);
        updateHostWeight();
//        migrate();
        distribute();
//        updateServerInfo();
    }

    public void distribute() {
        List<Request> requests = ioUtils.getAllRequests().get(currentDay);
        int previousDayServerNums = activatedServers.size();
        for (Request request: requests) {
            if (request.getOperand().equals("add")) {
                sortedRequests.add(request);
            } else {
                // 降序排序
//                sortedRequests.sort((request1, request2) -> {
//                    VirtualMachine virtualMachine1 = ioUtils.getVirtualMachineMap().get(request1.getVirtualMachineName());
//                    VirtualMachine virtualMachine2 = ioUtils.getVirtualMachineMap().get(request2.getVirtualMachineName());
//                    Integer request1Score = virtualMachine1.getCoreNums() + virtualMachine1.getMemorySize();
//                    Integer request2Score = virtualMachine2.getCoreNums() + virtualMachine2.getMemorySize();
//                    return -request1Score.compareTo(request2Score);
//                });
                handleSortedRequests();
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

        if (!sortedRequests.isEmpty())
            handleSortedRequests();

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

        if(!isTest) {
            if (migrateCount == 0)
                System.out.println("(migration, 0)");
            else {
                System.out.println("(migration, " + migrateCount + ")");
                System.out.print(migrationOutput.toString());
            }
        }


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

    public void migrate() {
        List<Integer> migratedVirtualMachineIDs = new ArrayList<>();
        migrationOutput = new StringBuilder();
        migrateCount = 0;
        if (activatedServers.size() < 2)
            return;
        for (int i = activatedServers.size() - 1; i > 0; i --) {
            Server currentServer = activatedServers.get(i);
//            List<Integer> currentServerMigratedVirtualMachineIDs = new ArrayList<>();
            Pair<Integer, Integer> nodeA = currentServer.getNodeA();
            Pair<Integer, Integer> nodeB = currentServer.getNodeB();
//            if (currentServer.getVirtualMachineIDs().size() > 3)
//                return;
            Iterator<Integer> iterator = currentServer.getVirtualMachineIDs().iterator();

//            for (int virtualMachineID: currentServer.getVirtualMachineIDs()) {
            while(iterator.hasNext()) {
                int virtualMachineID = iterator.next();
                if (migrateCount == migrationUpperbound || migratedVirtualMachineIDs.contains(virtualMachineID))
                    return;
                VirtualMachine virtualMachine = ioUtils.getVirtualMachineMap().get(ioUtils.getVirtualMachineIDMap().get(virtualMachineID));
                int coreNums = virtualMachine.getCoreNums();
                int memorySize = virtualMachine.getMemorySize();
                for (int j = i - 1; j > -1; j --) {
                    Server targetServer = activatedServers.get(j);
                    String curDeploymentMode = fitServer(virtualMachine, targetServer);
                    String preDeploymentMode = requestMap.get(virtualMachineID).getE2();
                    if (!curDeploymentMode.equals("False")) {
                        migrationOutput.append("(").append(virtualMachineID).append(", ").append(targetServer.getID()).append(curDeploymentMode.equals("AB") ?"": curDeploymentMode.equals("A")?curDeploymentMode: "B").append(")\n");
                        if (preDeploymentMode.equals("AB")) {
                            coreNums /= 2;
                            memorySize /= 2;
                            currentServer.updateHostState(nodeA, nodeA.getE1() + coreNums, nodeA.getE2() + memorySize);
                            currentServer.updateHostState(nodeB, nodeB.getE1() + coreNums, nodeB.getE2() + memorySize);
                        } else if (preDeploymentMode.equals("A")) {
                            currentServer.updateHostState(nodeA, nodeA.getE1() + coreNums, nodeA.getE2() + memorySize);
                        } else {
                            currentServer.updateHostState(nodeB, nodeB.getE1() + coreNums, nodeB.getE2() + memorySize);
                        }
                        migrateCount ++;
//                        currentServer.getVirtualMachineIDs().remove(virtualMachineID);
                        iterator.remove();
                        targetServer.getVirtualMachineIDs().add(virtualMachineID);
                        requestMap.put(virtualMachineID, new Pair<>(targetServer.getID(), curDeploymentMode));
                        migratedVirtualMachineIDs.add(virtualMachineID);
                        break;
                    }
                }
            }
        }
    }

    private void handleSortedRequests() {
        Label:
        for (Request request: sortedRequests) {
            for (Server server: activatedServers) {
                String deploymentMode = fitServer(ioUtils.getVirtualMachineMap().get(request.getVirtualMachineName()), server);
                if (!deploymentMode.equals("False")) {
//                    if (server.getRemappedId() == 60){
//                        System.out.println(server.getHost().getCoreNums() / 2 + " " + server.getHost().getMemorySize() / 2);
//                        System.out.println(server.getNodeA().getE1() + " " + server.getNodeA().getE2());
//                        System.out.println(server.getNodeB().getE2() + " " + server.getNodeB().getE2());
//                        for (int id: server.getVirtualMachineIDs()) {
//                            VirtualMachine virtualMachine = ioUtils.getVirtualMachineMap().get(ioUtils.getVirtualMachineIDMap().get(id));
//                            System.out.println(virtualMachine.getCoreNums() + " " + virtualMachine.getMemorySize() + " " + virtualMachine.getDeploymentMode());
//                        }
//                    }
                    server.getVirtualMachineIDs().add(request.getVirtualMachineID());
                    requestMap.put(request.getVirtualMachineID(), new Pair<>(server.getID(), deploymentMode));
//                    if (!checkSingleServer(server))
//                        System.out.println("Boom");
                    virtualMachineNums++;
                    continue Label;
                }
            }
            orderHost(ioUtils.getVirtualMachineMap().get(request.getVirtualMachineName()));
            Server newServer = activatedServers.get(activatedServers.size() - 1);
            String deploymentMode = fitServer(ioUtils.getVirtualMachineMap().get(request.getVirtualMachineName()), newServer);
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

    public String fitServer(VirtualMachine virtualMachine, Server server) {
//        VirtualMachine virtualMachine = ioUtils.getVirtualMachineMap().get(request.getVirtualMachineName());
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
        return host.getCoreNums() * 0.01 + host.getMemorySize() * 0.01 + host.getHardwareCost() * 0.5 +
                host.getDailyCost() * (totalDay - currentDay) * 0.48;
    }

    private void updateHostWeight() {
        for (Host host: ioUtils.getHosts())
            hostWeightMap.put(host, getHostWeight(host));
    }

    public <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        Map<K, V> result = new LinkedHashMap<>();
        map.entrySet().stream().sorted(Map.Entry.<K,V>comparingByValue()).forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    public void updateServerInfo() {
        for (Server server: activatedServers) {
            int totalCPUOnNodeA = 0;
            int totalCPUOnNodeB = 0;
            int totalMemoryOnNodeA = 0;
            int totalMemoryOnNodeB = 0;
            int fixedCPU = server.getHost().getCoreNums() / 2;
            int fixedMemory = server.getHost().getMemorySize() / 2;
            Pair<Integer, Integer> nodeA = server.getNodeA();
            Pair<Integer, Integer> nodeB = server.getNodeB();
            for (int virtualMachineID: server.getVirtualMachineIDs()) {
                VirtualMachine virtualMachine = ioUtils.getVirtualMachineMap().get(ioUtils.getVirtualMachineIDMap().get(virtualMachineID));
                int coreNums = virtualMachine.getCoreNums();
                int memorySize = virtualMachine.getMemorySize();
                String deploymentMode = requestMap.get(virtualMachineID).getE2();
                switch (deploymentMode) {
                    case "AB": {
                        coreNums /= 2;
                        memorySize /= 2;
                        totalCPUOnNodeA += coreNums;
                        totalCPUOnNodeB += coreNums;
                        totalMemoryOnNodeA += memorySize;
                        totalMemoryOnNodeB += memorySize;
                        break;
                    }
                    case "A" : {
                        totalCPUOnNodeA += coreNums;
                        totalMemoryOnNodeA += memorySize;
                        break;
                    }
                    case "B" : {
                        totalCPUOnNodeB += coreNums;
                        totalMemoryOnNodeB += memorySize;
                        break;
                    }
                }
            }
            server.updateHostState(nodeA, fixedCPU - totalCPUOnNodeA, fixedMemory - totalMemoryOnNodeA);
            server.updateHostState(nodeB, fixedCPU - totalCPUOnNodeB, fixedMemory - totalMemoryOnNodeB);
        }
    }

    private boolean checkSingleServer(Server server) {
        int totalCPUOnNodeA = 0;
        int totalCPUOnNodeB = 0;
        int totalMemoryOnNodeA = 0;
        int totalMemoryOnNodeB = 0;
        int fixedCPU = server.getHost().getCoreNums() / 2;
        int fixedMemory = server.getHost().getMemorySize() / 2;
        Pair<Integer, Integer> nodeA = server.getNodeA();
        Pair<Integer, Integer> nodeB = server.getNodeB();
        for (int virtualMachineID: server.getVirtualMachineIDs()) {
            VirtualMachine virtualMachine = ioUtils.getVirtualMachineMap().get(ioUtils.getVirtualMachineIDMap().get(virtualMachineID));
            int coreNums = virtualMachine.getCoreNums();
            int memorySize = virtualMachine.getMemorySize();
            String deploymentMode = requestMap.get(virtualMachineID).getE2();
            switch (deploymentMode) {
                case "AB" -> {
                    coreNums /= 2;
                    memorySize /= 2;
                    totalCPUOnNodeA += coreNums;
                    totalCPUOnNodeB += coreNums;
                    totalMemoryOnNodeA += memorySize;
                    totalCPUOnNodeB += memorySize;
                }
                case "A" -> {
                    totalCPUOnNodeA += coreNums;
                    totalMemoryOnNodeA += memorySize;
                }
                case "B" -> {
                    totalCPUOnNodeB += coreNums;
                    totalMemoryOnNodeB += memorySize;
                }
            }
        }
        return nodeA.getE1() == fixedCPU - totalCPUOnNodeA && nodeB.getE1() == fixedCPU - totalCPUOnNodeB && nodeA.getE2() == fixedMemory - totalMemoryOnNodeA && nodeB.getE2() == fixedMemory - totalMemoryOnNodeB;
    }
}
