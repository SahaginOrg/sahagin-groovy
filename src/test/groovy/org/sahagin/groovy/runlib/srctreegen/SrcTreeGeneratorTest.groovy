package org.sahagin.groovy.runlib.srctreegen

import org.sahagin.groovy.runlib.external.adapter.geb.GebAdapter
import org.sahagin.groovy.runlib.srctreegen.SrcTreeGenerator
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

        AdapterContainer.globalInitialize(
                AcceptableLocales.getInstance(Locale.JA_JP), "junit4")
        SysMessages.globalInitialize(AcceptableLocales.getInstance(Locale.JA_JP))
        new GebAdapter().initialSetAdapter()

        // TODO dummy
        SrcTreeGenerator gen = new SrcTreeGenerator(
            AdapterContainer.globalInstance().getAdditionalTestDocs(), AcceptableLocales.getInstance(Locale.JA_JP))
        File rootDir = new File("src/test/resources/org/sahagin/groovy/runlib/srctreegen/SrcTreeGeneratorTestRes/variousData/input")
        SrcTree srcTree = gen.generateWithRuntimeClassPath(rootDir)
        YamlUtils.dump(srcTree.toYamlObject(), new File("report-in/srcTree"))
        HtmlReport report = new HtmlReport()
        report.generate(new File("report-in"), new File("report-out"))

    }

}
