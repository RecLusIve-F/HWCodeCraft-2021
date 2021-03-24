package com.huawei.java;

import com.huawei.java.main.IOUtils;
import com.huawei.java.pojo.*;

import java.util.*;

public class Solver1 {
    private long hardwareCost;
    private long dailyCost;

    private final List<Server> activatedServers;
    private final Map<String,Integer> hostOrders;
    private final Map<Integer, Pair<Integer, String>> requestMap;
    private final List<Request> unsortedRequests;
    private final com.huawei.java.main.IOUtils ioUtils;
    private int virtualMachineNums;
    private int migrationUpperbound;
    private int currentDay;
    private final int totalDay;
    private final boolean isTest;

    public Solver1 (IOUtils ioUtils, int totalDay, boolean isTest) {
        this.ioUtils = ioUtils;
        this.totalDay = totalDay;
        this.isTest = isTest;

        hardwareCost = 0;
        dailyCost = 0;

        activatedServers = new ArrayList<>();
        hostOrders = new HashMap<>();
        requestMap = new HashMap<>();
        unsortedRequests = new ArrayList<>();
    }

    public void dailyRoutine(int day) {
        currentDay = day;
        migrationUpperbound = (int) Math.floor(virtualMachineNums / 1000.0 * 5);
//        migrate();
        distribute();
    }

    public void distribute() {
        List<Request> requests = ioUtils.getAllRequests().get(currentDay);
        int previousDayServerNums = activatedServers.size();
        for (Request request: requests) {
            if (request.getOperand().equals("add")) {
                unsortedRequests.add(request);
            } else {
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

        if (!unsortedRequests.isEmpty()) {
            handleSortedRequests();
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
//                System.out.println(request.getVirtualMachineID());
//                System.out.println(activatedServers);
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
        }

    }

    public void migrate() {
        int migrateCount = 0;
        if (activatedServers.size() < 2)
            return;
        for (int i = activatedServers.size() - 1; i > 0; i --) {
            Server currentServer = activatedServers.get(i);
            Pair<Integer, Integer> nodeA = currentServer.getNodeA();
            Pair<Integer, Integer> nodeB = currentServer.getNodeB();
            if (currentServer.getVirtualMachineIDs().size() > 3)
                return;
            for (int virtualMachineID: currentServer.getVirtualMachineIDs()) {
                if (migrateCount == migrationUpperbound)
                    return;
                VirtualMachine virtualMachine = ioUtils.getVirtualMachineMap().get(ioUtils.getVirtualMachineIDMap().get(virtualMachineID));
                int coreNums = virtualMachine.getCoreNums();
                int memorySize = virtualMachine.getMemorySize();
                for (int j = i - 1; j > -1; j --) {
                    Server targetServer = activatedServers.get(j);
                    String deploymentMode = fitServer(virtualMachine, targetServer);
                    if (!deploymentMode.equals("False")) {
                        if (deploymentMode.equals("AB")) {
                            coreNums /= 2;
                            memorySize /= 2;
                            currentServer.updateHostState(nodeA, nodeA.getE1() + coreNums, nodeA.getE2() + memorySize);
                            currentServer.updateHostState(nodeB, nodeB.getE1() + coreNums, nodeB.getE2() + memorySize);
                        } else if (deploymentMode.equals("A")) {
                            currentServer.updateHostState(nodeA, nodeA.getE1() + coreNums, nodeA.getE2() + memorySize);
                        } else {
                            currentServer.updateHostState(nodeB, nodeB.getE1() + coreNums, nodeB.getE2() + memorySize);
                        }
                        migrateCount++;
                        break;
                    }
                }
            }
        }
    }

    private void handleSortedRequests() {
        unsortedRequests.sort((request1, request2) -> {
            VirtualMachine virtualMachine1 = ioUtils.getVirtualMachineMap().get(request1.getVirtualMachineName());
            VirtualMachine virtualMachine2 = ioUtils.getVirtualMachineMap().get(request2.getVirtualMachineName());
            Integer request1Score = virtualMachine1.getCoreNums() + virtualMachine1.getMemorySize();
            Integer request2Score = virtualMachine2.getCoreNums() + virtualMachine2.getMemorySize();
            return request1Score.compareTo(request2Score);
        });

        Label:
        for (Request request: unsortedRequests) {
            VirtualMachine virtualMachine = ioUtils.getVirtualMachineMap().get(ioUtils.getVirtualMachineIDMap().get(request.getVirtualMachineID()));
            for (Server server: activatedServers) {
                String deploymentMode = fitServer(virtualMachine, server);
                if (!deploymentMode.equals("False")) {
                    server.getVirtualMachineIDs().add(request.getVirtualMachineID());
                    requestMap.put(request.getVirtualMachineID(), new Pair<>(server.getID(), deploymentMode));
                    virtualMachineNums++;
//                    sortedRequests.remove(request);
                    continue Label;
                }
            }
            orderHost();
            break;
//            Server newServer = activatedServers.get(activatedServers.size() - 1);
//            String deploymentMode = fitServer(virtualMachine, newServer);
//            requestMap.put(request.getVirtualMachineID(), new Pair<>(newServer.getID(), deploymentMode));
//            virtualMachineNums++;
        }
        unsortedRequests.clear();
    }

    private void orderHost() {
//        Host host = ioUtils.getHosts().get(13);
//        hostOrders.put(host.getName(), hostOrders.getOrDefault(host.getName(), 0) + 1);
//
//        activatedServers.add(activatedServers.size(), new Server(host, activatedServers.size()));
        int totalCoreNums = 0;
        int totalMemorySize = 0;
        Host minHost = ioUtils.getHosts().get(0);
        int minCost;
        int minNums;

        for (Request request: unsortedRequests) {
            VirtualMachine virtualMachine = ioUtils.getVirtualMachineMap().get(request.getVirtualMachineName());
            totalCoreNums += virtualMachine.getCoreNums();
            totalMemorySize += virtualMachine.getMemorySize();
        }

        minNums = getHostNums(totalCoreNums, totalMemorySize, minHost);
        minCost = minNums * minHost.getHardwareCost() + (totalDay - currentDay) * minHost.getDailyCost();

        for (int i = 1; i < ioUtils.getHosts().size(); i ++) {
            Host host = ioUtils.getHosts().get(i);
            int cost;
            int nums;
            nums = getHostNums(totalCoreNums, totalMemorySize, host);
            cost = nums * host.getHardwareCost() + (totalDay - currentDay) * host.getDailyCost();
            System.out.println(cost + " " + nums);
            if (cost < minCost) {
                minCost = cost;
                minHost = host;
                minNums = nums;
            }
        }


        hostOrders.put(minHost.getName(), hostOrders.getOrDefault(minHost.getName(), 0) + minNums);
        int idx = activatedServers.size();
        for (int i = 0; i < minNums; i ++)
            activatedServers.add(activatedServers.size(), new Server(minHost, activatedServers.size()));

        for (Request request: unsortedRequests) {
            VirtualMachine virtualMachine = ioUtils.getVirtualMachineMap().get(ioUtils.getVirtualMachineIDMap().get(request.getVirtualMachineID()));
            for (int i = idx; i < activatedServers.size(); i ++) {
                Server server = activatedServers.get(i);
                String deploymentMode = fitServer(virtualMachine, server);
                if (!deploymentMode.equals("False")) {
                    server.getVirtualMachineIDs().add(request.getVirtualMachineID());
                    requestMap.put(request.getVirtualMachineID(), new Pair<>(server.getID(), deploymentMode));
                    virtualMachineNums++;
                    break;
                }
            }
        }
    }

    public String fitServer(VirtualMachine virtualMachine, Server server) {
//        VirtualMachine virtualMachine = ioUtils.getVirtualMachineMap().get(ioUtils.getVirtualMachineIDMap().get(request.getVirtualMachineID()));
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
        System.out.println("hardwareCost::"+hardwareCost+" dailyCost::"+dailyCost+
                " totalCost::" + (hardwareCost+dailyCost));
        return (hardwareCost+dailyCost);
    }

    public int getHostNums(int totalCoreNums, int totalMemorySize, Host host) {
        int a = (int) Math.ceil(totalMemorySize / (double) host.getMemorySize());
        int b = (int) Math.ceil(totalCoreNums / (double) host.getCoreNums());;
        int c = totalMemorySize > host.getMemorySize()? a == 0? a: a + 1 : 1;
        int d = totalCoreNums > host.getCoreNums()? b == 0? b: b+1 : 1;

        return Math.max(c, d);
    }
}
