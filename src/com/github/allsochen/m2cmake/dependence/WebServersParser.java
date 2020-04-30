package com.github.allsochen.m2cmake.dependence;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WebServersParser {

    public static Document read(String basePath, String fileName) {
        String filePath = basePath + File.separator + ".idea" + File.separator + fileName;
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        Document document = null;
        try {
            //DOM parser instance
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            //parse an XML file into a DOM tree
            document = builder.parse(new File(filePath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return document;
    }

    private static Element findFileTransferNode(NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            if (element.getTagName().equals("fileTransfer")) {
                return element;
            }
        }
        return null;
    }

    private static Element findPaths(NodeList nodeList, String name) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            if (element.getTagName().equals("paths") && element.getAttribute("name").equals(name)) {
                return element;
            }
        }
        return null;
    }

    public static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().startsWith("win");
    }

    public static List<String> parse(String basePath) {
        List<String> paths = new ArrayList<>();
        Document webServersDoc = read(basePath, "webServers.xml");
        if (webServersDoc == null) {
            return paths;
        }
        Document deploymentDoc = read(basePath, "deployment.xml");
        if (deploymentDoc == null) {
            return paths;
        }
//        <webServer id="42ce528b-fb42-4032-b0d8-94de2fe355ec" name="NewsRecommendationServer" url="http://localhost">
//            <fileTransfer port="0" mountedRoot="X:\allsochen\projects\MTT" accessType="MOUNT" />
//        </webServer>
        NodeList webServers = webServersDoc.getDocumentElement().getElementsByTagName("webServer");
        NodeList pathsNode = deploymentDoc.getDocumentElement().getElementsByTagName("paths");
        for (int i = 0; i < webServers.getLength(); i++) {
            Element webServer = (Element) webServers.item(i);
            String name = webServer.getAttribute("name");
            Element fileTransfer = findFileTransferNode(webServer.getElementsByTagName("fileTransfer"));
            if (name != null && fileTransfer != null) {
                Element pathsElement = findPaths(pathsNode, name);
                if (pathsElement == null) {
                    continue;
                }
                NodeList mappings = pathsElement.getElementsByTagName("mapping");
                for (int j = 0; j < mappings.getLength(); j++) {
                    Element mappingElement = (Element) mappings.item(j);
                    String deploy = mappingElement.getAttribute("deploy").replaceAll("/", "");
                    paths.add(fileTransfer.getAttribute("mountedRoot") + File.separator + deploy);
                }
            }
        }
        return paths;
    }

    public static void main(String[] args) {
        String a = "/aaa\\ff";
        System.out.println(a.replaceAll("/", "\\\\"));
        System.out.println(a.replaceAll("/", File.separator));
        System.out.println(a.replaceAll("\\\\", "/"));
    }
}
