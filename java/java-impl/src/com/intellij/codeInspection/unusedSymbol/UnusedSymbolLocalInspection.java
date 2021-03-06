/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.codeInspection.unusedSymbol;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupAdapter;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiModifier;
import com.intellij.ui.ClickListener;
import com.intellij.ui.UI;
import com.intellij.ui.UserActivityProviderComponent;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.intellij.util.Producer;
import com.intellij.util.VisibilityUtil;
import com.intellij.util.containers.HashSet;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Hashtable;
import java.util.Set;

/**
 * User: anna
 * Date: 17-Feb-2006
 */
public class UnusedSymbolLocalInspection extends UnusedSymbolLocalInspectionBase {

  /**
   * use {@link com.intellij.codeInspection.deadCode.UnusedDeclarationInspection} instead
   */
  @Deprecated
  public UnusedSymbolLocalInspection() {
  }

  public class OptionsPanel {
    private JCheckBox myCheckLocalVariablesCheckBox;
    private JCheckBox myCheckClassesCheckBox;
    private JCheckBox myCheckFieldsCheckBox;
    private JCheckBox myCheckMethodsCheckBox;
    private JCheckBox myCheckParametersCheckBox;
    private JCheckBox myAccessors;
    private JPanel myPanel;
    private JLabel myClassVisibilityCb;
    private JLabel myFieldVisibilityCb;
    private JLabel myMethodVisibilityCb;
    private JLabel myMethodParameterVisibilityCb;

    public OptionsPanel() {
      myCheckLocalVariablesCheckBox.setSelected(LOCAL_VARIABLE);
      myCheckClassesCheckBox.setSelected(CLASS);
      myCheckFieldsCheckBox.setSelected(FIELD);
      myCheckMethodsCheckBox.setSelected(METHOD);

      myCheckParametersCheckBox.setSelected(PARAMETER);
      myAccessors.setSelected(isIgnoreAccessors());
      myAccessors.setEnabled(PARAMETER);

      final ActionListener listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          LOCAL_VARIABLE = myCheckLocalVariablesCheckBox.isSelected();
          CLASS = myCheckClassesCheckBox.isSelected();
          FIELD = myCheckFieldsCheckBox.isSelected();
          METHOD = myCheckMethodsCheckBox.isSelected();
          PARAMETER = myCheckParametersCheckBox.isSelected();

          myAccessors.setEnabled(METHOD);
          setIgnoreAccessors(!myAccessors.isSelected());
        }
      };
      myCheckLocalVariablesCheckBox.addActionListener(listener);
      myCheckFieldsCheckBox.addActionListener(listener);
      myCheckMethodsCheckBox.addActionListener(listener);
      myCheckClassesCheckBox.addActionListener(listener);
      myCheckParametersCheckBox.addActionListener(listener);
      myAccessors.addActionListener(listener);

      ((MyLabel)myClassVisibilityCb).setupVisibilityLabel(() -> myClassVisibility, modifier -> setClassVisibility(modifier));
      ((MyLabel)myFieldVisibilityCb).setupVisibilityLabel( () -> myFieldVisibility, modifier -> setFieldVisibility(modifier));
      ((MyLabel)myMethodVisibilityCb).setupVisibilityLabel(() -> myMethodVisibility, modifier -> setMethodVisibility(modifier));
      ((MyLabel)myMethodParameterVisibilityCb).setupVisibilityLabel(() -> myParameterVisibility, modifier -> setParameterVisibility(modifier));
    }

    public JComponent getPanel() {
      return myPanel;
    }

    private void createUIComponents() {
      myClassVisibilityCb = new MyLabel();
      myFieldVisibilityCb = new MyLabel();
      myMethodVisibilityCb = new MyLabel();
      myMethodParameterVisibilityCb = new MyLabel();
    }
  }

  private static class MyLabel extends JLabel implements UserActivityProviderComponent {

    @PsiModifier.ModifierConstant private static final String[] MODIFIERS =
      new String[]{PsiModifier.PUBLIC, PsiModifier.PROTECTED, PsiModifier.PACKAGE_LOCAL, PsiModifier.PRIVATE};

    private Set<ChangeListener> myListeners = new HashSet<>();

    public MyLabel() {
      setIcon(AllIcons.General.Combo2);
      setHorizontalTextPosition(SwingConstants.LEFT);
    }

    private void fireStateChanged() {
      for (ChangeListener listener : myListeners) {
        listener.stateChanged(new ChangeEvent(this));
      }
    }

    private static String getPresentableText(String modifier) {
      return StringUtil.capitalize(VisibilityUtil.toPresentableText(modifier));
    }

    private void setupVisibilityLabel(Producer<String> visibilityProducer, Consumer<String> setter) {
      setText(getPresentableText(visibilityProducer.produce()));
      new ClickListener() {
        @Override
        public boolean onClick(@NotNull MouseEvent e, int clickCount) {
          @SuppressWarnings("UseOfObsoleteCollectionType")
          Hashtable<Integer, JComponent> sliderLabels = new Hashtable<>();
          for (int i = 0; i < MODIFIERS.length; i++) {
            sliderLabels.put(i + 1, new JLabel(getPresentableText(MODIFIERS[i])));
          }

          JSlider slider = new JSlider(SwingConstants.VERTICAL, 1, MODIFIERS.length, 1);
          slider.setLabelTable(sliderLabels);
          slider.putClientProperty(UIUtil.JSLIDER_ISFILLED, Boolean.TRUE);
          slider.setPreferredSize(JBUI.size(150, 100));
          slider.setPaintLabels(true);
          slider.setSnapToTicks(true);
          slider.setValue(ArrayUtil.find(MODIFIERS, visibilityProducer.produce()) + 1);
          final JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(slider, null)
            .setCancelOnClickOutside(true)
            .createPopup();
          popup.addListener(new JBPopupAdapter() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
              final String modifier = MODIFIERS[slider.getValue() - 1];
              setter.consume(modifier);
              setText(getPresentableText(modifier));
              fireStateChanged();
            }
          });
          popup.show(new RelativePoint(MyLabel.this, new Point(getWidth(), 0)));
          return true;
        }
      }.installOn(this);
    }

    @Override
    public void setForeground(Color fg) {
      super.setForeground(isEnabled() ? UI.getColor("link.foreground") : fg);
    }

    @Override
    public void addChangeListener(ChangeListener changeListener) {
      myListeners.add(changeListener);
    }

    @Override
    public void removeChangeListener(ChangeListener changeListener) {
      myListeners.remove(changeListener);
    }
  }

  @Override
  @Nullable
  public JComponent createOptionsPanel() {
    return new OptionsPanel().getPanel();
  }
}
