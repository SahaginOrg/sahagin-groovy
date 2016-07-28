package org.sahagin.groovy.runlib.runresultsgen

import java.io.File
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List
import java.util.logging.Logger

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.sahagin.groovy.runlib.external.adapter.GroovyAdapterContainer
import org.sahagin.groovy.runlib.external.adapter.geb.GebAdapter
import org.sahagin.groovy.runlib.external.adapter.javalib.GroovyJavaLibAdapter
import org.sahagin.groovy.runlib.external.adapter.javasystem.GroovyJavaSystemAdapter
import org.sahagin.groovy.runlib.external.adapter.junit4.GroovyJUnit4Adapter
import org.sahagin.groovy.runlib.external.adapter.spock.SpockAdapter
import org.sahagin.groovy.runlib.external.adapter.webdriver.GroovyWebDriverAdapter
import org.sahagin.groovy.runlib.srctreegen.GroovySrcTreeGenerator
import org.sahagin.groovy.share.GroovyConfig
import org.sahagin.report.HtmlReport
import org.sahagin.runlib.external.adapter.Adapter
import org.sahagin.runlib.external.adapter.AdapterContainer
import org.sahagin.runlib.runresultsgen.HookMethodDef
import org.sahagin.runlib.runresultsgen.HookMethodManager
import org.sahagin.runlib.runresultsgen.StackLineUtils
import org.sahagin.share.AcceptableLocales
import org.sahagin.share.CommonPath
import org.sahagin.share.IllegalDataStructureException
import org.sahagin.share.IllegalTestScriptException
import org.sahagin.share.Logging
import org.sahagin.share.SrcTreeChecker
import org.sahagin.share.SysMessages
import org.sahagin.share.runresults.LineScreenCapture
import org.sahagin.share.runresults.RootMethodRunResult
import org.sahagin.share.runresults.RunFailure
import org.sahagin.share.runresults.StackLine
import org.sahagin.share.srctree.SrcTree
import org.sahagin.share.srctree.TestMethod
import org.sahagin.share.yaml.YamlConvertException
import org.sahagin.share.yaml.YamlUtils

class GroovyHookMethodDef {
    private static final String MSG_TEST_FRAMEWORK_NOT_FOUND = "testFramework not found: %s"
    private static Logger logger = Logging.getLogger(GroovyHookMethodDef.class.name)
    private static HookMethodManager manager = null
    
    private static boolean javaAgentArgCheck() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        for (String arg : runtimeMxBean.getInputArguments()) {
            if (arg != null && arg.toLowerCase().startsWith("-javaagent")) {
                // should not set both Sahagin-groovy hook and Sahagin-Java hook at the same time
                // TODO don't throw error if javaagent option is not for sahagin-xxx.jar
                throw new RuntimeException("Sahagin-groovy does not support javaagent option now")
            }
        }
    }
    
    static void initialize() {
        if (manager != null) {
            return
        }
        javaAgentArgCheck()
        
        String configFilePath
        String propValue = System.getProperty("sahagin.configPath")
        if (propValue == null || propValue == "") {
            configFilePath = "sahagin.yml"
        } else {
            configFilePath = propValue
        }
        GroovyConfig config = GroovyConfig.generateFromYamlConfig(new File(configFilePath))
        Logging.setLoggerEnabled(config.isOutputLog())
        logger.info("initialize")
        AcceptableLocales locales = AcceptableLocales.getInstance(config.userLocale)
        GroovyAdapterContainer.globalInitialize(locales, config.testFramework)
        SysMessages.globalInitialize(locales)

        // default adapters
        new GroovyJavaSystemAdapter().initialSetAdapter()
        new GroovyJavaLibAdapter().initialSetAdapter()
        new GroovyJUnit4Adapter().initialSetAdapter()
        new GroovyWebDriverAdapter().initialSetAdapter()
        new SpockAdapter().initialSetAdapter()
        new GebAdapter().initialSetAdapter()       

        for (String adapterClassName : config.adapterClassNames) {
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

        if (!GroovyAdapterContainer.globalInstance().isRootMethodAdapterSet()) {
            throw new RuntimeException(String.format(
                    MSG_TEST_FRAMEWORK_NOT_FOUND, config.testFramework))
        }

        // delete previous data
        if (config.rootBaseReportIntermediateDataDir.exists()) {
            FileUtils.deleteDirectory(config.rootBaseReportIntermediateDataDir)
        }
        
        SrcTree srcTree = generateAndDumpSrcTree(config, locales)
        manager = new HookMethodManager(srcTree, config)
        
        if (!config.isRunTestOnly()) {
            // set up shutdown hook which generates HTML report
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    HtmlReport report = new HtmlReport()
                    try {
                        report.generate(config.rootBaseReportInputIntermediateDataDirs,
                            config.rootBaseReportOutputDir)
                    } catch (IllegalDataStructureException | IllegalTestScriptException e) {
                        throw new RuntimeException(e)
                    }
                }
            })
        }
    }

    private static SrcTree generateAndDumpSrcTree(GroovyConfig config, AcceptableLocales locales) {
        // generate and dump srcTree
        GroovySrcTreeGenerator generator = new GroovySrcTreeGenerator(
                AdapterContainer.globalInstance().additionalTestDocs, locales)
        File srcTreeFile = CommonPath.srcTreeFile(config.rootBaseRunOutputIntermediateDataDir)
        SrcTree srcTree = generator.generateWithRuntimeClassPath(config.rootBaseTestDir)
        SrcTreeChecker.check(srcTree)
        YamlUtils.dump(srcTree.toYamlObject(), srcTreeFile)
        return srcTree
    }

    private static void initializedCheck() {
        if (manager == null) {
            throw new IllegalStateException("initialize first")
        }
    }

    public static void beforeMethodHook(String hookedClassQualifiedName, 
        String hookedMethodSimpleName, String actualHookedMethodSimpleName) {
        initializedCheck()
        manager.beforeMethodHook(hookedClassQualifiedName, 
            hookedMethodSimpleName, actualHookedMethodSimpleName)
    }

    public static void methodErrorHook(
            String hookedClassQualifiedName, String hookedMethodSimpleName, Throwable e) {
        initializedCheck()
        manager.methodErrorHook(hookedClassQualifiedName, hookedMethodSimpleName, e)
    }

    public static void afterMethodHook(
            String hookedClassQualifiedName, String hookedMethodSimpleName) {
        initializedCheck()
        manager.afterMethodHook(hookedClassQualifiedName, hookedMethodSimpleName)
    }

    public static void beforeCodeLineHook(String hookedClassQualifiedName, String hookedMethodSimpleName,
            String actualHookedMethodSimpleName, String hookedArgClassesStr, 
            int hookedLine, int actualhookedLine) {
        initializedCheck()
        manager.beforeCodeLineHook(hookedClassQualifiedName,
                hookedMethodSimpleName, actualHookedMethodSimpleName,
                hookedArgClassesStr, hookedLine, actualhookedLine)
    }

    public static void afterCodeLineHook(String hookedClassQualifiedName, String hookedMethodSimpleName,
            String actualHookedMethodSimpleName, String hookedArgClassesStr,
            int hookedLine, int actualhookedLine) {
        initializedCheck()
        manager.afterCodeLineHook(hookedClassQualifiedName,
                hookedMethodSimpleName, actualHookedMethodSimpleName,
                hookedArgClassesStr, hookedLine, actualhookedLine)
    }
}