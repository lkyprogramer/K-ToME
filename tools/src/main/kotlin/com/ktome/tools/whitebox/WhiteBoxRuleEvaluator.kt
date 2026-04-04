package com.ktome.tools.whitebox

import com.ktome.core.harness.whitebox.WhiteBoxAggregateRule
import com.ktome.core.harness.whitebox.WhiteBoxAssertionResult
import com.ktome.core.harness.whitebox.WhiteBoxCaseRule

internal object WhiteBoxRuleEvaluator {
    fun <T> evaluateCaseRules(
        case: T,
        rules: List<WhiteBoxCaseRule<T>>,
    ): List<WhiteBoxAssertionResult> = rules.flatMap { rule -> rule.verify(case) }

    fun <T> evaluateAggregateRules(
        cases: List<T>,
        rules: List<WhiteBoxAggregateRule<T>>,
    ): List<WhiteBoxAssertionResult> = rules.flatMap { rule -> rule.verify(cases) }
}
