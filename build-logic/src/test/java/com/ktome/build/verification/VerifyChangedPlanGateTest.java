package com.ktome.build.verification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

final class VerifyChangedPlanGateTest {
    @Test
    void taskDependsOnTraversesIterableDependencyNotation() {
        Project project = ProjectBuilder.builder().build();
        Task target = project.getTasks().create("target");
        Task alias = project.getTasks().create("alias");
        alias.dependsOn(Collections.singletonList(target));

        assertTrue(taskDependsOn(alias, target));
    }

    @Test
    void taskDependsOnTraversesMapDependencyNotationValues() {
        Project project = ProjectBuilder.builder().build();
        Task target = project.getTasks().create("target");
        Task alias = project.getTasks().create("alias");
        alias.dependsOn(Map.of("selected", target));

        assertTrue(taskDependsOn(alias, target));
    }

    @Test
    void taskDependsOnTraversesCallableDependencyNotationResult() {
        Project project = ProjectBuilder.builder().build();
        Task target = project.getTasks().create("target");
        Task alias = project.getTasks().create("alias");
        Callable<Object> callableDependency = () -> Collections.singletonList(target);
        alias.dependsOn(callableDependency);

        assertTrue(taskDependsOn(alias, target));
    }

    @Test
    void taskDependsOnIgnoresUnrelatedIterableDependencyNotation() {
        Project project = ProjectBuilder.builder().build();
        Task target = project.getTasks().create("target");
        Task unrelated = project.getTasks().create("unrelated");
        Task alias = project.getTasks().create("alias");
        alias.dependsOn(Collections.singletonList(unrelated));

        assertFalse(taskDependsOn(alias, target));
    }

    private static boolean taskDependsOn(Task current, Task target) {
        try {
            Method method = VerifyChangedPlanGate.class.getDeclaredMethod(
                    "taskDependsOn", Task.class, Task.class, Set.class);
            method.setAccessible(true);
            return (boolean) method.invoke(null, current, target, new HashSet<String>());
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new AssertionError("Unable to invoke VerifyChangedPlanGate.taskDependsOn", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError("VerifyChangedPlanGate.taskDependsOn failed", cause);
        }
    }
}
