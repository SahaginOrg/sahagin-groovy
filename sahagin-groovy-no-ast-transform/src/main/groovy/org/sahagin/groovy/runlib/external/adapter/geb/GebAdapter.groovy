package org.sahagin.groovy.runlib.external.adapter.geb

import geb.Browser

import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.NoSuchSessionException
import org.sahagin.groovy.runlib.external.adapter.GroovyAdapterContainer
import org.sahagin.runlib.external.adapter.Adapter
import org.sahagin.runlib.external.adapter.AdapterContainer
import org.sahagin.runlib.external.adapter.ResourceAdditionalTestDocsAdapter
import org.sahagin.runlib.external.adapter.ScreenCaptureAdapter
import org.sahagin.share.CommonPath

class GebAdapter implements Adapter {

    @Override
    void initialSetAdapter() {
        GroovyAdapterContainer container = GroovyAdapterContainer.globalInstance()
        container.addAdditionalTestDocsAdapter(new AdditionalTestDocsAdapterImpl())
        container.addSrcTreeVisitorAdapter(new GebSrcTreeVisitorAdapter())
    }

    @Override
    String getName() {
        return "geb"
    }

    // can set null
    static void setAdapter(final Browser browser) {
        AdapterContainer container = AdapterContainer.globalInstance()
        container.screenCaptureAdapter = new ScreenCaptureAdapterImpl(browser)
    }

    private static class ScreenCaptureAdapterImpl implements ScreenCaptureAdapter {
        private Browser browser

        ScreenCaptureAdapterImpl(Browser browser) {
            this.browser = browser
        }

        @Override
        byte[] captureScreen() {
            if (browser == null) {
                return null
            }
            WebDriver driver = browser.driver
            if (driver == null) {
                return null
            }
            if (!(driver instanceof TakesScreenshot)) {
                return null
            }
            try {
                return ((TakesScreenshot) driver)
                        .getScreenshotAs(OutputType.BYTES)
            } catch (NoSuchSessionException e) {
                // just do nothing if WebDriver instance is in invalid state
                return null
            }
        }

    }

    private static class AdditionalTestDocsAdapterImpl extends
    ResourceAdditionalTestDocsAdapter {

        @Override
        String resourceDirPath() {
            return CommonPath.standardAdapdaterLocaleResDirPath("groovy") + "/geb"
        }

        @Override
        void classAdd() {
            // delegate setting
            classAdd("geb.junit3.GebTest", "geb.Browser")
            classAdd("geb.junit4.GebTest", "geb.Browser")
            classAdd("geb.spock.GebSpec", "geb.Browser")
            classAdd("geb.testng.GebTest", "geb.Browser")
        }

        @Override
        void methodAdd() {
            // in alphabetical order
            methodAdd("geb.Browser", "at")
            methodAdd("geb.Browser", "go", "String")
            methodAdd("geb.Browser", "to", "java.lang.Class,Object[]", 1)
            methodAdd("geb.navigator.Navigator", "click")
            methodAdd("geb.navigator.Navigator", "text")
            methodAdd("geb.navigator.Navigator", "value", "Object")
            methodAdd("geb.navigator.Navigator", "value", "void")
            methodAdd("geb.Page", '$', "java.util.Map")
            methodAdd("geb.Page", '$', "String")
        }
    }
}
