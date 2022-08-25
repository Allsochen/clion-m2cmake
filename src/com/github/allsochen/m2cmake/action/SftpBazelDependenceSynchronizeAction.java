package com.github.allsochen.m2cmake.action;

import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.dependence.SambaFileSynchronizeWorker;
import com.github.allsochen.m2cmake.dependence.SftpFileSynchronizeWorker;
import com.github.allsochen.m2cmake.makefile.BazelWorkspace;
import com.github.allsochen.m2cmake.makefile.BazelWorkspaceAnalyser;
import com.github.allsochen.m2cmake.utils.ProjectUtil;
import com.github.allsochen.m2cmake.utils.ProjectWrapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class SftpBazelDependenceSynchronizeAction extends AnAction {

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

        // Synchronized source dependence to destination.
        SftpFileSynchronizeWorker fsw = new SftpFileSynchronizeWorker(jsonConfig, bazelWorkspace, projectWrapper);

        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                "Bazel dependence synchronize...") {

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setFraction(0);
                fsw.perform(progressIndicator);
                progressIndicator.setFraction(1.0);
            }
        });
    }
}
