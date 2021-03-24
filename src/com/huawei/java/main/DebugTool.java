package com.huawei.java.main;

public class DebugTool {
    public void debugPrint(String[] outputs) {
        StringBuilder output = new StringBuilder();
        for (String s: outputs) {
            output.append(s).append(" ");
        }
        System.out.println(output);
    }
}
