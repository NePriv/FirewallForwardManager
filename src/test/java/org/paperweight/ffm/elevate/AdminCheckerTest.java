package org.paperweight.ffm.elevate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminCheckerTest {

    private static final String ELEVATED_GROUPS = """
            USER INFORMATION
            ----------------

            用户名            DESKTOP\\Dream
            SID               S-1-5-21-1111-2222-3333-1001

            组信息
            --------------
            ...
            强制标签\\高强制级别                                            标签            S-1-16-12288
            强制标签\\管理员                                             标签            S-1-16-12288
            """;

    private static final String NORMAL_GROUPS = """
            组信息
            --------------
            强制标签\\中强制级别                                            标签            S-1-16-8192
            """;

    @Test
    void detectsElevatedToken() {
        assertTrue(AdminChecker.containsHighIntegrity(ELEVATED_GROUPS));
        assertFalse(AdminChecker.containsHighIntegrity(NORMAL_GROUPS));
        assertFalse(AdminChecker.containsHighIntegrity(null));
        assertFalse(AdminChecker.containsHighIntegrity(""));
    }
}
