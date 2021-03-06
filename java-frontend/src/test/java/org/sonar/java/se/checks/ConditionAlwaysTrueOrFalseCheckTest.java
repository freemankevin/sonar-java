/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.java.se.checks;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.sonar.java.se.AlwaysTrueOrFalseExpressionCollector;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.SETestUtils;
import org.sonar.java.testing.CheckVerifier;
import org.sonar.java.testing.InternalCheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.TestUtils.testSourcesPath;

class ConditionAlwaysTrueOrFalseCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("symbolicexecution/checks/ConditionAlwaysTrueOrFalseCheck.java"))
      .withChecks(new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_boolean_wrapper() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("symbolicexecution/checks/BooleanWrapper.java"))
      .withChecks(new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_without_jsr305() {
    List<File> classpath = FileUtils.listFiles(new File("target/test-jars"), new String[] {"jar"}, true).stream()
      .filter(file -> file.getName().startsWith("spring-core-") || file.getName().startsWith("spring-web-"))
      .collect(Collectors.toList());
    classpath.add(new File("target/test-classes"));
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("symbolicexecution/checks/SpringNullableAndNonNullAnnotationsWithoutJSR305.java"))
      .withCheck(new BooleanGratuitousExpressionsCheck())
      .withClassPath(classpath)
      .verifyIssues();
  }

  @Test
  void test_unreachable_vs_gratuitous() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("symbolicexecution/checks/UnreachableOrGratuitous.java"))
      .withCheck(new ConditionalUnreachableCodeCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void whole_stack_required_for_ps_equality() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("symbolicexecution/checks/PsEqualityRequiresFullStack.java"))
      .withCheck(new AssertNoAlwaysTrueOrFalseExpression())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyNoIssues();
  }

  @Test
  void condition_always_true_with_optional() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("symbolicexecution/checks/ConditionAlwaysTrueWithOptional.java"))
      .withCheck(new AssertNoAlwaysTrueOrFalseExpression())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyNoIssues();
  }

  @Test
  void resetFields_ThreadSleepCalls() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("symbolicexecution/checks/ThreadSleepCall.java"))
      .withCheck(new AssertNoAlwaysTrueOrFalseExpression())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyNoIssues();
  }

  @Test
  void reporting() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("symbolicexecution/checks/ConditionAlwaysTrueOrFalseCheckReporting.java"))
      .withChecks(new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void max_returned_flows() {
    ((InternalCheckVerifier) CheckVerifier.newVerifier())
      .withCustomIssueVerifier(issues -> {
        assertThat(issues).hasSize(2);
        assertThat(issues.iterator().next().flows).hasSize(20);
      })
      .onFile(testSourcesPath("symbolicexecution/checks/ConditionAlwaysTrueOrFalseCheckMaxReturnedFlows.java"))
      .withChecks(new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void reporting_getting_wrong_parent() {
    // Checks flow iterating through the correct parent
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("symbolicexecution/checks/ConditionAlwaysTrueOrFalseCheckParentLoop.java"))
      .withChecks(new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_transitivity() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("symbolicexecution/checks/Transitivity.java"))
      .withChecks(new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  private static class AssertNoAlwaysTrueOrFalseExpression extends SECheck {
    @Override
    public void checkEndOfExecution(CheckerContext context) {
      AlwaysTrueOrFalseExpressionCollector atof = context.alwaysTrueOrFalseExpressions();
      assertThat(atof.alwaysFalse()).isEmpty();
      assertThat(atof.alwaysTrue()).isEmpty();
    }
  }

  @Test
  void test_constraint_is_not_lost_after_copying() {
    // see also SONARJAVA-2351
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("symbolicexecution/checks/ConstraintCopy.java"))
      .withChecks(new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_binary_expressions_always_not_null() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("symbolicexecution/checks/BinaryExpressionNotNull.java"))
      .withChecks(new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }
}
