package com.huawei.java.main;

import com.huawei.java.pojo.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class IOUtils {
    private final List<Host> hosts;
    private final Map<String, Host> hostMap;
    private final Map<String, VirtualMachine> virtualMachineMap;
    private final Map<Integer, String> virtualMachineIDMap;
    private final List<List<Request>> allRequests;
    private final int trainingSetID;

    private BufferedReader bufferedReader;

    public IOUtils(int trainingSetID) {
        hosts = new ArrayList<>();
        hostMap = new HashMap<>();
        virtualMachineMap = new HashMap<>();
        virtualMachineIDMap = new HashMap<>();
        allRequests = new ArrayList<>();
        this.trainingSetID = trainingSetID;
    }

    public String[] parseString(String s) {
        return s.substring(1,s.length()-1).split(", ");
    }

    public int readHost() {
        int hostCount = 0;
        try {
            bufferedReader = new BufferedReader(new FileReader("data/training-" + trainingSetID + ".txt"));
            hostCount = Integer.parseInt(bufferedReader.readLine());
            for (int i = 0; i < hostCount; i ++) {
                String[] hostInfo = parseString(bufferedReader.readLine());
                Host host = new Host(hostInfo[0], Integer.parseInt(hostInfo[1]), Integer.parseInt(hostInfo[2]),
                        Integer.parseInt(hostInfo[3]), Integer.parseInt(hostInfo[4]));
                hosts.add(host);
                hostMap.put(hostInfo[0], host);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hostCount;
    }

    public int readVirtualMachine() {
        int virtualMachineCount = 0;
        try {
            virtualMachineCount = Integer.parseInt(bufferedReader.readLine());
            for (int i = 0; i < virtualMachineCount; i ++) {
                String[] virtualMachineInfo = parseString(bufferedReader.readLine());
                VirtualMachine virtualMachine = new VirtualMachine(virtualMachineInfo[0], Integer.parseInt(virtualMachineInfo[1]),
                        Integer.parseInt(virtualMachineInfo[2]), Integer.parseInt(virtualMachineInfo[3]));
                virtualMachineMap.put(virtualMachineInfo[0], virtualMachine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return virtualMachineCount;
    }

    public int readDailyRequest() {
        int requestNums = 0;
        try {
            requestNums = Integer.parseInt(bufferedReader.readLine());
            List<Request> requests = new ArrayList<>();
            for (int i = 0; i < requestNums; i ++) {
                String[] requestInfo = parseString(bufferedReader.readLine());
                String operand = requestInfo[0];
                String virtualMachineName;
                int virtualMachineID;
                if (operand.equals("add")) {
                    virtualMachineName = requestInfo[1];
                    virtualMachineID = Integer.parseInt(requestInfo[2]);
                    virtualMachineIDMap.put(virtualMachineID, virtualMachineName);
                } else {
                    virtualMachineID = Integer.parseInt(requestInfo[1]);
                    virtualMachineName = virtualMachineIDMap.get(virtualMachineID);
                }
                requests.add(new Request(operand, virtualMachineName, virtualMachineID));
            }
            allRequests.add(requests);
        } catch (IOException e){
            e.printStackTrace();
        }
        return requestNums;
    }

    public int readTotalDays() {
        int days = 0;
        try {
            days = Integer.parseInt(bufferedReader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return days;
    }

    public List<Host> getHosts() {
        return hosts;
    }

    public Map<String, Host> getHostMap() {
        return hostMap;
    }

    public Map<String, VirtualMachine> getVirtualMachineMap() {
        return virtualMachineMap;
    }

    public Map<Integer, String> getVirtualMachineIDMap() {
        return virtualMachineIDMap;
    }

    public List<List<Request>> getAllRequests() {
        return allRequests;
    }
}
