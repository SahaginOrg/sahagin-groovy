package org.sahagin.groovy.runlib.srctreegen

import org.sahagin.groovy.runlib.external.adapter.geb.GebAdapter
import org.sahagin.groovy.runlib.srctreegen.SrcTreeGenerator
import org.sahagin.report.HtmlReport
import org.sahagin.runlib.external.adapter.AdapterContainer
import org.sahagin.share.AcceptableLocales
import org.sahagin.share.SysMessages
import org.sahagin.share.srctree.SrcTree
import org.junit.Test
import org.sahagin.share.yaml.YamlUtils

class SrcTreeGeneratorTest {

    @Test
    void variousData() {

        AdapterContainer.globalInitialize(
                AcceptableLocales.getInstance(null), "junit4")
        SysMessages.globalInitialize(AcceptableLocales.getInstance(null))
        new GebAdapter().initialSetAdapter()

        // TODO dummy
        SrcTreeGenerator gen = new SrcTreeGenerator(
            AdapterContainer.globalInstance().getAdditionalTestDocs(), null)
        File rootDir = new File("src/test/resources/org/sahagin/groovy/runlib/srctreegen/SrcTreeGeneratorTestRes/variousData/input")
        SrcTree srcTree = gen.generateWithRuntimeClassPath(rootDir)
        YamlUtils.dump(srcTree.toYamlObject(), new File("report-in/srcTree"))
        HtmlReport report = new HtmlReport()
        report.generate(new File("report-in"), new File("report-out"))

    }

}
