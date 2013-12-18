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
package com.cloudbees.clickstack.jetty;

import com.cloudbees.clickstack.domain.metadata.*;
import com.cloudbees.clickstack.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class SetupJettyConfigurationFiles {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private Metadata metadata;
    private Set<String> databaseProperties = new HashSet(Arrays.asList("minIdle", "maxIdle", "maxActive", "maxWait",
            "initialSize",
            "validationQuery", "validationQueryTimeout", "testOnBorrow", "testOnReturn",
            "timeBetweenEvictionRunsMillis", "numTestsPerEvictionRun", "minEvictableIdleTimeMillis", "testWhileIdle",
            "removeAbandoned", "removeAbandonedTimeout", "logAbandoned", "defaultAutoCommit", "defaultReadOnly",
            "defaultTransactionIsolation", "poolPreparedStatements", "maxOpenPreparedStatements", "defaultCatalog",
            "connectionInitSqls", "connectionProperties", "accessToUnderlyingConnectionAllowed",
            "type", "validatorClassName", "initSQL", "jdbcInterceptors", "validationInterval", "jmxEnabled",
            "fairQueue", "abandonWhenPercentageFull", "maxAge", "useEquals", "suspectTimeout", "rollbackOnReturn",
            "commitOnReturn", "alternateUsernameAllowed", "useDisposableConnectionFacade", "logValidationErrors",
            "propagateInterruptState"));

    public SetupJettyConfigurationFiles(@Nonnull Metadata metadata) {
        this.metadata = metadata;
    }

    protected void addXForwardedForSupport(Document jettyXmlDocument) {
        Element httpConfig = XmlUtils.getUniqueElement(jettyXmlDocument, "/Configure/New[@id='httpConfig']");

        Element callElement = jettyXmlDocument.createElement("Call");
        callElement.setAttribute("name", "addCustomizer");
        httpConfig.appendChild(callElement);

        Element argElement = jettyXmlDocument.createElement("Arg");
        callElement.appendChild(argElement);

        Element newElement = jettyXmlDocument.createElement("New");
        newElement.setAttribute("class", "org.eclipse.jetty.server.ForwardedRequestCustomizer");
        argElement.appendChild(newElement);
    }

    /**
     * See <a href="http://wiki.eclipse.org/Jetty/Howto/Configure_JNDI_Datasource">Jetty/Howto/Configure JNDI Datasource</a>
     */
    protected SetupJettyConfigurationFiles addDatabase(Database database, Document contextDocument) {
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
        dataSourceInstance.setAttribute("class", "org.apache.tomcat.jdbc.pool.DataSource");
        objectToBind.appendChild(dataSourceInstance);

        Map<String, String> params = new TreeMap<>();
        params.put("driverClassName", database.getJavaDriver());
        params.put("url", "jdbc:" + database.getUrl());
        params.put("username", database.getUsername());
        params.put("password", database.getPassword());

        // by default max to 20 connections which is the limit of CloudBees MySQL databases
        params.put("maxActive", "20");
        params.put("maxIdle", "10");
        params.put("minIdle", "1");

        // test on borrow and while idle to release idle connections
        params.put("testOnBorrow", "true");
        params.put("testWhileIdle", "true");
        params.put("validationQuery", database.getValidationQuery());
        params.put("validationInterval", "5000"); // 5 secs

        for (Map.Entry<String, String> param : params.entrySet()) {
            dataSourceInstance.appendChild(createJettyConfigSetDirective(param.getKey(), param.getValue(), contextDocument));
        }

        contextDocument.getDocumentElement().appendChild(dataSource);
        return this;
    }

    private Element createJettyConfigSetDirective(String name, String value, Document contextDocument) {
        Element setElement = contextDocument.createElement("Set");
        setElement.setAttribute("name", name);
        setElement.setTextContent(value);
        return setElement;
    }

    protected SetupJettyConfigurationFiles addEmail(Email email, Document appXmlDocument) {
        logger.warning("Ignore addEmail(" + email + ")");
        return this;
    }

    protected SetupJettyConfigurationFiles addSessionStore(SessionStore store, Document appXmlDocument) {
        logger.warning("Ignore addSessionStore(" + store + ")");

        return this;
    }

    protected SetupJettyConfigurationFiles addPrivateAppValve(Metadata metadata, Document appXmlDocument) {
        logger.warning("Ignore addPrivateAppValve(" + metadata + ")");

        return this;
    }

    protected void buildJettyConfiguration(Metadata metadata, Document jettyXmlDocument, Document appXmlDocument) throws ParserConfigurationException {

        String message = "File generated by jetty9-clickstack at " + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date());

        appXmlDocument.appendChild(appXmlDocument.createComment(message));
        jettyXmlDocument.appendChild(jettyXmlDocument.createComment(message));

        for (Resource resource : metadata.getResources().values()) {
            if (resource instanceof Database) {
                addDatabase((Database) resource, appXmlDocument);
            } else if (resource instanceof Email) {
                addEmail((Email) resource, appXmlDocument);
            } else if (resource instanceof SessionStore) {
                addSessionStore((SessionStore) resource, appXmlDocument);
            }
        }
        addPrivateAppValve(metadata, appXmlDocument);

        addXForwardedForSupport(jettyXmlDocument);
    }

    public void buildJettyConfiguration(@Nonnull Path jettyBase) throws Exception {

        Path jettyXmlFile = jettyBase.resolve("etc/jetty.xml");
        Document jettyXmlDocument = XmlUtils.loadXmlDocumentFromPath(jettyXmlFile);
        XmlUtils.checkRootElement(jettyXmlDocument, "Configure");

        Path appXmlFile = jettyBase.resolve("webapps/app.xml");

        Document appXmlDocument = XmlUtils.loadXmlDocumentFromPath(appXmlFile);
        XmlUtils.checkRootElement(appXmlDocument, "Configure");

        this.buildJettyConfiguration(metadata, jettyXmlDocument, appXmlDocument);

        // see http://permalink.gmane.org/gmane.comp.ide.eclipse.jetty.user/1745
        Map<String, String> outputProperties = new HashMap<>();
        outputProperties.put(OutputKeys.DOCTYPE_PUBLIC, "-//Mort Bay Consulting//DTD Configure//EN");
        outputProperties.put(OutputKeys.DOCTYPE_SYSTEM, "http://jetty.mortbay.org/configure.dtd");

        XmlUtils.flush(appXmlDocument, Files.newOutputStream(appXmlFile), outputProperties);
        XmlUtils.flush(jettyXmlDocument, Files.newOutputStream(jettyXmlFile), outputProperties);
    }
}
