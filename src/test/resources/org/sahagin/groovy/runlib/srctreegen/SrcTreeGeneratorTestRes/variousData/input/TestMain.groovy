package org.sahagin.groovy.runlib.srctreegen.SrcTreeGeneratorTestRes.variousData.input

import org.junit.Test

class TestMain {

    @Test
    void test3() {
        TestSub s = new TestSub()
        s.sub()
        println 'ABC'
    }

    @Test
    void test4() {
        println 'ABC'
    }
}
