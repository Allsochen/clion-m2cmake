package com.github.allsochen.m2cmake;

import com.github.allsochen.m2cmake.configuration.Configuration;
import com.github.allsochen.m2cmake.configuration.JsonConfig;
import com.github.allsochen.m2cmake.configuration.JsonConfigBuilder;
import com.github.allsochen.m2cmake.configuration.Properties;
import com.github.allsochen.m2cmake.file.CmakeFileGenerator;
import com.github.allsochen.m2cmake.file.TafMakefileAnalysis;
import com.github.allsochen.m2cmake.file.TafMakefileProperty;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;

public class CmakeFileGenerateAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        String basePath = project.getBasePath();
        TafMakefileAnalysis analysis = new TafMakefileAnalysis();
        TafMakefileProperty tafMakefileProperty = analysis.analysis(basePath);

        String json = Properties.get(Configuration.JSON_STR);
        if (json == null || json.isEmpty()) {
            json = JsonConfigBuilder.getInstance().create();
        }
        JsonConfig jsonConfig;
        try {
            jsonConfig = JsonConfigBuilder.getInstance().deserialize(json);
        } catch (Exception e) {
            Messages.showInfoMessage(project,
                    "please check json configuration, Settings->TAF m2cmake configuration",
                    "m2cmake configuration error");
            return;
        }

        CmakeFileGenerator cmakeFileGenerator = new CmakeFileGenerator(basePath, tafMakefileProperty, jsonConfig);
        try {
            cmakeFileGenerator.create();
        } catch (Exception e) {
            e.printStackTrace();
        }
        VirtualFile[] virtualFiles = FileEditorManager.getInstance(project).getOpenFiles();
        for (VirtualFile virtualFile : virtualFiles) {
            if (virtualFile.getName().contains("CMakeLists")) {
                virtualFile.refresh(false, false);
            }
        }
        File cmakeFile = CmakeFileGenerator.getCmakeListFile(basePath);

        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(cmakeFile);
        if (vf != null) {
            OpenFileDescriptor descriptor = new OpenFileDescriptor(project, vf);
            FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
        }
    }
}
