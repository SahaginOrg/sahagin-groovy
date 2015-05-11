package org.sahagin.groovy.runlib.srctreegen

import org.sahagin.groovy.runlib.external.adapter.geb.GebAdapter
import org.sahagin.groovy.runlib.srctreegen.SrcTreeGenerator
import org.sahagin.groovy.share.GroovyConfig
import org.sahagin.report.HtmlReport
import org.sahagin.runlib.external.adapter.AdapterContainer
import org.sahagin.runlib.external.Locale
import org.sahagin.share.AcceptableLocales
import org.sahagin.share.SysMessages
import org.sahagin.share.srctree.SrcTree
import org.junit.Test
import org.sahagin.share.yaml.YamlUtils

class SrcTreeGeneratorTest {

    @Test
    void variousData() {
        GroovyConfig config = GroovyConfig.generateFromYamlConfig(
            new File("src/test/groovy/org/sahagin/groovy/runlib/srctreegen/sahagin.yml"))

        AcceptableLocales locales = AcceptableLocales.getInstance(config.getUserLocale())
        AdapterContainer.globalInitialize(locales, config.getTestFramework())
        SysMessages.globalInitialize(locales)
        new GebAdapter().initialSetAdapter()

        // TODO dummy
        SrcTreeGenerator gen = new SrcTreeGenerator(
            AdapterContainer.globalInstance().getAdditionalTestDocs(), locales)
        SrcTree srcTree = gen.generateWithRuntimeClassPath(config.getRootBaseTestDir())
        YamlUtils.dump(srcTree.toYamlObject(), new File(config.getRootBaseReportIntermediateDataDir(), "srcTree"))
        HtmlReport report = new HtmlReport()
        report.generate(config.getRootBaseReportIntermediateDataDir(), config.getRootBaseReportOutputDir())
    }

}
