package org.sahagin.groovy.runlib.external.adapter.geb

import geb.Browser

import org.fluentlenium.core.Fluent
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.SessionNotFoundException
import org.sahagin.runlib.external.CaptureStyle
import org.sahagin.runlib.external.adapter.Adapter
import org.sahagin.runlib.external.adapter.AdapterContainer
import org.sahagin.runlib.external.adapter.ResourceAdditionalTestDocsAdapter
import org.sahagin.runlib.external.adapter.ScreenCaptureAdapter
import org.sahagin.share.CommonPath

public class GebAdapter implements Adapter {

    @Override
    void initialSetAdapter() {
        AdapterContainer container = AdapterContainer.globalInstance()
        container.addAdditionalTestDocsAdapter(new AdditionalTestDocsAdapterImpl())
    }

    @Override
    String getName() {
        return "geb"
    }

    // can set null
    public static void setAdapter(final Browser browser) {
        AdapterContainer container = AdapterContainer.globalInstance()
        container.setScreenCaptureAdapter(new ScreenCaptureAdapterImpl(browser))
    }

    private static class ScreenCaptureAdapterImpl implements ScreenCaptureAdapter {
        private Browser browser

        public ScreenCaptureAdapterImpl(Browser browser) {
            this.browser = browser
        }

        @Override
        public byte[] captureScreen() {
            if (browser == null) {
                return null
            }
            WebDriver driver = browser.getDriver()
            if (driver == null) {
                return null
            }
            if (!(driver instanceof TakesScreenshot)) {
                return null
            }
            try {
                return ((TakesScreenshot) driver)
                        .getScreenshotAs(OutputType.BYTES)
            } catch (SessionNotFoundException e) {
                // just do nothing if WebDriver instance is in invalid state
                return null
            }
        }

    }

    private static class AdditionalTestDocsAdapterImpl extends
    ResourceAdditionalTestDocsAdapter {

        @Override
        public String resourceDirPath() {
            return CommonPath.standardAdapdaterLocaleResDirPath("groovy") + "/geb"
        }

        @Override
        public void classAdd() {
            // delegate setting
            classAdd("geb.junit3.GebTest", "geb.Browser")
            classAdd("geb.junit4.GebTest", "geb.Browser")
            classAdd("geb.testnggm.GebTest", "geb.Browser")
        }

        @Override
        public void methodAdd() {
            // in alphabetical order
            methodAdd("geb.Browser", "go", "String")
        }

    }

}
