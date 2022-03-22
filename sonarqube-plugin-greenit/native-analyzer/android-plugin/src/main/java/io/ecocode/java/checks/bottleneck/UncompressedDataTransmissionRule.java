/*
 * ecoCode SonarQube Plugin
 * Copyright (C) 2020-2021 Snapp' - Université de Pau et des Pays de l'Adour
 * mailto: contact@ecocode.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package io.ecocode.java.checks.bottleneck;

import com.google.common.collect.ImmutableList;
import io.ecocode.java.checks.helpers.CheckArgumentComplexType;
import org.sonar.check.Rule;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

/**
 * Checks the use of GzipOutputStream when using an output stream to check if the data outputted is compressed
 */
@Rule(key = "EBOT003", name = "ecoCodeUncompressedDataTransmission")
public class UncompressedDataTransmissionRule extends IssuableSubscriptionVisitor {
    private static final String ERROR_MESSAGE = "Prefer using GzipOutputStream instead of OutputStream to improve energy efficiency.";
    private static final MethodMatchers matcherUrlConnection = MethodMatchers.create().ofSubTypes("java.net.URLConnection").names("getOutputStream").addWithoutParametersMatcher().build();
    // TODO: 22/03/2022 Change methodMatcher for anything but a "new GZIPOutputStream()", the new being the problem here.
    //  I managed to compare it with another NEW_CLASS but it ain't gonna work for the other cases

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return ImmutableList.of(Tree.Kind.VARIABLE);
    }

    @Override
    public void visitNode(Tree tree) {
        super.visitNode(tree);
        if (tree.is(Tree.Kind.VARIABLE)) {
            VariableTree variableTree = (VariableTree) tree;
            if (variableTree.type().symbolType().fullyQualifiedName().equals("java.io.OutputStream")
                    && variableTree.initializer() != null) {
                checkMethodInitilization(variableTree.initializer(), variableTree.initializer());
            }
        }
    }

    /**
     * Method called after the initialisation of a OutputStream class to check if the data outputted is compressed
     *
     * @param treeToCheck  Parameter corresponding to the current tree that is being checked
     * @param treeToReport Parameter corresponding to the tree that will be reported (same as the upper parameter but without any change in case of a complex variable)
     */
    private void checkMethodInitilization(Tree treeToCheck, Tree treeToReport) {
        try {
            if (treeToCheck.is(Tree.Kind.METHOD_INVOCATION)
                    && matcherUrlConnection.matches((MethodInvocationTree) treeToCheck)) {
                reportIssue(treeToReport, ERROR_MESSAGE);
            } else if (treeToCheck.is(Tree.Kind.NEW_CLASS)
                    && !(((NewClassTreeImpl) treeToCheck).getConstructorIdentifier().name().equals("GZIPOutputStream"))) {
                reportIssue(treeToReport, ERROR_MESSAGE);
            } else {
                Tree returnedArgument = (Tree) CheckArgumentComplexType.getChildExpression((ExpressionTree) treeToCheck);
                if (returnedArgument != treeToCheck) {
                    checkMethodInitilization(returnedArgument, treeToReport);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}