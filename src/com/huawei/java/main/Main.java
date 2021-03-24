package com.huawei.java.main;

public class Main {

    public static void main(String[] args) {
	// write your code here
        long totalCost = 0;
        for (int i = 1; i < 3; i ++) {
            IOUtils ioUtils = new IOUtils(i);
            DebugTool debugTool = new DebugTool();
            int hostNums = ioUtils.readHost();
            int virtualMachineNums = ioUtils.readVirtualMachine();
            int days = ioUtils.readTotalDays();
            Solver solver = new Solver(ioUtils, days, true);
            for (int j = 0; j < days; j ++) {
                int requestNums = ioUtils.readDailyRequest();
                solver.dailyRoutine(j);
            }
            totalCost += solver.displayCost();
        }
        System.out.println(totalCost);
        System.out.println(1213528723);
    }
}
