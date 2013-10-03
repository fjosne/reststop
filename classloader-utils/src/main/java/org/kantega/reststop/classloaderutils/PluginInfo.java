package org.kantega.reststop.classloaderutils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.*;

import static java.util.Arrays.asList;

/**
 *
 */
public class PluginInfo extends Artifact {

    private Map<String, List<Artifact>> classpaths = new HashMap<>();
    private File sourceDirectory;
    private boolean directDeploy;
    private List<Artifact> dependsOn = new ArrayList<>();
    private Properties config = new Properties();

    public List<Artifact> getClassPath(String scope) {
        if (!classpaths.containsKey(scope)) {
            classpaths.put(scope, new ArrayList<Artifact>());
        }

        return classpaths.get(scope);
    }

    public List<File> getClassPathFiles(String scope) {
        List<File> files = new ArrayList<>();
        for (Artifact artifact : getClassPath(scope)) {
            files.add(artifact.getFile());
        }
        return files;
    }

    public static List<PluginInfo> parse(Document document) {
        List<PluginInfo> infos = new ArrayList<>();

        NodeList pluginElements = document.getDocumentElement().getElementsByTagName("plugin");

        for (int i = 0; i < pluginElements.getLength(); i++) {

            PluginInfo pluginInfo = new PluginInfo();

            infos.add(pluginInfo);

            Element pluginElement = (Element) pluginElements.item(i);

            parseGav(pluginInfo, pluginElement);

            pluginInfo.setDirectDeploy(!"false".equals(pluginElement.getAttribute("directDeploy")));
            String pluginFile = pluginElement.getAttribute("pluginFile");
            if(!pluginFile.isEmpty()) {
                File pluginJar = new File(pluginFile);
                pluginInfo.setFile(pluginJar);
            }
            String sourceDir = pluginElement.getAttribute("sourceDirectory");
            if(sourceDir != null && !sourceDir.trim().isEmpty()) {
                pluginInfo.setSourceDirectory(new File(sourceDir));
            }

            NodeList dependsOnElems = pluginElement.getElementsByTagName("depends-on");
            for(int d = 0; d < dependsOnElems.getLength(); d++) {
                Element depElem = (Element) dependsOnElems.item(d);
                Artifact depArt = new Artifact();
                parseGav(depArt, depElem);
                pluginInfo.addDependsOn(depArt);
            }

            NodeList configElems = pluginElement.getElementsByTagName("config");

            Properties props = new Properties();
            for(int c = 0; c < configElems.getLength(); c++) {
                Element configElem = (Element) configElems.item(c);

                NodeList propElems = configElem.getElementsByTagName("prop");
                for(int p = 0; p < configElems.getLength(); p++) {
                    Element propElem = (Element) propElems.item(p);

                    props.setProperty(propElem.getAttribute("name"), propElem.getAttribute("value"));
                }

            }

            pluginInfo.setConfig(props);

            for (String scope : asList("test", "runtime", "compile")) {

                NodeList classPathElems = pluginElement.getElementsByTagName(scope);
                if(classPathElems.getLength() > 0) {
                    Element runtimeElement = (Element) classPathElems.item(0);


                    NodeList artifacts = runtimeElement.getElementsByTagName("artifact");


                    for (int a = 0; a < artifacts.getLength(); a++) {
                        Element artifactElement = (Element) artifacts.item(a);
                        String filePath = artifactElement.getAttribute("file");
                        File file = null;
                        if(filePath != null) {
                            file = new File(filePath);
                        }
                        Artifact artifact = new Artifact(artifactElement.getAttribute("groupId"),
                                artifactElement.getAttribute("artifactId"),
                                artifactElement.getAttribute("version"), file);

                        pluginInfo.getClassPath(scope).add(artifact);

                    }
                }

            }
        }
        return infos;
    }

    public void addDependsOn(Artifact depArt) {
        dependsOn.add(depArt);
    }

    private static void parseGav(Artifact pluginInfo, Element pluginElement) {
        pluginInfo.setGroupId(pluginElement.getAttribute("groupId"));
        pluginInfo.setArtifactId(pluginElement.getAttribute("artifactId"));
        pluginInfo.setVersion(pluginElement.getAttribute("version"));
    }

    public List<PluginInfo> getParents(Collection<PluginInfo> all) {
        List<PluginInfo> deps = new ArrayList<>();

        for (Artifact compile : getClassPath("compile")) {
            for (PluginInfo info : all) {
                if(compile.getGroupIdAndArtifactId().equals(info.getGroupIdAndArtifactId())) {
                    deps.add(info);
                }
            }
        }

        return deps;
    }

    public List<PluginInfo> getChildren(Collection<PluginInfo> all) {
        List<PluginInfo> deps = new ArrayList<>();

            for (PluginInfo info : all) {
                for (Artifact compile : info.getClassPath("compile")) {
                    if(compile.getGroupIdAndArtifactId().equals(getGroupIdAndArtifactId())) {
                        deps.add(info);
                    }
                }
            }


        return deps;
    }

    @Override
    public String toString() {
        return "Plugin " + getGroupId() +":" + getArtifactId() +":" + getVersion();
    }

    public void setSourceDirectory(File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public File getSourceDirectory() {
        return sourceDirectory;
    }

    public boolean isDevelopmentPlugin() {
        return "org.kantega.reststop".equals(getGroupId()) && "reststop-development-plugin".equals(getArtifactId());
    }

    public String getPluginId() {
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
    }

    public void setDirectDeploy(boolean directDeploy) {
        this.directDeploy = directDeploy;
    }

    public boolean isDirectDeploy() {
        return directDeploy;
    }


    public static List<PluginInfo> sortByRuntimeDependencies(List<PluginInfo> infos) {
        Map<String, Boolean> colors = new HashMap<String, Boolean>();
        List<PluginInfo> sorted = new LinkedList<PluginInfo>();

        Map<String, PluginInfo> plugins = new HashMap<>();
        for (PluginInfo info : infos) {
            plugins.put(info.getGroupIdAndArtifactId(), info);
        }

        for (PluginInfo info : infos) {
            if (!colors.containsKey(info.getGroupIdAndArtifactId()))
                dfs(info, plugins, colors, sorted);
        }
        return sorted;
    }

    private static void dfs(PluginInfo info, Map<String, PluginInfo> plugins, Map<String, Boolean> colors, List<PluginInfo> sorted) {
        colors.put(info.getGroupIdAndArtifactId(), Boolean.FALSE);
        for (Artifact dep : info.getDependsOn()) {
            String key = dep.getGroupIdAndArtifactId();
            if (plugins.containsKey(key) && !colors.containsKey(key)) {
                dfs(plugins.get(key), plugins, colors, sorted);
            }
        }
        colors.put(info.getGroupIdAndArtifactId(), Boolean.TRUE);
        sorted.add(info);
    }

    public String getGroupIdAndArtifactId() {
        return getGroupId() +":" + getArtifactId();
    }

    public List<Artifact> getDependsOn() {
        return dependsOn;
    }

    public void setConfig(Properties config) {
        this.config = cloneProperties(config);
    }

    public Properties getConfig() {
        return cloneProperties(config);
    }

    private static Properties cloneProperties(Properties props) {
        Properties properties = new Properties();
        if(props != null) {
            properties.putAll(props);
        }
        return properties;
    }
}
