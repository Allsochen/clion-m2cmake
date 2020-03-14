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

public class ConsoleWindow {

    private ToolWindow window;

    private ConsoleView consoleView;

    private static ConsoleWindow consoleWindow;

    private ConsoleWindow(Project project) {
        if (project != null) {
            ToolWindowManager manager = ToolWindowManager.getInstance(project);
            String title = "TAF Synchronize Console";
            TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
            consoleView = builder.getConsole();
            window = manager.getToolWindow(title);
            if (window == null) {
                window = manager.registerToolWindow(title, consoleView.getComponent(), ToolWindowAnchor.BOTTOM);
                window.setTitle(title);
            }
        }
    }

    public static synchronized ConsoleWindow getInstance(Project project) {
        if (consoleWindow == null) {
            consoleWindow = new ConsoleWindow(project);
        }
        return consoleWindow;
    }

    public void println(String message, ConsoleViewContentType consoleViewContentType) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String datetime = sdf.format(new Date());
        consoleView.print("[" + datetime + "]" + "\t" + message + "\n", consoleViewContentType);
    }
}
