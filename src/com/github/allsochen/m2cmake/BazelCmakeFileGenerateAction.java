package com.github.allsochen.m2cmake;

import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.dependence.FileSynchronizeWorker;
import com.github.allsochen.m2cmake.makefile.BazelCmakeFileGenerator;
import com.github.allsochen.m2cmake.makefile.BazelWorkspace;
import com.github.allsochen.m2cmake.makefile.BazelWorkspaceAnalyser;
import com.github.allsochen.m2cmake.utils.ProjectUtil;
import com.github.allsochen.m2cmake.view.ConsoleWindow;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

public class BazelCmakeFileGenerateAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        String basePath = project.getBasePath();

        BazelWorkspace bazelWorkspace = BazelWorkspaceAnalyser.analysis(basePath, project.getName());

        String app = ProjectUtil.chooseApp(null);
        String target = ProjectUtil.chooseTarget(project.getName(), null, bazelWorkspace.getTarget());

        JsonConfig jsonConfig = ProjectUtil.getJsonConfig(project);
        if (jsonConfig == null) {
            return;
        }

        ConsoleWindow consoleWindow = ConsoleWindow.getInstance(project);
        // Synchronized source dependence to destination.
        FileSynchronizeWorker fsw = new FileSynchronizeWorker(jsonConfig, null, bazelWorkspace,
                app, target, project);
        BazelCmakeFileGenerator generator = new BazelCmakeFileGenerator(app, target,
                basePath, bazelWorkspace, jsonConfig, consoleWindow, fsw);
        generator.create();
        generator.open(project);
        generator.reload();
    }
}
