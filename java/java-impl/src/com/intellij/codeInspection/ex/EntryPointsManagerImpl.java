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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 27, 2002
 * Time: 2:57:13 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.codeInspection.ex;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInspection.util.SpecialAnnotationsUtil;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@State(name = "EntryPointsManager")
public class EntryPointsManagerImpl extends EntryPointsManagerBase implements PersistentStateComponent<Element> {
  public EntryPointsManagerImpl(Project project) {
    super(project);
  }

  @Override
  public void configureAnnotations() {
    final List<String> list = new ArrayList<>(ADDITIONAL_ANNOTATIONS);
    final JPanel listPanel = SpecialAnnotationsUtil.createSpecialAnnotationsListControl(list, "Mark as entry point if annotated by", true);
    new DialogWrapper(myProject) {
      {
        init();
        setTitle("Configure Annotations");
      }

      @Override
      protected JComponent createCenterPanel() {
        return listPanel;
      }

      @Override
      protected void doOKAction() {
        ADDITIONAL_ANNOTATIONS.clear();
        ADDITIONAL_ANNOTATIONS.addAll(list);
        DaemonCodeAnalyzer.getInstance(myProject).restart();
        super.doOKAction();
      }
    }.show();
  }

  @Override
  public JButton createConfigureAnnotationsBtn() {
    return createConfigureAnnotationsButton();
  }

  public static JButton createConfigureAnnotationsButton() {
    final JButton configureAnnotations = new JButton("Configure annotations...");
    configureAnnotations.setHorizontalAlignment(SwingConstants.LEFT);
    configureAnnotations.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        getInstance(ProjectUtil.guessCurrentProject(configureAnnotations)).configureAnnotations();
      }
    });
    return configureAnnotations;
  }

  public static JButton createConfigureClassPatternsButton() {
    final JButton configureClassPatterns = new JButton("Configure code patterns...");
    configureClassPatterns.setHorizontalAlignment(SwingConstants.LEFT);
    configureClassPatterns.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final Project project = ProjectUtil.guessCurrentProject(configureClassPatterns);
        final EntryPointsManagerBase entryPointsManagerBase = getInstance(project);
        final ArrayList<ClassPattern> list = new ArrayList<>();
        for (ClassPattern pattern : entryPointsManagerBase.getPatterns()) {
          list.add(new ClassPattern(pattern));
        }
        final ClassPatternsPanel panel = new ClassPatternsPanel(list);
        new DialogWrapper(entryPointsManagerBase.myProject) {

          {
            init();
            setTitle("Configure Code Patterns");
          }

          @Nullable
          @Override
          protected JComponent createCenterPanel() {
            return panel;
          }

          @Override
          protected void doOKAction() {
            final String error = panel.getValidationError(project);
            if (error != null) {
              Messages.showErrorDialog(panel, error);
              return;
            }
            final LinkedHashSet<ClassPattern> patterns = entryPointsManagerBase.getPatterns();
            patterns.clear();
            patterns.addAll(list);
            DaemonCodeAnalyzer.getInstance(entryPointsManagerBase.myProject).restart();
            super.doOKAction();
          }
        }.show();
      }
    });
    return configureClassPatterns;
  }
}