package com.kumuluz.ee.common.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class KumuluzProject {

    private String name;

    private String description;

    private String groupId;

    private String artifactId;

    private String version;

    private String kumuluzVersion;

    private boolean module;

    private String parentArtifactId;

    private List<KumuluzProject> modules;

    private String packageName;

    private HashMap<String, String> directoryPaths;

    private String executableName;

    private String port;

    private boolean windowsOS;

    private String moduleVersion = "${project.version}";

    public KumuluzProject(){
        this.modules = new LinkedList<>();
        this.directoryPaths = new HashMap<>();
    }

    public KumuluzProject newModule(String artifactId){
        KumuluzProject module = new KumuluzProject();
        module.setModule(true);
        module.setArtifactId(artifactId);
        module.setGroupId(this.groupId);
        module.setKumuluzVersion(this.kumuluzVersion);
        module.setVersion(this.version);
        module.setParentArtifactId(this.artifactId);
        module.setPort(this.port);

        this.modules.add(module);

        return module;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getKumuluzVersion() {
        return kumuluzVersion;
    }

    public void setKumuluzVersion(String kumuluzVersion) {
        this.kumuluzVersion = kumuluzVersion;
    }

    public boolean isModule() {
        return module;
    }

    public void setModule(boolean module) {
        this.module = module;
    }

    public String getParentArtifactId() {
        return parentArtifactId;
    }

    public void setParentArtifactId(String parentArtifactId) {
        this.parentArtifactId = parentArtifactId;
    }

    public List<KumuluzProject> getModules() {
        return modules;
    }

    public void setModules(List<KumuluzProject> modules) {
        this.modules = modules;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public HashMap<String, String> getDirectoryPaths() {
        return directoryPaths;
    }

    public void setDirectoryPaths(HashMap<String, String> directoryPaths) {
        this.directoryPaths = directoryPaths;
    }

    public String getExecutableName() {
        return executableName;
    }

    public void setExecutableName(String executableName) {
        this.executableName = executableName;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public boolean isWindowsOS() {
        return windowsOS;
    }

    public void setWindowsOS(boolean windowsOS) {
        this.windowsOS = windowsOS;
    }

    public String getModuleVersion() {
        return moduleVersion;
    }

    public void setModuleVersion(String moduleVersion) {
        this.moduleVersion = moduleVersion;
    }
}
