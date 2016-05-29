package org.sahagin.groovy.runlib.external.adapter.javalib

import org.sahagin.groovy.runlib.external.adapter.GroovyAdapterContainer
import org.sahagin.runlib.external.adapter.Adapter
import org.sahagin.runlib.external.adapter.javalib.JavaLibAdditionalTestDocsAdapter

class GroovyJavaLibAdapter implements Adapter {

    @Override
    public void initialSetAdapter() {
        GroovyAdapterContainer container = GroovyAdapterContainer.globalInstance()
        container.addAdditionalTestDocsAdapter(new JavaLibAdditionalTestDocsAdapter())
    }

    @Override
    public String getName() {
        return "javaLib"
    }
}