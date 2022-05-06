package com.github.allsochen.m2cmake.view;

import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ConsoleWindow {

    private final ConsoleView consoleView;

    private static final Map<String, ConsoleWindow> CONSOLE_WINDOWS = new HashMap<>();

    private ConsoleWindow(Project project) {
        ToolWindowManager manager = ToolWindowManager.getInstance(project);
        String title = "TAF/tRPC Synchronize Console";
        TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
        consoleView = builder.getConsole();
        ToolWindow window = manager.getToolWindow(title);
        if (window == null) {
            window = manager.registerToolWindow(title, consoleView.getComponent(), ToolWindowAnchor.BOTTOM);
        }
    }

    public static synchronized ConsoleWindow getInstance(Project project) {
        if (!CONSOLE_WINDOWS.containsKey(project.getName())) {
            ConsoleWindow consoleWindow = new ConsoleWindow(project);
            CONSOLE_WINDOWS.put(project.getName(), consoleWindow);
            return consoleWindow;
        }
        return CONSOLE_WINDOWS.get(project.getName());
    }

    public void println(String message, ConsoleViewContentType consoleViewContentType) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String datetime = sdf.format(new Date());
        consoleView.print("[" + datetime + "]" + "\t" + message + "\n", consoleViewContentType);
    }
}
