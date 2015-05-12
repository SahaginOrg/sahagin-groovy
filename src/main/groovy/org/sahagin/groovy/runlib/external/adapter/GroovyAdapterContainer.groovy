package org.sahagin.groovy.runlib.external.adapter

import org.codehaus.groovy.ast.MethodNode
import org.sahagin.runlib.external.adapter.JavaAdapterContainer
import org.sahagin.runlib.external.adapter.JavaRootMethodAdapter
import org.sahagin.share.AcceptableLocales
import org.sahagin.runlib.external.adapter.AdapterContainer

class GroovyAdapterContainer extends AdapterContainer {
    private String acceptableTestFramework = null
    private GroovyRootMethodAdapter rootMethodAdapter = null
    private List<SrcTreeVisitorAdapter> srcTreeVisitorAdapters = new ArrayList<SrcTreeVisitorAdapter>(4)

    // make constructor private
    private GroovyAdapterContainer(AcceptableLocales locales, String acceptableTestFramework) {
        super(locales)
        if (acceptableTestFramework == null) {
            throw new NullPointerException()
        }
        this.acceptableTestFramework = acceptableTestFramework
    }

    // some method call of this class requires initialization before calling the method
    public static void globalInitialize(AcceptableLocales locales, String acceptableTestFramework) {
        globalInstance = new GroovyAdapterContainer(locales, acceptableTestFramework)
    }

    public static GroovyAdapterContainer globalInstance() {
        if (globalInstance == null) {
            throw new IllegalStateException("globalInitialize is not called yet")
        }
        if (!(globalInstance instanceof GroovyAdapterContainer)) {
            throw new IllegalStateException(
                    "global instance is not GroovyAdapterContainer: " + globalInstance)
        }
        return globalInstance as GroovyAdapterContainer
    }

    // TODO throw error if called　from other method than initialSetAdapter
    public void setRootMethodAdapter(GroovyRootMethodAdapter rootMethodAdapter) {
        if (rootMethodAdapter == null) {
            throw new NullPointerException()
        }
        if (acceptableTestFramework == null) {
            throw new IllegalStateException("acceptableTestFramework is not set")
        }
        if (acceptableTestFramework.equals(rootMethodAdapter.getName())) {
            this.rootMethodAdapter = rootMethodAdapter
        }
    }

    public boolean isRootMethodAdapterSet() {
        return this.rootMethodAdapter != null
    }

    public boolean isRootMethod(MethodNode node) {
        return rootMethodAdapter.isRootMethod(node)
    }

    public void addSrcTreeVisitorAdapter(SrcTreeVisitorAdapter srcTreeVisitorAdapter) {
        if (srcTreeVisitorAdapter == null) {
            throw new NullPointerException()
        }
        srcTreeVisitorAdapters.add(srcTreeVisitorAdapter)
    }

    public List<SrcTreeVisitorAdapter> getSrcTreeVisitorAdapters() {
        return srcTreeVisitorAdapters
    }

}