package com.github.allsochen.m2cmake.configuration;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.panels.VerticalLayout;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Configuration implements Configurable {

    private boolean modified = false;

    private ConfigurationModifiedListener listener = new ConfigurationModifiedListener(this);

    public static final String JSON_STR = "json_str";

    private JTextArea jsonArea;

    @Nls
    @Override
    public String getDisplayName() {
        return "m2cmake";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel jPanel = new JPanel();

        VerticalLayout verticalLayout = new VerticalLayout(1, 2);
        jPanel.setLayout(verticalLayout);

        String json = Properties.get(JSON_STR);

        // The first time a user installs the plugin, save the default options in their properties.
        if (json == null || json.isEmpty()) {
            json = JsonConfigBuilder.getInstance().create();
            Properties.set(JSON_STR, json);
        }

        jsonArea = new JTextArea(json, 10, 80);
        jsonArea.setLineWrap(true);
        jsonArea.setWrapStyleWord(true);

        jsonArea.getDocument().addDocumentListener(listener);
        jPanel.add(jsonArea);
        return jPanel;
    }

    @Override
    public boolean isModified() {
        return this.modified;
    }

    private void setModified() {
        this.modified = true;
    }

    @Override
    public void apply() throws ConfigurationException {
        Properties.set(JSON_STR, jsonArea.getText());
        modified = false;
    }

    @Override
    public void reset() {
        String json = JsonConfigBuilder.getInstance().create();
        jsonArea.setText(json);
        modified = false;
    }

    @Override
    public void disposeUIResources() {
        jsonArea.getDocument().removeDocumentListener(listener);
    }

    private static class ConfigurationModifiedListener implements DocumentListener {
        private final Configuration option;

        ConfigurationModifiedListener(Configuration option) {
            this.option = option;
        }

        @Override
        public void insertUpdate(DocumentEvent documentEvent) {
            option.setModified();
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent) {
            option.setModified();
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent) {
            option.setModified();
        }
    }
}
