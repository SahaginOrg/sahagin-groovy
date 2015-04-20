package org.sahagin.groovy.runlib.srctreegen

import org.sahagin.groovy.runlib.srctreegen.SrcTreeGenerator
import org.sahagin.report.HtmlReport
import org.sahagin.share.srctree.SrcTree
import org.junit.Test
import org.sahagin.share.yaml.YamlUtils

class SrcTreeGeneratorTest {

    @Test
    void variousData() {
        // TODO dummy
        SrcTreeGenerator gen = new SrcTreeGenerator()
        File rootDir = new File("src/test/resources/org/sahagin/groovy/runlib/srctreegen/SrcTreeGeneratorTestRes/variousData/input")
        SrcTree srcTree = gen.generateWithRuntimeClassPath(rootDir)
        YamlUtils.dump(srcTree.toYamlObject(), new File("report-in/srcTree"))
        HtmlReport report = new HtmlReport()
        report.generate(new File("report-in"), new File("report-out"))

    }

}
