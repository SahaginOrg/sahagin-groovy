package org.sahagin.groovy.share

import java.io.File
import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Map

import org.sahagin.share.yaml.YamlConvertException
import org.sahagin.share.yaml.YamlUtils
import org.sahagin.share.Config

class GroovyConfig extends Config {
    private static final String INVALID_CONFIG_YAML = "failed to load config file \"%s\": %s"
    private static final String TEST_FRAMEWORK_DEFAULT = "spock"

    private File testDir
    private List<String> adapterClassNames = new ArrayList<>(8)
    private String testFramework = TEST_FRAMEWORK_DEFAULT

    public static GroovyConfig generateFromYamlConfig(File yamlConfigFile) throws YamlConvertException {
        Map<String, Object> configYamlObj = YamlUtils.load(yamlConfigFile)
        // use the parent directory of yamlConfigFile as the root directory
        GroovyConfig config = new GroovyConfig(yamlConfigFile.parentFile)
        try {
            config.fromYamlObject(configYamlObj)
        } catch (YamlConvertException e) {
            throw new YamlConvertException(String.format(
            INVALID_CONFIG_YAML, yamlConfigFile.absolutePath, e.localizedMessage), e)
        }
        return config
    }

    public GroovyConfig(File rootDir) {
        super(rootDir)
    }

    public File getRootBaseTestDir() {
        if (testDir.isAbsolute()) {
            return testDir
        } else {
            // avoid ambiguous argument method call for null
            return new File(getRootDir() as File, testDir.path)
        }
    }

    public void setTestDir(File testDir) {
        this.testDir = testDir
    }

    public List<String> getAdapterClassNames() {
        return adapterClassNames
    }

    public void addAdapterClassName(String adapterClassName) {
        adapterClassNames.add(adapterClassName)
    }

    public String getTestFramework() {
        return testFramework
    }

    public void setTestFramework(String testFramework) {
        this.testFramework = testFramework
    }

    @Override
    public Map<String, Object> toYamlObject() {
        Map<String, Object> result = super.toYamlObject()
        Map<String, Object> groovyConf = new HashMap<>(4)
        groovyConf.put("testDir", testDir.path)
        groovyConf.put("adapters", adapterClassNames)
        groovyConf.put("testFramework", testFramework)
        result.put("groovy", groovyConf)
        return result
    }

    @Override
    public void fromYamlObject(Map<String, Object> yamlObject)
            throws YamlConvertException {
        super.fromYamlObject(yamlObject)
        Map<String, Object> groovyYamlObj = YamlUtils.getYamlObjectValue(yamlObject, "groovy")
        // testDir for groovy is mandatory
        // (since cannot get source code path on run time)
        // TODO support array testDir value (so, testDir can be string or string array)
        testDir = new File(YamlUtils.getStrValue(groovyYamlObj, "testDir"))
        adapterClassNames = YamlUtils.getStrListValue(groovyYamlObj, "adapters", true)

        String testFrameworkValue = YamlUtils.getStrValue(groovyYamlObj, "testFramework", true)
        if (testFrameworkValue == null) {
            testFrameworkValue = TEST_FRAMEWORK_DEFAULT
        } else {
            testFramework = testFrameworkValue
        }
    }


}