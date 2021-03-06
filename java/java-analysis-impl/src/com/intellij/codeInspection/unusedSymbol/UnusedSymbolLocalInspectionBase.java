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

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.codeInspection.deadCode.UnusedDeclarationInspectionBase;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiModifier;
import org.intellij.lang.annotations.Pattern;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UnusedSymbolLocalInspectionBase extends BaseJavaLocalInspectionTool {
  @NonNls public static final String SHORT_NAME = HighlightInfoType.UNUSED_SYMBOL_SHORT_NAME;
  @NonNls public static final String DISPLAY_NAME = HighlightInfoType.UNUSED_SYMBOL_DISPLAY_NAME;
  @NonNls public static final String UNUSED_PARAMETERS_SHORT_NAME = "UnusedParameters";
  @NonNls public static final String UNUSED_ID = "unused";

  public boolean LOCAL_VARIABLE = true;
  public boolean FIELD = true;
  public boolean METHOD = true;
  public boolean CLASS = true;
  public boolean PARAMETER = true;
  public boolean REPORT_PARAMETER_FOR_PUBLIC_METHODS = true;

  protected String myClassVisibility = PsiModifier.PUBLIC;
  protected String myFieldVisibility = PsiModifier.PUBLIC;
  protected String myMethodVisibility = PsiModifier.PUBLIC;
  protected String myParameterVisibility = PsiModifier.PUBLIC;
  private boolean myIgnoreAccessors = false;


  @PsiModifier.ModifierConstant
  @Nullable
  public String getClassVisibility() {
    if (!CLASS) return null;
    return myClassVisibility;
  }
  @PsiModifier.ModifierConstant
  @Nullable
  public String getFieldVisibility() {
    if (!FIELD) return null;
    return myFieldVisibility;
  }
  @PsiModifier.ModifierConstant
  @Nullable
  public String getMethodVisibility() {
    if (!METHOD) return null;
    return myMethodVisibility;
  }

  @PsiModifier.ModifierConstant
  @Nullable
  public String getParameterVisibility() {
    if (!PARAMETER) return null;
    return myParameterVisibility;
  }

  public void setClassVisibility(String classVisibility) {
    this.myClassVisibility = classVisibility;
  }

  public void setFieldVisibility(String fieldVisibility) {
    this.myFieldVisibility = fieldVisibility;
  }

  public void setMethodVisibility(String methodVisibility) {
    this.myMethodVisibility = methodVisibility;
  }

  public void setParameterVisibility(String parameterVisibility) {
    REPORT_PARAMETER_FOR_PUBLIC_METHODS = PsiModifier.PUBLIC.equals(parameterVisibility);
    this.myParameterVisibility = parameterVisibility;
  }

  public boolean isIgnoreAccessors() {
    return myIgnoreAccessors;
  }

  public void setIgnoreAccessors(boolean ignoreAccessors) {
    myIgnoreAccessors = ignoreAccessors;
  }

  @Override
  @NotNull
  public String getGroupDisplayName() {
    return GroupNames.DECLARATION_REDUNDANCY;
  }

  @Override
  @NotNull
  public String getDisplayName() {
    return DISPLAY_NAME;
  }

  @Override
  @NotNull
  @NonNls
  public String getShortName() {
    return SHORT_NAME;
  }

  @Override
  @Pattern(VALID_ID_PATTERN)
  @NotNull
  @NonNls
  public String getID() {
    return UNUSED_ID;
  }

  @Override
  public String getAlternativeID() {
    return UnusedDeclarationInspectionBase.ALTERNATIVE_ID;
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @Override
  public void writeSettings(@NotNull Element node) throws WriteExternalException {
    writeVisibility(node, myClassVisibility, "inner_class");
    writeVisibility(node, myFieldVisibility, "field");
    writeVisibility(node, myMethodVisibility, "method");
    writeVisibility(node, "parameter", myParameterVisibility, getParameterDefaultVisibility());
    if (myIgnoreAccessors) {
      node.setAttribute("ignoreAccessors", Boolean.toString(true));
    }
    super.writeSettings(node);
  }

  private static void writeVisibility(Element node, String visibility, String type) {
    writeVisibility(node, type, visibility, PsiModifier.PUBLIC);
  }

  private static void writeVisibility(Element node,
                                      String type,
                                      String visibility,
                                      String defaultVisibility) {
    if (!defaultVisibility.equals(visibility)) {
      node.setAttribute(type, visibility);
    }
  }

  private String getParameterDefaultVisibility() {
    return REPORT_PARAMETER_FOR_PUBLIC_METHODS ? PsiModifier.PUBLIC : PsiModifier.PRIVATE;
  }

  @Override
  public void readSettings(@NotNull Element node) throws InvalidDataException {
    super.readSettings(node);
    myClassVisibility = readVisibility(node, "inner_class");
    myFieldVisibility = readVisibility(node, "field");
    myMethodVisibility = readVisibility(node, "method");
    myParameterVisibility = readVisibility(node, "parameter", getParameterDefaultVisibility());
    final String ignoreAccessors = node.getAttributeValue("ignoreAccessors");
    myIgnoreAccessors = ignoreAccessors != null && Boolean.parseBoolean(ignoreAccessors);
  }

  private static String readVisibility(@NotNull Element node, final String type) {
    return readVisibility(node, type, PsiModifier.PUBLIC);
  }

  private static String readVisibility(@NotNull Element node,
                                       final String type,
                                       final String defaultVisibility) {
    final String visibility = node.getAttributeValue(type);
    if (visibility == null) {
      return defaultVisibility;
    }
    return visibility;
  }
}
