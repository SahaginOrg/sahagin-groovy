package org.sahagin.groovy.runlib.srctreegen.SrcTreeGeneratorTestRes.variousData.input

import org.junit.Test
import geb.junit4.GebTest

class TestMain extends GebTest {

    @Test
    void test3() {
        go("http")
        $("#reserve_y").value("2014")
        $("#goto_next").click()

        TestSub s = new TestSub()
        s.sub()
        println 'ABC'
    }

    @Test
    void test4() {
        println 'ABC'
    }
}
