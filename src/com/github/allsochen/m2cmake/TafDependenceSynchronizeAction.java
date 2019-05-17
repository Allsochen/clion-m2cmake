package com.github.allsochen.m2cmake;

import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.dependence.FileSynchronizeWorker;
import com.github.allsochen.m2cmake.makefile.CmakeFileGenerator;
import com.github.allsochen.m2cmake.makefile.TafMakefileAnalysis;
import com.github.allsochen.m2cmake.makefile.TafMakefileProperty;
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
        TafMakefileAnalysis analysis = new TafMakefileAnalysis();
        TafMakefileProperty tafMakefileProperty = analysis.analysis(basePath);

        String app = BaseAction.chooseApp(tafMakefileProperty.getApp());
        String target = BaseAction.chooseTarget(project.getName(), tafMakefileProperty.getTargets());

        JsonConfig jsonConfig = BaseAction.getJsonConfig(project);
        if (jsonConfig == null) {
            return;
        }

        // Synchronized source dependence to destination.
        FileSynchronizeWorker fsw = new FileSynchronizeWorker(jsonConfig, tafMakefileProperty, app, target);

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
