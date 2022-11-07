module com.udacity.catpoint.imagemodule {
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.services.rekognition;
    requires transitive java.desktop;
    requires transitive org.slf4j;
    exports com.udacity.catpoint.imagemodule.service;
}