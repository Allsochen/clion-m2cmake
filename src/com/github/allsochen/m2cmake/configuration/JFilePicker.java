package com.github.allsochen.m2cmake.configuration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class JFilePicker extends JPanel {
    private JTextField textField;
    private JFileChooser fileChooser;

    JFilePicker(String textFieldLabel, String buttonLabel) {
        fileChooser = new JFileChooser();

        setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        JLabel label = new JLabel(textFieldLabel);

        textField = new JTextField(30);
        JButton button = new JButton(buttonLabel);

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                buttonActionPerformed(evt);
            }
        });

        add(label);
        add(textField);
        add(button);
    }

    JTextField getTextField() {
        return textField;
    }

    private void buttonActionPerformed(ActionEvent evt) {
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }


}
