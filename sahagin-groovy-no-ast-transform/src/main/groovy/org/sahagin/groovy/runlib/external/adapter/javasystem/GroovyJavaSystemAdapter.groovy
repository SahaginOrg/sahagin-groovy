package org.sahagin.groovy.runlib.external.adapter.javasystem

import org.sahagin.groovy.runlib.external.adapter.GroovyAdapterContainer
import org.sahagin.runlib.external.adapter.Adapter;
import org.sahagin.runlib.external.adapter.javasystem.JavaSystemAdditionalTestDocsAdapter

class GroovyJavaSystemAdapter implements Adapter {
    
        @Override
        public void initialSetAdapter() {
            GroovyAdapterContainer container = GroovyAdapterContainer.globalInstance()
            container.addAdditionalTestDocsAdapter(new JavaSystemAdditionalTestDocsAdapter())
        }
    
        @Override
        public String getName() {
            return "javaSystem"
        }
    
    }