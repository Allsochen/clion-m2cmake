
package com.github.allsochen.m2cmake.build;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;

public class AutomaticReloadCMakeBuilder {
    public AutomaticReloadCMakeBuilder() {
    }

    public static void build(String basePath) throws ParserConfigurationException,
            IOException, SAXException, TransformerException {
        String fileName = basePath + File.separator + ".idea/workspace.xml";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(fileName);
        NodeList nodeList = document.getElementsByTagName("component");

        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node componentNode = nodeList.item(i);
            if (componentNode.getNodeType() == 1) {
                Element componentElement = (Element) componentNode;
                if (componentElement.hasAttribute("name") &&
                        "CMakeSettings".equals(componentElement.getAttribute("name"))) {
                    componentElement.setAttribute("AUTO_RELOAD", "true");
                    break;
                }
            }
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(new File(fileName));
        transformer.transform(source, result);
    }
}
