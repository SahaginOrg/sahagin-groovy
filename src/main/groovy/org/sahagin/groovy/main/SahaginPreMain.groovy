package org.sahagin.groovy.main

import java.io.File
import java.lang.instrument.Instrumentation

import org.apache.commons.io.FileUtils
import org.sahagin.groovy.runlib.srctreegen.SrcTreeGenerator
import org.sahagin.main.SahaginMain
import org.sahagin.runlib.external.adapter.Adapter
import org.sahagin.runlib.external.adapter.AdapterContainer
import org.sahagin.runlib.external.adapter.fluentlenium.FluentLeniumAdapter
import org.sahagin.runlib.external.adapter.junit4.JUnit4Adapter
import org.sahagin.runlib.external.adapter.webdriver.WebDriverAdapter
import org.sahagin.runlib.runresultsgen.RunResultsGenerateHookSetter
import org.sahagin.share.AcceptableLocales
import org.sahagin.share.CommonPath
import org.sahagin.share.Config
import org.sahagin.share.IllegalTestScriptException
import org.sahagin.share.Logging
import org.sahagin.share.SrcTreeChecker
import org.sahagin.share.srctree.SrcTree
import org.sahagin.share.yaml.YamlUtils

class SahaginPreMain {

    static void premain(String agentArgs, Instrumentation inst) {
        String configFilePath
        if (agentArgs == null) {
            configFilePath = "sahagin.yml"
        } else {
            configFilePath = agentArgs
        }
        Config config = Config.generateFromYamlConfig(new File(configFilePath))
        AcceptableLocales locales = AcceptableLocales.getInstance(config.getUserLocale())
        AdapterContainer.globalInitialize(locales)

        // default adapters
        new JUnit4Adapter().initialSetAdapter()

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

        Logging.setLoggerEnabled(config.isOutputLog())

        // delete previous data
        if (config.getRootBaseReportIntermediateDataDir().exists()) {
            FileUtils.deleteDirectory(config.getRootBaseReportIntermediateDataDir())
        }

        SrcTree srcTree = generateAndDumpSrcTree(config, locales)

        // TODO temp
        String[] args = ["report", configFilePath]
        SahaginMain.main(args)
    }

    private static SrcTree generateAndDumpSrcTree(Config config, AcceptableLocales locales) {
        // generate and dump srcTree
        SrcTreeGenerator generator = new SrcTreeGenerator()
        File srcTreeFile = CommonPath.srcTreeFile(config.getRootBaseReportIntermediateDataDir())
        SrcTree srcTree = generator.generateWithRuntimeClassPath(config.getRootBaseTestDir())
        SrcTreeChecker.check(srcTree)
        YamlUtils.dump(srcTree.toYamlObject(), srcTreeFile)
        return srcTree
    }
}
