package org.sahagin.groovy.main

import java.lang.instrument.Instrumentation

class SahaginPreMain {

    static void premain(String agentArgs, Instrumentation inst) {
        println "echo"
    }
}
