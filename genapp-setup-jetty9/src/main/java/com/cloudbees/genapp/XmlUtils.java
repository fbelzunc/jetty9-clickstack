/*
 * Copyright 2010-2013, CloudBees Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudbees.genapp;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class XmlUtils {
    private final static XPath xpath = XPathFactory.newInstance().newXPath();
    private final static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    private final static TransformerFactory transformerFactory = TransformerFactory.newInstance();

    static {
        documentBuilderFactory.setValidating(false);
        try {
            documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception disabling 'http://apache.org/xml/features/nonvalidating/load-external-dtd'", e);
        }

    }

    public static Element getUniqueElement(Document document, String xpathExpression) {
        return getUniqueElement((Node) document, xpathExpression);

    }

    public static Element getUniqueElement(Element element, String xpathExpression) {
        return getUniqueElement((Node) element, xpathExpression);
    }

    public static Element getUniqueElement(Node element, String xpathExpression) {
        try {
            NodeList nl = (NodeList) xpath.compile(xpathExpression).evaluate(element, XPathConstants.NODESET);
            if (nl.getLength() == 0 || nl.getLength() > 1) {
                throw new RuntimeException("More or less (" + nl.getLength() + ") than 1 element found for expression: " + xpathExpression);
            }
            return (Element) nl.item(0);
        } catch (Exception e) {
            throw new RuntimeException("Exception evaluating xpath '" + xpathExpression + "' on " + element, e);
        }
    }

    public static Document loadXmlDocumentFromFile(@Nonnull java.io.File file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(file);
    }

    public static Document loadXmlDocumentFromStream(@Nonnull java.io.InputStream in) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(in);
    }

    public static void checkRootElement(@Nonnull org.w3c.dom.Document document, @Nullable java.lang.String expectedRootElementName) {
        if (document.getDocumentElement() == null || expectedRootElementName == null) {
            return;
        } else if (!expectedRootElementName.equals(document.getDocumentElement().getNodeName())) {
            throw new IllegalStateException("Invalid root element '" + document.getDocumentElement().getNodeName() + "', expected '" + expectedRootElementName + "'");

        }
    }

    public static void flush(Document in, OutputStream out) throws RuntimeException {
        Map<String, String> outputProperties = new HashMap<>();

        flush(in, out, outputProperties);
    }

    public static void flush(Document in, OutputStream out, Map<String, String> outputProperties) {
        try {
            // Write the content into XML file
            Transformer transformer = transformerFactory.newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

            for (Map.Entry<String, String> property : outputProperties.entrySet()) {
                transformer.setOutputProperty(property.getKey(), property.getValue());
            }

            transformer.transform(new DOMSource(in), new StreamResult(out));
        } catch (TransformerException e) {
            throw new RuntimeException("Exception flush document", e);
        }
    }

    public static void insertSiblingAfter(Element newElement, Element sibling) {
        Node nextSibling = sibling.getNextSibling();
        sibling.getParentNode().insertBefore(newElement, nextSibling);
    }

}
