module com.udacity.catpoint.securitymodule {
    requires com.udacity.catpoint.imagemodule;
    requires com.google.gson;
    requires com.google.common;
    requires java.prefs;
    requires com.miglayout.swing;

    opens com.udacity.catpoint.securitymodule.data;
    opens com.udacity.catpoint.securitymodule.service;
}