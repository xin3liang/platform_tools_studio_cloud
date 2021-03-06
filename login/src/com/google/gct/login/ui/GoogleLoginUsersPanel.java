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

import com.google.gct.login.CredentialedUser;
import com.google.gct.login.GoogleLogin;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.LinkedHashMap;

/**
 * The Google Login Panel that displays the currently logged in users and buttons to
 * add a new user and sign out a logged in user.
 */
public class GoogleLoginUsersPanel extends JPanel implements ListSelectionListener {
  private static final String PLAY_CONSOLE_URL = "https://play.google.com/apps/publish/#ProfilePlace";
  private static final String CLOUD_CONSOLE_URL = "https://console.developers.google.com/accountsettings";
  private final static String LEARN_MORE_URL = "https://developers.google.com/cloud/devtools/android_studio_templates/";
  private JBList list;
  private DefaultListModel listModel;
  private static final int MAX_VISIBLE_ROW_COUNT = 3;
  private static final String addAccountString = "Add Account";
  private static final String signInString = "Sign In";
  private static final String signOutString = "Sign Out";
  private JButton signOutButton;
  private JButton addAccountButton;
  private boolean valueChanged = false;
  private boolean ignoreSelection = false;

  public GoogleLoginUsersPanel() {
    super(new BorderLayout());

    int indexToSelect = initializeUsers();
    final UsersListCellRenderer usersListCellRenderer = new UsersListCellRenderer();

    //Create the list that displays the users and put it in a scroll pane.
    list = new JBList(listModel) {
      @Override
      public Dimension getPreferredScrollableViewportSize() {
        int numUsers = listModel.size();
        Dimension superPreferredSize = super.getPreferredScrollableViewportSize();
        if(numUsers <= 1) {
          return superPreferredSize;
        }

        if(GoogleLogin.getInstance().getActiveUser() == null){
          return superPreferredSize;
        } else if(!isActiveUserInVisibleArea()) {
          return superPreferredSize;
        } else {
          // if there is an active user in the visible area
          int usersToShow = numUsers > MAX_VISIBLE_ROW_COUNT ? MAX_VISIBLE_ROW_COUNT : numUsers;
          int scrollHeight = ((usersToShow - 1) *  usersListCellRenderer.getMainPanelHeight())
            + usersListCellRenderer.getActivePanelHeight();
          return new Dimension((int)superPreferredSize.getWidth(), scrollHeight);
        }
      }
    };

    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setSelectedIndex(indexToSelect);
    list.addListSelectionListener(this);
    list.setVisibleRowCount(getVisibleRowCount());
    list.setCellRenderer(usersListCellRenderer);
    JBScrollPane listScrollPane = new JBScrollPane(list);

    list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent mouseEvent) {
        list.updateUI();

        if(listModel.getSize() == 1 && (listModel.get(0) instanceof NoUsersListItem)) {
          // When there are no users available
          if(usersListCellRenderer.inLearnMoreUrl(mouseEvent.getPoint())){
            BrowserUtil.browse(LEARN_MORE_URL);
          }
        } else {
          // When users are available
          if(!valueChanged) {
            // Clicking on an already active user
            int index = list.locationToIndex(mouseEvent.getPoint());
            if (index >= 0) {
              boolean inPlayUrl = usersListCellRenderer.inPlayConsoleUrl(mouseEvent.getPoint(), index);
              if(inPlayUrl){
                BrowserUtil.browse(PLAY_CONSOLE_URL);
              } else {
                boolean inCloudUrl = usersListCellRenderer.inCloudConsoleUrl(mouseEvent.getPoint(), index);
                if(inCloudUrl) {
                  BrowserUtil.browse(CLOUD_CONSOLE_URL);
                }
              }
            }
          }
        }
        valueChanged = false;
      }
    });

    list.addMouseMotionListener(new MouseMotionListener() {
      @Override
      public void mouseMoved(MouseEvent mouseEvent) {
        // Determine if the user under the cursor is an active user, a non-active user or a non-user
        int index = list.locationToIndex(mouseEvent.getPoint());
        if (index >= 0) {
          // If current object is the non-user list item, use default cursor
          Object currentObject = listModel.get(index);
          if(currentObject instanceof NoUsersListItem) {
            if(usersListCellRenderer.inLearnMoreUrl(mouseEvent.getPoint())) {
              list.setCursor(new Cursor(Cursor.HAND_CURSOR));
            } else {
              list.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
            return;
          }

          if (((UsersListItem)currentObject).isActiveUser()) {
            // Active user
            boolean inPlayUrl = usersListCellRenderer.inPlayConsoleUrl(mouseEvent.getPoint(), index);
            boolean inCloudUrl = usersListCellRenderer.inCloudConsoleUrl(mouseEvent.getPoint(), index);
            if (inPlayUrl || inCloudUrl) {
              list.setCursor(new Cursor(Cursor.HAND_CURSOR));
            } else {
              list.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
          } else {
            // For non-active user
            list.setCursor(new Cursor(Cursor.HAND_CURSOR));
          }
        }
      }

      @Override
      public void mouseDragged(MouseEvent e) {
      }
    });

    boolean noUsersAvailable = (listModel.getSize() == 1) && (listModel.get(0) instanceof NoUsersListItem);
    addAccountButton = new JButton(noUsersAvailable ? signInString : addAccountString);
    AddAccountListener addAccountListener = new AddAccountListener();
    addAccountButton.addActionListener(addAccountListener);
    addAccountButton.setHorizontalAlignment(SwingConstants.LEFT);

    signOutButton = new JButton(signOutString);
    signOutButton.addActionListener(new SignOutListener());

    if(list.isSelectionEmpty()) {
      signOutButton.setEnabled(false);
    } else {
      // If list contains the NoUsersListItem place holder
      // sign out button should be hidden
      if(noUsersAvailable) {
        signOutButton.setVisible(false);
      } else {
        signOutButton.setEnabled(true);
      }
    }

    //Create a panel to hold the buttons
    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
    buttonPane.add(addAccountButton);
    buttonPane.add(Box.createHorizontalGlue());
    buttonPane.add(signOutButton);
    buttonPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    add(listScrollPane, BorderLayout.CENTER);
    add(buttonPane, BorderLayout.PAGE_END);
  }

  /**
   * The action listener for {@code signOutButton}
   */
  class SignOutListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      GoogleLogin.getInstance().logOut();
    }
  }

  /**
   * The action listener for {@code addAccountButton}
   */
  class AddAccountListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      GoogleLogin.getInstance().logIn();
    }
  }

  //This method is required by ListSelectionListener.
  @Override
  public void valueChanged(ListSelectionEvent e) {
    if(ignoreSelection) {
      return;
    }
    valueChanged = true;
    if (e.getValueIsAdjusting() == false) {
      if (list.getSelectedIndex() == -1) {
        signOutButton.setEnabled(false);
      } else {
        signOutButton.setEnabled(true);

        // Make newly selected value the active value
        UsersListItem selectedUser = (UsersListItem)listModel.get(list.getSelectedIndex());
        if(!selectedUser.isActiveUser()) {
          GoogleLogin.getInstance().setActiveUser(selectedUser.getUserEmail());
        }

        // Change order of elements in the list so that the
        // active user becomes the first element in the list
        ignoreSelection = true;
        try {
          listModel.remove(list.getSelectedIndex());
          listModel.add(0, selectedUser);

          // Re-select the active user
          list.setSelectedIndex(0);
        } finally {
          ignoreSelection = false;
        }
      }
    }
  }

  public JBList getList() {
    return list;
  }

  private int initializeUsers() {
    LinkedHashMap<String, CredentialedUser> allUsers = GoogleLogin.getInstance().getAllUsers();
    listModel = new DefaultListModel();

    int activeUserIndex = allUsers.size();
    for(CredentialedUser aUser : allUsers.values()) {
      listModel.addElement(new UsersListItem(aUser));
      if(aUser.isActive()) {
        activeUserIndex = listModel .getSize() - 1;
      }
    }

    if(listModel.getSize() == 0) {
      // Add no user panel
      listModel.addElement(NoUsersListItem.INSTANCE);
    } else if ((activeUserIndex != 0) && (activeUserIndex < listModel.getSize())) {
      // Change order of elements in the list so that the
      // active user becomes the first element in the list
      UsersListItem activeUser = (UsersListItem)listModel.remove(activeUserIndex);
      listModel.add(0, activeUser);
      activeUserIndex = 0;
    }

    return activeUserIndex;
  }

  private int getVisibleRowCount(){
    if (listModel == null) {
      return 0;
    }

    int size = listModel.getSize();
    if(size >= MAX_VISIBLE_ROW_COUNT) {
      return MAX_VISIBLE_ROW_COUNT;
    } else if (size == 0) {
      return 3;
    } else {
      return  size;
    }
  }

  private boolean isActiveUserInVisibleArea() {
    int max = listModel.getSize() < MAX_VISIBLE_ROW_COUNT ?
      listModel.getSize() : MAX_VISIBLE_ROW_COUNT;

    for(int i = 0; i < max; i++){
      if(((UsersListItem)listModel.get(i)).isActiveUser()) {
        return true;
      }
    }
    return false;
  }
}
