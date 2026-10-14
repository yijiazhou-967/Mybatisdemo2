package com.console;

import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.Test;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class RelationshipConsoleTest {
    @Test
    void invalidChoicesAndExitDoNotOpenDatabaseSessions() throws Exception {
        AtomicInteger opened = new AtomicInteger();
        RelationshipConsole terminal = new RelationshipConsole(() -> { opened.incrementAndGet(); return null; });
        StringWriter output = new StringWriter();
        terminal.run(new StringReader("unexpected\n\uFEFF0\n"), new PrintWriter(output));
        assertEquals(0, opened.get());
        assertTrue(output.toString().contains("请输入 0 到 6"));
        assertEquals(1, output.toString().split("请输入 0 到 6", -1).length - 1);
        assertTrue(output.toString().contains("查询终端已退出"));
    }

    @Test
    void invalidIdsAreRetriedAndEofExitsWithoutConnecting() throws Exception {
        RelationshipConsole terminal = new RelationshipConsole(() -> { fail("无效编号不应连接数据库"); return null; });
        StringWriter output = new StringWriter();
        terminal.run(new StringReader("1\n' OR 1=1\n-1\n2147483648\n"), new PrintWriter(output));
        assertEquals(3, output.toString().split("编号必须为正整数", -1).length - 1);
        assertTrue(output.toString().contains("输入结束"));
    }

    @Test
    void queryFailureReturnsToTheMenuAndHidesConnectionDetails() throws Exception {
        RelationshipConsole terminal = new RelationshipConsole(() -> {
            throw new PersistenceException("internal connection details");
        });
        StringWriter output = new StringWriter();
        terminal.run(new StringReader("2\n1\n0\n"), new PrintWriter(output));
        assertTrue(output.toString().contains("查询失败"));
        assertTrue(output.toString().contains("查询终端已退出"));
        assertFalse(output.toString().contains("internal connection details"));
    }
}
