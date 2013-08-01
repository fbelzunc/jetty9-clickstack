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
package com.cloudbees.genapp.jetty9;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.cloudbees.genapp.metadata.ConfigurationBuilder;
import com.cloudbees.genapp.resource.SessionStore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cloudbees.genapp.metadata.Metadata;
import com.cloudbees.genapp.resource.Resource;
import com.cloudbees.genapp.resource.Database;

import java.util.logging.Logger;

import com.cloudbees.genapp.resource.Email;

public class JettyAppXmlBuilder implements ConfigurationBuilder {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private Document contextDocument;
    private Metadata metadata;

    public JettyAppXmlBuilder() {
    }

    private JettyAppXmlBuilder(Metadata metadata) {
        this.metadata = metadata;
    }

    public JettyAppXmlBuilder create(Metadata metadata) {
        return new JettyAppXmlBuilder(metadata);
    }

    private JettyAppXmlBuilder addResources(Metadata metadata) {
        for (Resource resource : metadata.getResources().values()) {
            if (resource instanceof Database) {
                addDatabase((Database) resource);
            } else if (resource instanceof Email) {
                addEmail((Email) resource);
            } else if (resource instanceof SessionStore) {
                addSessionStore((SessionStore) resource);
            }

        }
        return this;
    }

    /**
     * See <a href="http://wiki.eclipse.org/Jetty/Howto/Configure_JNDI_Datasource">Jetty/Howto/Configure JNDI Datasource</a>
     *
     * @param database
     * @return
     */
    private JettyAppXmlBuilder addDatabase(Database database) {

        //Add database Jetty 9
        logger.info("Insert DataSource " + database.getName());

        // Jetty <jdbc-connection-pool>
        Element dataSource = contextDocument.createElement("New");
        dataSource.setAttribute("id", database.getName());
        dataSource.setAttribute("class", "org.eclipse.jetty.plus.jndi.Resource");

        Element jndiName = contextDocument.createElement("Arg");
        jndiName.setTextContent("jdbc/" + database.getName());
        dataSource.appendChild(jndiName);

        Element objectToBind = contextDocument.createElement("Arg");
        dataSource.appendChild(objectToBind);


        Element dataSourceInstance = contextDocument.createElement("New");
        dataSourceInstance.setAttribute("class", "org.apache.commons.dbcp.BasicDataSource");
        objectToBind.appendChild(dataSourceInstance);

        dataSourceInstance.appendChild(createJettyConfigSetDirective("driverClassName", database.getJavaDriver()));
        dataSourceInstance.appendChild(createJettyConfigSetDirective("url", "jdbc:" + database.getUrl()));
        dataSourceInstance.appendChild(createJettyConfigSetDirective("username", database.getUsername()));
        dataSourceInstance.appendChild(createJettyConfigSetDirective("password", database.getPassword()));

        contextDocument.getDocumentElement().appendChild(dataSource);

        return this;
    }

    private Element createJettyConfigSetDirective(String name, String value) {
        Element setElement = contextDocument.createElement("Set");
        setElement.setAttribute("name", name);
        setElement.setTextContent(value);
        return setElement;
    }

    private JettyAppXmlBuilder addEmail(Email email) {
        logger.warning("email is not yet supported, ignore it");
        return this;
    }

    private JettyAppXmlBuilder addSessionStore(SessionStore store) {
        logger.warning("session store is not yet supported, ignore it");
        return this;
    }

    private JettyAppXmlBuilder fromExistingDocument(Document contextDocument) {
        String rootElementName = contextDocument.getDocumentElement().getNodeName();
        if (!rootElementName.equals("Configure"))
            throw new IllegalArgumentException("Document is missing root <Context> element");
        this.contextDocument = contextDocument;
        return this;
    }

    private JettyAppXmlBuilder fromExistingDocument(File file) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(file);
        fromExistingDocument(document);
        return this;
    }

    private Document buildContextDocument() throws ParserConfigurationException {
        if (contextDocument == null) {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            contextDocument = documentBuilder.newDocument();
            Element rootContextDocumentElement = contextDocument.createElement("Context");
            contextDocument.appendChild(rootContextDocumentElement);
        }
        addResources(metadata);
        return contextDocument;
    }

    @Override
    public void writeConfiguration(Metadata metadata, File configurationFile) throws Exception {
        Document contextXml = this.create(metadata).fromExistingDocument(configurationFile).buildContextDocument();

        // Write the content into XML file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//Mort Bay Consulting//DTD Configure//EN");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://jetty.mortbay.org/configure.dtd");

        transformer.transform(new DOMSource(contextXml), new StreamResult(configurationFile));
    }
}
