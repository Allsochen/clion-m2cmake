package com.github.allsochen.m2cmake.action;

import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.dependence.SambaFileSynchronizeWorker;
import com.github.allsochen.m2cmake.generator.BazelToCmakeFileGenerator;
import com.github.allsochen.m2cmake.makefile.BazelWorkspace;
import com.github.allsochen.m2cmake.makefile.BazelWorkspaceAnalyser;
import com.github.allsochen.m2cmake.utils.ProjectUtil;
import com.github.allsochen.m2cmake.utils.ProjectWrapper;
import com.github.allsochen.m2cmake.view.ConsoleWindow;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BazelCmakeFileGenerateAction extends AnAction {

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        String basePath = project.getBasePath();

        BazelWorkspace bazelWorkspace = BazelWorkspaceAnalyser.analysis(basePath, project.getName());

        ProjectWrapper projectWrapper = new ProjectWrapper(
                ProjectUtil.chooseApp(null),
                ProjectUtil.chooseTarget(project.getName(), null, bazelWorkspace.getTarget()),
                project);

        JsonConfig jsonConfig = ProjectUtil.getJsonConfig(project);
        if (jsonConfig == null) {
            return;
        }

        ConsoleWindow consoleWindow = ConsoleWindow.getInstance(project);
        // Synchronized source dependence to destination.
        SambaFileSynchronizeWorker fsw = new SambaFileSynchronizeWorker(jsonConfig, null,
                bazelWorkspace, projectWrapper);
        BazelToCmakeFileGenerator generator = new BazelToCmakeFileGenerator(projectWrapper,
                bazelWorkspace, jsonConfig, consoleWindow, fsw);
        executorService.submit(() -> ProgressManager.getInstance().run(new Task.Backgroundable(project,
                "Transfer bazel to CMakeList...") {

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setFraction(0);
                generator.create();
                generator.open();
                generator.reload();
                progressIndicator.setFraction(1.0);
            }
        }));
    }
}
