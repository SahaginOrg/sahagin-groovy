package org.sahagin.groovy.runlib.external.adapter.webdriver

import org.sahagin.groovy.runlib.external.adapter.GroovyAdapterContainer
import org.sahagin.runlib.external.adapter.Adapter;
import org.sahagin.runlib.external.adapter.webdriver.WebDriverAdditionalTestDocsAdapter

class GroovyWebDriverAdapter implements Adapter {

    @Override
    public void initialSetAdapter() {
        GroovyAdapterContainer container = GroovyAdapterContainer.globalInstance()
        container.addAdditionalTestDocsAdapter(new WebDriverAdditionalTestDocsAdapter())
    }

    @Override
    public String getName() {
        return "webDriver"
    }

}