/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.diagnostic.errordialog;

import com.intellij.diagnostic.DiagnosticBundle;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerConfigurable;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.util.ui.JBEmptyBorder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PluginConflictDialog extends DialogWrapper {
  public static final int WIDTH = 510;

  @NotNull
  private final List<PluginId> myConflictingPlugins;

  private final boolean myIsConflictWithPlatform;
  @Nullable
  private final List<JBRadioButton> myRadioButtons;

  private JPanel myContentPane;

  private JBLabel myTopMessageLabel;

  private JPanel myConflictingPluginsListPanel;

  private JBLabel myBottomMessageLabel;

  public PluginConflictDialog(@NotNull Component parent,
                              @NotNull List<PluginId> conflictingPlugins,
                              boolean isConflictWithPlatform) {
    super(parent, false);

    myConflictingPlugins = conflictingPlugins;
    myIsConflictWithPlatform = isConflictWithPlatform;

    if (myIsConflictWithPlatform) {
      myRadioButtons = null;
    }
    else {
      myRadioButtons = new ArrayList<>();
    }

    $$$setupUI$$$();
    setTitle(DiagnosticBundle.message("error.dialog.conflict.plugin.title"));
    init();

    myTopMessageLabel.setText(getTopMessageText(conflictingPlugins, isConflictWithPlatform));
    myBottomMessageLabel.setText(DiagnosticBundle.message("error.dialog.conflict.plugin.footer"));

    myTopMessageLabel.setPreferredSize(new Dimension(WIDTH, (int)myTopMessageLabel.getPreferredSize().getHeight()));
    myContentPane.setPreferredSize(new Dimension(WIDTH, (int)myContentPane.getMinimumSize().getHeight()));
  }

  private static String getTopMessageText(@NotNull List<PluginId> conflictingPlugins, boolean isConflictWithPlatform) {
    final int pluginsNumber = conflictingPlugins.size();
    if (isConflictWithPlatform) {
      return DiagnosticBundle.message("error.dialog.conflict.plugin.header.platform", pluginsNumber);
    }
    else {
      final List<String> names = conflictingPlugins.stream()
        .map(PluginConflictDialog::getPluginNameOrId)
        .map(s -> "<b>" + s + "</b>")
        .collect(Collectors.toList());
      return DiagnosticBundle.message("error.dialog.conflict.plugin.header.each.other",
                                      StringUtil.join(names.subList(0, pluginsNumber - 1), ", "),
                                      names.get(pluginsNumber - 1));
    }
  }

  private void $$$setupUI$$$() {
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myContentPane;
  }

  private void createUIComponents() {
    final ButtonGroup buttonGroup = new ButtonGroup();

    myConflictingPluginsListPanel = new JPanel(new GridLayout(0, 1));
    final List<JPanel> pluginDescriptions = myConflictingPlugins.stream()
      .map(plugin -> getChooserPanelForPlugin(buttonGroup, plugin))
      .collect(Collectors.toList());

    if (!myIsConflictWithPlatform) {
      pluginDescriptions.add(0, getChooserPanelForPlugin(buttonGroup, null));
    }

    for (JPanel panel : pluginDescriptions) {
      myConflictingPluginsListPanel.add(panel);
    }

    setUpDefaultSelection();
  }

  @NotNull
  private JPanel getChooserPanelForPlugin(@NotNull ButtonGroup buttonGroup, @Nullable PluginId plugin) {
    final JPanel panel = new JPanel(new BorderLayout());
    if (!myIsConflictWithPlatform) {
      assert myRadioButtons != null;

      final JBRadioButton radioButton = new JBRadioButton();
      myRadioButtons.add(radioButton);
      buttonGroup.add(radioButton);

      panel.add(radioButton, BorderLayout.WEST);
      panel.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          radioButton.setSelected(true);
        }
      });
    }

    final JPanel descriptionPanel;
    if (plugin != null) {
      descriptionPanel = getPluginDescriptionPanel(plugin, !myIsConflictWithPlatform);
    }
    else {
      descriptionPanel = getDisableAllPanel();
    }
    descriptionPanel.setBorder(new JBEmptyBorder(10, myIsConflictWithPlatform ? 10 : 0, 10, 20));

    panel.add(descriptionPanel, BorderLayout.CENTER);
    return panel;
  }

  private void setUpDefaultSelection() {
    if (myIsConflictWithPlatform) {
      return;
    }
    assert myRadioButtons != null && myRadioButtons.size() == myConflictingPlugins.size() + 1;

    for (int i = 0; i < myConflictingPlugins.size(); i++) {
      final IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(myConflictingPlugins.get(i));
      if (pluginDescriptor != null
          && (pluginDescriptor.isBundled() || StringUtil.equalsIgnoreCase(pluginDescriptor.getVendor(), "JetBrains"))) {
        myRadioButtons.get(i).setSelected(true);
        return;
      }
    }
    myRadioButtons.get(myRadioButtons.size() - 1).setSelected(true);
  }

  @NotNull
  private static JPanel getPluginDescriptionPanel(@NotNull PluginId plugin, boolean addUseWord) {
    final JPanel panel = new JPanel(new BorderLayout());

    final IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(plugin);
    if (pluginDescriptor == null) {
      panel.add(new JBLabel(plugin.getIdString()), BorderLayout.CENTER);
      return panel;
    }

    final StringBuilder sb = new StringBuilder("<html>");
    if (addUseWord) {
      sb.append("Use ");
    }
    sb.append(pluginDescriptor.getName());
    if (pluginDescriptor.getVendor() != null) {
      sb.append(" by ").append(pluginDescriptor.getVendor());
    }
    sb.append("</html>");
    panel.add(new JBLabel(sb.toString()));
    return panel;
  }

  @NotNull
  private static String getPluginNameOrId(@NotNull PluginId pluginId) {
    final IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(pluginId);
    if (pluginDescriptor == null) {
      return pluginId.getIdString();
    }
    else {
      return pluginDescriptor.getName();
    }
  }

  @NotNull
  private static JPanel getDisableAllPanel() {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(new JBLabel(DiagnosticBundle.message("error.dialog.conflict.plugin.disable.all")));
    return panel;
  }

  @NotNull
  @Override
  protected Action getOKAction() {
    return new DisableAction();
  }

  private class DisableAction extends DialogWrapperAction {
    protected DisableAction() {
      super(DiagnosticBundle.message("error.dialog.disable.plugin.action.disable"));
      putValue(DEFAULT_ACTION, Boolean.TRUE);
    }

    @Override
    protected void doAction(ActionEvent e) {
      for (int i = 0; i < myConflictingPlugins.size(); ++i) {
        if (myRadioButtons == null || !myRadioButtons.get(i).isSelected()) {
          PluginManagerCore.disablePlugin(myConflictingPlugins.get(i).getIdString());
        }
      }
      close(OK_EXIT_CODE);
      PluginManagerConfigurable.shutdownOrRestartApp("");
    }
  }
}
