package com.github.allsochen.m2cmake;

import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.dependence.FileSynchronizeWorker;
import com.github.allsochen.m2cmake.makefile.BazelWorkspace;
import com.github.allsochen.m2cmake.makefile.BazelWorkspaceAnalyser;
import com.github.allsochen.m2cmake.utils.ProjectUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class BazelDependenceSynchronizeAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        String basePath = project.getBasePath();

        BazelWorkspace bazelWorkspace = BazelWorkspaceAnalyser.analysis(basePath, project.getName());

        String app = ProjectUtil.chooseApp(null);
        String target = ProjectUtil.chooseTarget(project.getName(), null,
                bazelWorkspace.getTarget());

        JsonConfig jsonConfig = ProjectUtil.getJsonConfig(project);
        if (jsonConfig == null) {
            return;
        }

        // Synchronized source dependence to destination.
        FileSynchronizeWorker fsw = new FileSynchronizeWorker(jsonConfig, null, bazelWorkspace,
                app, target, project);

        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                "TAF Bazel dependence synchronize...") {

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setFraction(0);
                fsw.perform(progressIndicator);
                progressIndicator.setFraction(1.0);
            }
        });
    }
}
