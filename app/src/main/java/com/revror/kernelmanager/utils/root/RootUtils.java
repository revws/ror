/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
/* PUBLIC OPEN-SOURCE DESIGN DISTRIBUTION - CORE ENGINE DECOUPLED */
package com.revror.kernelmanager.utils.root;

import java.util.Collections;
import java.util.List;

public class RootUtils {
    public static boolean rootAccess() { return false; }
    public static boolean busyboxInstalled() { return false; }
    public static void chmod(String file, String permission) {}
    public static String getProp(String prop) { return ""; }
    public static String mount(String command, String mountPoint) { return ""; }
    public static boolean isWritableSystem() { return false; }
    public static boolean isWritableRoot() { return false; }
    public static void closeSU() {}
    public static void runCommand(String command) {}
    public static void runAndGetLiveOutput(String command, List<String> output) {}
    public static String runAndGetOutput(String command) { return ""; }
    public static String runAndGetError(String command) { return ""; }
}