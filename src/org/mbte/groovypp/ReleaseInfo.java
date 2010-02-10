package org.mbte.groovypp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

public class ReleaseInfo {
    private static Properties releaseInfo = new Properties();
    private static final String RELEASE_INFO_FILE = "META-INF/groovypp-release-info.properties";
    private static final String KEY_IMPLEMENTATION_VERSION = "ImplementationVersion";
    private static final String KEY_BUILD_DATE = "BuildDate";
    private static final String KEY_BUILD_TIME = "BuildTime";
    private static boolean loaded = false;

    public static String getVersion() {
        return get(KEY_IMPLEMENTATION_VERSION);
    }

    public static Properties getAllProperties() {
        loadInfo();
        return releaseInfo;
    }

    private static String get(String propName) {
        loadInfo();
        String propValue = releaseInfo.getProperty(propName);
        return (propValue == null ? "" : propValue);
    }

    private static void loadInfo() {
        if(!loaded) {
            URL url = null;
            ClassLoader cl = ReleaseInfo.class.getClassLoader();
            if(cl instanceof URLClassLoader) {
                // this avoids going through the parent classloaders/bootstarp
                url = ((URLClassLoader) cl).findResource(RELEASE_INFO_FILE);
            } else {
                // fallback option as ClassLoader#findResource() is protected
                url = cl.getResource(RELEASE_INFO_FILE);
            }
            try {
                InputStream is = url.openStream();
                if(is != null) {
                    releaseInfo.load(is);
                }
            } catch(IOException ioex) {
                // ignore. In case of some exception, release info is not available
            }
            loaded = true;
        }
    }
}
