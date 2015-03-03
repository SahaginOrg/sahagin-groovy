package org.sahagin.groovy.runlib.srctreegen

import org.sahagin.groovy.runlib.srctreegen.SrcTreeGenerator
import org.sahagin.share.srctree.SrcTree
import org.junit.Test
import org.sahagin.share.yaml.YamlUtils

class SrcTreeGeneratorTest {
    
    @Test
    void variousData() {      
        // TODO dummy
        SrcTreeGenerator gen = new SrcTreeGenerator()
        String[] files = ["src/test/resources/org/sahagin/groovy/runlib/srctreegen/SrcTreeGeneratorTestRes/variousData/input/TestMain.groovy", \
                      "src/test/resources/org/sahagin/groovy/runlib/srctreegen/SrcTreeGeneratorTestRes/variousData/input/TestSub.groovy"]
        SrcTree srcTree = gen.generate(files)
        YamlUtils.dump(srcTree.toYamlObject(), new File("a/result.txt"))
        
    }

}
