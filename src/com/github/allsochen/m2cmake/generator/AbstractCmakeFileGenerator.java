package com.github.allsochen.m2cmake.generator;

import com.github.allsochen.m2cmake.build.AutomaticReloadCMakeBuilder;
import com.github.allsochen.m2cmake.utils.ProjectWrapper;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;

public abstract class AbstractCmakeFileGenerator {

    protected ProjectWrapper projectWrapper;

    AbstractCmakeFileGenerator(ProjectWrapper projectWrapper) {
        this.projectWrapper = projectWrapper;
    }

    /**
     * 创建 CmakeList.txt 文件
     */
    public abstract void create();

    public void open() {
        Project project = projectWrapper.getProject();
        try {
            VirtualFile[] virtualFiles = FileEditorManager.getInstance(project).getOpenFiles();
            for (VirtualFile virtualFile : virtualFiles) {
                if (virtualFile.getName().contains("CMakeLists")) {
                    virtualFile.refresh(false, false);
                }
            }
            File cmakeFile = BazelToCmakeFileGenerator.getCmakeListFile(project.getBasePath());

            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(cmakeFile);
            if (vf != null) {
                OpenFileDescriptor descriptor = new OpenFileDescriptor(project, vf);
                FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        try {
            // Set project to auto build.
            try {
                LocalFileSystem.getInstance().refresh(true);
                VfsUtil.markDirtyAndRefresh(true, true, true,
                        ProjectRootManager.getInstance(projectWrapper.getProject()).getContentRoots());
                AutomaticReloadCMakeBuilder.build(projectWrapper.getProject().getBasePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
