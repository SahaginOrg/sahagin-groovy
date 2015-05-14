package org.sahagin.groovy.runlib.srctreegen

import org.sahagin.groovy.runlib.external.adapter.GroovyAdapterContainer
import org.sahagin.groovy.runlib.external.adapter.geb.GebAdapter
import org.sahagin.groovy.runlib.external.adapter.javalib.GroovyJavaLibAdapter
import org.sahagin.groovy.runlib.external.adapter.junit4.GroovyJUnit4Adapter
import org.sahagin.groovy.runlib.external.adapter.spock.SpockAdapter
import org.sahagin.groovy.runlib.srctreegen.GroovySrcTreeGenerator
import org.sahagin.groovy.share.GroovyConfig
import org.sahagin.report.HtmlReport
import org.sahagin.runlib.external.adapter.Adapter
import org.sahagin.runlib.external.adapter.AdapterContainer
import org.sahagin.runlib.external.Locale
import org.sahagin.share.AcceptableLocales
import org.sahagin.share.SysMessages
import org.sahagin.share.srctree.SrcTree
import org.junit.Test
import org.sahagin.share.yaml.YamlUtils

class GroovySrcTreeGeneratorTest {

    @Test
    void variousData() {
        GroovyConfig config = GroovyConfig.generateFromYamlConfig(
            new File("src/test/groovy/org/sahagin/groovy/runlib/srctreegen/sahagin.yml"))

        AcceptableLocales locales = AcceptableLocales.getInstance(config.getUserLocale())
        GroovyAdapterContainer.globalInitialize(locales, config.getTestFramework())
        SysMessages.globalInitialize(locales)
        new GroovyJavaLibAdapter().initialSetAdapter()
        new GroovyJUnit4Adapter().initialSetAdapter()
        new SpockAdapter().initialSetAdapter()
        new GebAdapter().initialSetAdapter()

        for (String adapterClassName : config.getAdapterClassNames()) {
            // TODO handle exception thrown by forName or newInstance method
            // more appropriately
            Class<?> adapterClass = Class.forName(adapterClassName)
            assert adapterClass != null
            Object adapterObj = adapterClass.newInstance()
            assert adapterObj != null
            assert adapterObj instanceof Adapter
            Adapter adapter = (Adapter) adapterObj
            adapter.initialSetAdapter()
        }

        // TODO dummy
        GroovySrcTreeGenerator gen = new GroovySrcTreeGenerator(
            AdapterContainer.globalInstance().getAdditionalTestDocs(), locales)

        if (!GroovyAdapterContainer.globalInstance().isRootMethodAdapterSet()) {
            throw new RuntimeException("TODO message here")
        }

        SrcTree srcTree = gen.generateWithRuntimeClassPath(config.getRootBaseTestDir())
        YamlUtils.dump(srcTree.toYamlObject(), new File(config.getRootBaseReportIntermediateDataDir(), "srcTree"))
        HtmlReport report = new HtmlReport()
        report.generate(config.getRootBaseReportIntermediateDataDir(), config.getRootBaseReportOutputDir())
    }

}
