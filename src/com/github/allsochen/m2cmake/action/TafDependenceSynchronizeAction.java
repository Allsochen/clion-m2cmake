package com.github.allsochen.m2cmake.action;

import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.dependence.FileSynchronizeWorker;
import com.github.allsochen.m2cmake.makefile.TafMakefileAnalyser;
import com.github.allsochen.m2cmake.makefile.TafMakefileProperty;
import com.github.allsochen.m2cmake.utils.ProjectUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class TafDependenceSynchronizeAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        String basePath = project.getBasePath();

        TafMakefileAnalyser analysis = new TafMakefileAnalyser();
        TafMakefileProperty tafMakefileProperty = analysis.analysis(basePath);

        String app = ProjectUtil.chooseApp(tafMakefileProperty.getApp());
        String target = ProjectUtil.chooseTarget(project.getName(), tafMakefileProperty.getTargets(), null);

        JsonConfig jsonConfig = ProjectUtil.getJsonConfig(project);
        if (jsonConfig == null) {
            return;
        }

        // Synchronized source dependence to destination.
        FileSynchronizeWorker fsw = new FileSynchronizeWorker(jsonConfig, tafMakefileProperty, null,
                app, target, project);

        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                "TAF dependence recurse synchronize...") {

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setFraction(0);
                fsw.perform(progressIndicator);
                progressIndicator.setFraction(1.0);
            }
        });
    }
}
