package org.paperweight.ffm.service;

import org.junit.jupiter.api.Test;
import org.paperweight.ffm.exec.CommandResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpHelperServiceTest {

    private static CommandResult query(int exitCode, String stdout) {
        return new CommandResult(List.of("sc.exe", "query", "iphlpsvc"), true, exitCode, stdout, "", null, 10);
    }

    @Test
    void runningServiceIsDetected() {
        assertTrue(IpHelperService.isRunning(query(0, """
                SERVICE_NAME: iphlpsvc
                        TYPE               : 20  WIN32_SHARE_PROCESS
                        STATE              : 4  RUNNING
                """)));
        assertTrue(IpHelperService.isRunning(query(0, "状态     : 4  RUNNING")));
    }

    @Test
    void stoppedOrFailedQueryIsNotRunning() {
        assertFalse(IpHelperService.isRunning(query(0, "STATE : 1  STOPPED")));
        assertFalse(IpHelperService.isRunning(query(1060, "指定的服务未安装。")));
    }

    @Test
    void alreadyRunningStartIsRecognised() {
        assertTrue(IpHelperService.alreadyRunning(
                new CommandResult(List.of("sc.exe", "start", "iphlpsvc"), true, 1056, "", "", null, 10)));
        assertTrue(IpHelperService.alreadyRunning(
                new CommandResult(List.of("sc.exe", "start", "iphlpsvc"), true, 0, "服务实例已经在运行。", "", null, 10)));
        assertTrue(IpHelperService.alreadyRunning(
                new CommandResult(List.of("sc.exe", "start", "iphlpsvc"), true, 0, "The service has already been started.", "", null, 10)));
        assertFalse(IpHelperService.alreadyRunning(
                new CommandResult(List.of("sc.exe", "start", "iphlpsvc"), true, 5, "拒绝访问。", "", null, 10)));
    }
}
