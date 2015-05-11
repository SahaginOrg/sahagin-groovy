package org.sahagin.groovy.runlib.srctreegen.SrcTreeGeneratorTestRes.variousData.input

import geb.spock.GebSpec
import org.junit.Test

class TestMainSpec extends GebSpec {

    @Test
    void B() {}

    def Adfafafa() {
        when: "AAAAAAAAAAA"
        println "1111111"

        then: "BBBBBBBBBB"
        println "2222222"
        println "3333333"

        when: "CCCCCCCCCCCCCCCCC"
        println "4444444"

        then:
        println "5555555"
        println "6666666"
   }

    private def goToLogin() {
        when:
        println "A"
        println "B"
        println "C"

        then:
        println "D"
        println "E"
        println "F"
    }

    def goToLogin2() {
        when:
        println "ABC"

        then:
        println "DEF"
    }
}