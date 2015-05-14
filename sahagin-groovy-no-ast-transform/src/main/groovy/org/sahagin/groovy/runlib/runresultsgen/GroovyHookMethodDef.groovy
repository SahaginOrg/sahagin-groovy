import java.io.File;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.sahagin.groovy.runlib.external.adapter.GroovyAdapterContainer
import org.sahagin.groovy.runlib.external.adapter.geb.GebAdapter
import org.sahagin.groovy.runlib.external.adapter.javalib.GroovyJavaLibAdapter
import org.sahagin.groovy.runlib.external.adapter.junit4.GroovyJUnit4Adapter
import org.sahagin.groovy.runlib.external.adapter.spock.SpockAdapter
import org.sahagin.groovy.runlib.srctreegen.GroovySrcTreeGenerator
import org.sahagin.groovy.share.GroovyConfig
import org.sahagin.runlib.external.adapter.Adapter;
import org.sahagin.runlib.external.adapter.AdapterContainer;
import org.sahagin.share.AcceptableLocales;
import org.sahagin.share.CommonPath;
import org.sahagin.share.Logging;
import org.sahagin.share.SrcTreeChecker
import org.sahagin.share.SysMessages;
import org.sahagin.share.srctree.SrcTree;
import org.sahagin.share.yaml.YamlConvertException;
import org.sahagin.share.yaml.YamlUtils;

class GroovyHookMethodDef {
    private static final String MSG_TEST_FRAMEWORK_NOT_FOUND = "testFramework not found: %s";
    private static Logger logger = Logging.getLogger(GroovyHookMethodDef.class.getName());
    private static boolean initialized = false;
    private static SrcTree srcTree;
    
    static void initialize() {
        if (initialized) {
            return;
        }
        logger.info("initialize");
        
        String configFilePath;
        String propValue = System.getProperty("sahagin.configPath")
        if (propValue == null || propValue == "") {
            configFilePath = "sahagin.yml";
        } else {
            configFilePath = propValue;
        }
        GroovyConfig config = GroovyConfig.generateFromYamlConfig(new File(configFilePath));
        Logging.setLoggerEnabled(config.isOutputLog());   
        AcceptableLocales locales = AcceptableLocales.getInstance(config.getUserLocale());
        GroovyAdapterContainer.globalInitialize(locales, config.getTestFramework());
        SysMessages.globalInitialize(locales);

        // default adapters
        new GroovyJUnit4Adapter().initialSetAdapter();
        new SpockAdapter().initialSetAdapter();
        new GroovyJavaLibAdapter().initialSetAdapter();
        new GebAdapter().initialSetAdapter();        

        for (String adapterClassName : config.getAdapterClassNames()) {
            // TODO handle exception thrown by forName or newInstance method
            // more appropriately
            Class<?> adapterClass = Class.forName(adapterClassName);
            assert adapterClass != null;
            Object adapterObj = adapterClass.newInstance();
            assert adapterObj != null;
            assert adapterObj instanceof Adapter;
            Adapter adapter = (Adapter) adapterObj;
            adapter.initialSetAdapter();
        }

        if (!GroovyAdapterContainer.globalInstance().isRootMethodAdapterSet()) {
            throw new RuntimeException(String.format(
                    MSG_TEST_FRAMEWORK_NOT_FOUND, config.getTestFramework()));
        }

        // delete previous data
        if (config.getRootBaseReportIntermediateDataDir().exists()) {
            FileUtils.deleteDirectory(config.getRootBaseReportIntermediateDataDir());
        }

        SrcTree srcTree = generateAndDumpSrcTree(config, locales);
    }

    private static SrcTree generateAndDumpSrcTree(GroovyConfig config, AcceptableLocales locales) {
        // generate and dump srcTree
        GroovySrcTreeGenerator generator = new GroovySrcTreeGenerator(
                AdapterContainer.globalInstance().getAdditionalTestDocs(), locales);
        File srcTreeFile = CommonPath.srcTreeFile(config.getRootBaseReportIntermediateDataDir());
        SrcTree srcTree = generator.generateWithRuntimeClassPath(config.getRootBaseTestDir());
        SrcTreeChecker.check(srcTree);
        YamlUtils.dump(srcTree.toYamlObject(), srcTreeFile);
        return srcTree;
    }

}