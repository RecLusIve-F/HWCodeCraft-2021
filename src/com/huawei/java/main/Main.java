package com.huawei.java.main;

import com.huawei.java.solver.*;

public class Main {

    public static void main(String[] args) {
	// write your code here
        long totalCost = 0;
        for (int i = 1; i < 2; i ++) {
            IOUtils ioUtils = new IOUtils(i);
            DebugTool debugTool = new DebugTool();
            int hostNums = ioUtils.readHost();
            int virtualMachineNums = ioUtils.readVirtualMachine();
            int days = ioUtils.readTotalDays();
            Solver solver = new Solver(ioUtils, days, false, true);
//            SolverBaseline solver = new SolverBaseline(ioUtils, days, false, false);
//            SolverMigrate solver = new SolverMigrate(ioUtils, days, false, true);
            for (int j = 0; j < days; j ++) {
                int requestNums = ioUtils.readDailyRequest();
                solver.dailyRoutine(j);
            }
            totalCost += solver.displayCost();
        }
        System.out.println(totalCost  + "\tnow");
        System.out.println(1167923183 + "\tsort & hostWeight & migrate");
        System.out.println(1172487733 + "\tsort & migrate");
        System.out.println(1185623634 + "\tsort & hostWeight");
        System.out.println(1213528723 + "\tsort");
        System.out.println(1217926777 + "\tbaseline");
    }
}