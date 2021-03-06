/*
 * Copyright 2009 the original author or authors.
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
package org.codenarc.report

import org.codenarc.AnalysisContext
import org.codenarc.results.DirectoryResults
import org.codenarc.results.FileResults
import org.codenarc.rule.Rule
import org.codenarc.rule.StubRule
import org.codenarc.rule.Violation
import org.codenarc.rule.basic.ReturnFromFinallyBlockRule
import org.codenarc.rule.basic.ThrowExceptionFromFinallyBlockRule
import org.codenarc.rule.imports.DuplicateImportRule
import org.codenarc.rule.unnecessary.UnnecessaryBooleanInstantiationRule
import org.codenarc.rule.unnecessary.UnnecessaryStringInstantiationRule
import org.codenarc.ruleset.ListRuleSet
import org.codenarc.test.AbstractTestCase
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.codenarc.test.TestUtil.assertContainsAllInOrder
import static org.codenarc.test.TestUtil.shouldFailWithMessageContaining

/**
 * Tests for HtmlReportWriter
 *
 * @author Chris Mair
 */
class HtmlReportWriterTest extends AbstractTestCase {
    private static final LONG_LINE = 'throw new Exception() // Some very long message 1234567890123456789012345678901234567890'
    private static final MESSAGE = 'bad stuff'
    private static final LINE1 = 111
    private static final LINE2 = 222
    private static final LINE3 = 333
    private static final VIOLATION1 = new Violation(rule:new StubRule(name:'RULE1', priority:1), lineNumber:LINE1, sourceLine:'if (file) {')
    private static final VIOLATION2 = new Violation(rule:new StubRule(name:'RULE2', priority:2), lineNumber:LINE2, message:MESSAGE)
    private static final VIOLATION3 = new Violation(rule:new StubRule(name:'RULE3', priority:3), lineNumber:LINE3, sourceLine:LONG_LINE, message: 'Other info')
    private static final NEW_REPORT_FILE = 'target/NewReport.html'
    private static final TITLE = 'My Cool Project'

    private reportWriter
    private analysisContext
    private results
    private ruleSet

    @Test
    void testWriteReport() {
        reportWriter.writeReport(analysisContext, results)
        def actual = getReportText()
        def expected = new File('./src/test/groovy/org/codenarc/report/data/HtmlReportWriterTest.testWriteReport.html')
        assert actual
        assert expected
        assert actual == expected.text
    }

    @Test
    void testWriteReport_Priority4() {
        VIOLATION1.rule.name = 'RULE4'
        VIOLATION1.rule.priority = 4
        reportWriter.writeReport(analysisContext, results)
        def actual = getReportText()

        def expected = new File('./src/test/groovy/org/codenarc/report/data/HtmlReportWriterTest.testWriteReport_Priority4.html')
        assert actual
        assert expected
        assert actual == expected.text
    }

    @Test
    void testWriteReport_NoDescriptionsForRuleIds() {
        ruleSet = new ListRuleSet([new StubRule(name:'MyRuleXX'), new StubRule(name:'MyRuleYY')])
        reportWriter.customMessagesBundleName = 'DoesNotExist'
        analysisContext.ruleSet = ruleSet
        reportWriter.writeReport(analysisContext, results)
        def actual = getReportText()

        def expected = new File('./src/test/groovy/org/codenarc/report/data/HtmlReportWriterTest.testWriteReport_NoDescriptionsForRuleIds.html')
        assert actual
        assert expected
        assert actual == expected.text
    }

    @Test
    void testWriteReport_RuleDescriptionsProvidedInCodeNarcMessagesFile() {
        def biRule = new UnnecessaryBooleanInstantiationRule()
        ruleSet = new ListRuleSet([new StubRule(name:'MyRuleXX'), new StubRule(name:'MyRuleYY'), biRule])
        analysisContext.ruleSet = ruleSet
        reportWriter.writeReport(analysisContext, results)
        def actual = getReportText()
        def expected = new File('./src/test/groovy/org/codenarc/report/data/HtmlReportWriterTest.testWriteReport_RuleDescriptionsProvidedInCodeNarcMessagesFile.html')
        assert actual
        assert expected
        assert actual == expected.text
    }

    @Test
    void testWriteReport_RuleDescriptionsSetDirectlyOnTheRule() {
        ruleSet = new ListRuleSet([
                new StubRule(name:'MyRuleXX', description:'description77'),
                new StubRule(name:'MyRuleYY', description:'description88')])
        analysisContext.ruleSet = ruleSet
        reportWriter.writeReport(analysisContext, results)
        def actual = getReportText()
        def expected = new File('./src/test/groovy/org/codenarc/report/data/HtmlReportWriterTest.testWriteReport_RuleDescriptionsSetDirectlyOnTheRule.html')
        assert actual
        assert expected
        assert actual == expected.text
    }

    @Test
    void testWriteReport_DoesNotIncludeRuleDescriptionsForDisabledRules() {
        ruleSet = new ListRuleSet([
                new StubRule(name:'MyRuleXX', enabled:false),
                new StubRule(name:'MyRuleYY'),
                new StubRule(name:'MyRuleZZ', enabled:false)])
        analysisContext.ruleSet = ruleSet
        reportWriter.writeReport(analysisContext, results)
        def reportText = getReportText()
        assert !reportText.contains('MyRuleXX')
        assert !reportText.contains('MyRuleZZ')
    }

    @Test
    void testWriteReport_IncludesRuleThatDoesNotSupportGetDescription() {
        analysisContext.ruleSet = new ListRuleSet([ [getName:{ 'RuleABC' }, getPriority: { 2 } ] as Rule])
        reportWriter.writeReport(analysisContext, results)
        assertContainsAllInOrder(getReportText(), ['RuleABC', 'No description'])
    }

    @Test
    void testWriteReport_SetOutputFileAndTitle() {
        final OUTPUT_FILE = NEW_REPORT_FILE
        reportWriter.outputFile = OUTPUT_FILE
        reportWriter.title = TITLE
        reportWriter.writeReport(analysisContext, results)
        def actual = getReportText(OUTPUT_FILE)
        def expected = new File('./src/test/groovy/org/codenarc/report/data/HtmlReportWriterTest.testWriteReport_SetOutputFileAndTitle.html')
        assert actual
        assert expected
        assert actual == expected.text
    }

    @Test
    void testWriteReport_NullResults() {
        shouldFailWithMessageContaining('results') { reportWriter.writeReport(analysisContext, null) }
    }

    @Test
    void testWriteReport_NullAnalysisContext() {
        shouldFailWithMessageContaining('analysisContext') { reportWriter.writeReport(null, results) }
    }

    @Test
    void testIsDirectoryContainingFilesWithViolations() {
        def results = new FileResults('', [])
        assert !reportWriter.isDirectoryContainingFilesWithViolations(results)

        results = new FileResults('', [VIOLATION1])
        assert !reportWriter.isDirectoryContainingFilesWithViolations(results)

        results = new DirectoryResults('')
        assert !reportWriter.isDirectoryContainingFilesWithViolations(results)

        results.addChild(new FileResults('', []))
        assert !reportWriter.isDirectoryContainingFilesWithViolations(results), 'child with no violations'

        def child = new DirectoryResults('')
        child.addChild(new FileResults('', [VIOLATION1]))
        results.addChild(child)
        assert !reportWriter.isDirectoryContainingFilesWithViolations(results), 'grandchild with violations'

        results.addChild(new FileResults('', [VIOLATION1]))
        assert reportWriter.isDirectoryContainingFilesWithViolations(results)
    }

    @Test
    void testIsDirectoryContainingFiles() {
        def results = new FileResults('', [])
        assert !reportWriter.isDirectoryContainingFiles(results)

        results = new DirectoryResults('')
        assert !reportWriter.isDirectoryContainingFiles(results)

        results.numberOfFilesInThisDirectory = 2
        assert reportWriter.isDirectoryContainingFiles(results)
    }

    @Test
    void testFormatSourceLine() {
        assert reportWriter.formatSourceLine('') == null
        assert reportWriter.formatSourceLine('abc') == 'abc'
        assert reportWriter.formatSourceLine('abcdef' * 20) == 'abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefab..abcdefabcdef'
        assert reportWriter.formatSourceLine('abc', 2) == 'abc'
        assert reportWriter.formatSourceLine('abcdef' * 20, 2) == 'cdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd..abcdefabcdef'
    }

    @Before
    void setUpHtmlReportWriterTest() {
        reportWriter = new HtmlReportWriter() 
        reportWriter.metaClass.getFormattedTimestamp << { 'Feb 24, 2011 9:32:38 PM' }
        reportWriter.metaClass.getCodeNarcVersion << { '0.12' }

        def dirResultsMain = new DirectoryResults('src/main', 1)
        def dirResultsCode = new DirectoryResults('src/main/code', 2)
        def dirResultsTest = new DirectoryResults('src/main/test', 3)
        def dirResultsTestSubdirNoViolations = new DirectoryResults('src/main/test/noviolations', 4)
        def dirResultsTestSubdirEmpty = new DirectoryResults('src/main/test/empty')
        def fileResults1 = new FileResults('src/main/MyAction.groovy', [VIOLATION1, VIOLATION3, VIOLATION3, VIOLATION1, VIOLATION2])
        def fileResults2 = new FileResults('src/main/MyAction2.groovy', [VIOLATION3])
        def fileResults3 = new FileResults('src/main/MyActionTest.groovy', [VIOLATION1, VIOLATION2])
        dirResultsMain.addChild(fileResults1)
        dirResultsMain.addChild(dirResultsCode)
        dirResultsMain.addChild(dirResultsTest)
        dirResultsCode.addChild(fileResults2)
        dirResultsTest.addChild(fileResults3)
        dirResultsTest.addChild(dirResultsTestSubdirNoViolations)
        dirResultsTest.addChild(dirResultsTestSubdirEmpty)
        results = new DirectoryResults()
        results.addChild(dirResultsMain)

        ruleSet = new ListRuleSet([
                new UnnecessaryBooleanInstantiationRule(),
                new ReturnFromFinallyBlockRule(),
                new UnnecessaryStringInstantiationRule(),
                new ThrowExceptionFromFinallyBlockRule(),
                new DuplicateImportRule()
        ])
        analysisContext = new AnalysisContext(sourceDirectories:['/src/main'], ruleSet:ruleSet)
    }

    @After
    void tearDownHtmlReportWriterTest() {
        new File(NEW_REPORT_FILE).delete()
        new File(HtmlReportWriter.DEFAULT_OUTPUT_FILE).delete()
    }

    private String getReportText(String filename=HtmlReportWriter.DEFAULT_OUTPUT_FILE) {
        new File(filename).text
    }

}
