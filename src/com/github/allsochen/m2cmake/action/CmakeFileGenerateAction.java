package com.github.allsochen.m2cmake.action;

import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.generator.MakefileToCmakeFileGenerator;
import com.github.allsochen.m2cmake.makefile.BazelWorkspace;
import com.github.allsochen.m2cmake.makefile.BazelWorkspaceAnalyser;
import com.github.allsochen.m2cmake.makefile.TafMakefileAnalyser;
import com.github.allsochen.m2cmake.makefile.TafMakefileProperty;
import com.github.allsochen.m2cmake.utils.ProjectUtil;
import com.github.allsochen.m2cmake.utils.ProjectWrapper;
import com.github.allsochen.m2cmake.view.ConsoleWindow;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

public class CmakeFileGenerateAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        String basePath = project.getBasePath();
        TafMakefileAnalyser analysis = new TafMakefileAnalyser();
        TafMakefileProperty tafMakefileProperty = analysis.analysis(basePath);

        BazelWorkspace bazelWorkspace = BazelWorkspaceAnalyser.analysis(basePath, project.getName());

        String app = ProjectUtil.chooseApp(tafMakefileProperty.getApp());
        String target = ProjectUtil.chooseTarget(project.getName(), tafMakefileProperty.getTargets(),
                bazelWorkspace.getTarget());
        ProjectWrapper projectWrapper = new ProjectWrapper(app, target, project);

        JsonConfig jsonConfig = ProjectUtil.getJsonConfig(project);
        if (jsonConfig == null) {
            return;
        }

        ConsoleWindow consoleWindow = ConsoleWindow.getInstance(project);
        MakefileToCmakeFileGenerator cmakeFileGenerator = new MakefileToCmakeFileGenerator(projectWrapper,
                tafMakefileProperty, jsonConfig, consoleWindow);
        cmakeFileGenerator.create();
        cmakeFileGenerator.open();
        cmakeFileGenerator.reload();
    }
}
