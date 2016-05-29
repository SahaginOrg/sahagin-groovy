package org.sahagin.groovy.runlib.external.adapter.spock

import org.sahagin.runlib.external.CaptureStyle;
import org.sahagin.runlib.external.adapter.ResourceAdditionalTestDocsAdapter
import org.sahagin.share.CommonPath

class SpockAdditionalTestDocsAdapter extends ResourceAdditionalTestDocsAdapter {
    public static final String CLASS_QUALIFIED_NAME = "spockSystem";
    public static final String METHOD_IMPLICIT_ASSERT = "implicitAssert";

    @Override
    public String resourceDirPath() {
        return CommonPath.standardAdapdaterLocaleResDirPath("groovy") + "/spock";
    }

    @Override
    public void classAdd() {
    }

    @Override
    public void methodAdd() {
        // in alphabetical order
        methodAdd(CLASS_QUALIFIED_NAME, METHOD_IMPLICIT_ASSERT, null, CaptureStyle.NONE);
    }
}
