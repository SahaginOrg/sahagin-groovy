package org.sahagin.groovy.runlib.srctreegen.SrcTreeGeneratorTestRes.variousData.input

import org.junit.Test
import org.sahagin.runlib.external.TestDoc

class TestSub {

    @Test
    void test() {
        TestMain m = new TestMain();
        println 'ABC'
    }
    
    @TestDoc("a")
    void test2() {
        println 'ABC'
    }
}
