/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.gct.login.ui;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A dialog to get the verification code from the user.
 * This dialog will  provide the users with an authentication URL to navigate to
 * and a text box to paste the token they get when they log into Google via an external browser.
 */
// TODO: set a maximum size for the dialog
public class GoogleLoginCopyAndPasteDialog extends DialogWrapper {
  private static final String TITLE = "Sign in to Google Services";
  private static final String SUB_TITLE_1 =
    "Please sign in to Google Services from the link below.";
  private static final String SUB_TITLE_2 =
    "Copy and paste the verification code that will be provided into the text box below.";
  private static final String ERROR_MESSAGE = "Please log in using the Google Login url above,"
    + "and copy and paste the generated verification code.";
  private String verificationCode = "";
  private String urlString;
  private JTextField codeTextField;

  public GoogleLoginCopyAndPasteDialog(JComponent parent, GoogleAuthorizationCodeRequestUrl requestUrl, String message)
  {
    super(parent, true);
    urlString = requestUrl.build();

    if(message != null) {
      setTitle(message);
    } else {
      setTitle(TITLE);
    }

    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

    // Add the instructions
    mainPanel.add(Box.createVerticalStrut(5));
    mainPanel.add(new JLabel(SUB_TITLE_1));
    mainPanel.add(new JLabel(SUB_TITLE_2));

    // Add the url label and the url text
    JPanel urlPanel = new JPanel(new BorderLayout());
    urlPanel.setLayout(new BoxLayout(urlPanel, BoxLayout.LINE_AXIS));

    JLabel urlLabel = new JLabel(" Google Login Url: ");
    JTextField urlTextField = createUrlText();

    // Add the verification code label and text box
    JPanel codePanel = new JPanel();
    codePanel.setLayout(new BoxLayout(codePanel, BoxLayout.LINE_AXIS));
    JLabel codeLabel = new JLabel("Verification Code: ");

    createCodeText();

    urlLabel.setLabelFor(urlTextField);
    urlPanel.add(urlLabel);
    urlPanel.add(urlTextField);

    codeLabel.setLabelFor(codeTextField);
    codePanel.add(codeLabel);
    codePanel.add(codeTextField);

    // Add to main panel
    mainPanel.add(Box.createVerticalStrut(10));
    mainPanel.add(urlPanel);
    mainPanel.add(Box.createVerticalStrut(5));
    mainPanel.add(codePanel);
    mainPanel.add(Box.createVerticalStrut(5));

    return mainPanel;
  }

  @Override
  @NotNull
  protected Action getOKAction() {
    myOKAction = new OkAction(){
      @Override
      public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        verificationCode = codeTextField.getText();
      }
    };
    return myOKAction;
  }

  @Override
  @Nullable
  protected ValidationInfo doValidate() {
    if(codeTextField.getText().isEmpty()) {
      return new ValidationInfo(ERROR_MESSAGE, codeTextField);
    }
    return null;
  }


  @NotNull
  public String getVerificationCode() {
    return verificationCode;
  }


  private JTextField createUrlText() {
    final JTextField urlTextField = new JTextField(urlString);
    urlTextField.setBorder(null);
    urlTextField.setEditable(false);
    urlTextField.setBackground(UIUtil.getLabelBackground());

    // Add context menu to Url String
    JPopupMenu popup = new JPopupMenu();
    urlTextField.add(popup);
    urlTextField.setComponentPopupMenu(popup);

    JMenuItem copyMenu = new JMenuItem("Copy");
    copyMenu.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        urlTextField.copy();
      }
    });

    popup.add(copyMenu);
    return urlTextField;
  }


  private void createCodeText() {
    codeTextField = new JTextField();

    // Add context menu to Url String
    JPopupMenu popup = new JPopupMenu();
    codeTextField.add(popup);
    codeTextField.setComponentPopupMenu(popup);

    JMenuItem copyMenu = new JMenuItem("Paste");
    copyMenu.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        codeTextField.paste();
      }
    });

    popup.add(copyMenu);
  }

}
