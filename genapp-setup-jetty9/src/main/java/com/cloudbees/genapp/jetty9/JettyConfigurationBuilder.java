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

import com.cloudbees.genapp.XmlUtils;
import com.cloudbees.genapp.metadata.Metadata;
import com.cloudbees.genapp.resource.Database;
import com.cloudbees.genapp.resource.Email;
import com.cloudbees.genapp.resource.Resource;
import com.cloudbees.genapp.resource.SessionStore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class JettyConfigurationBuilder {

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
    private File appDir;

    public JettyConfigurationBuilder(Metadata metadata, File appDir) {
        this.metadata = metadata;
        if (!appDir.exists()) {
            throw new IllegalArgumentException("appDir does not exist '" + appDir.getAbsolutePath() + "'");
        } else if (!appDir.isDirectory()) {
            throw new IllegalArgumentException("appDir must be a directory '" + appDir.getAbsolutePath() + "'");
        }
        this.appDir = appDir;
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
    protected JettyConfigurationBuilder addDatabase(Database database, Document contextDocument) {
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

    protected JettyConfigurationBuilder addEmail(Email email, Document appXmlDocument) {
        logger.warning("Ignore addEmail(" + email + ")");
        return this;
    }

    protected JettyConfigurationBuilder addSessionStore(SessionStore store, Document appXmlDocument) {
        logger.warning("Ignore addSessionStore(" + store + ")");

        return this;
    }

    protected JettyConfigurationBuilder addPrivateAppValve(Metadata metadata, Document appXmlDocument) {
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

    /**
     * @param jettyXmlFilePath path of <code>jetty.xml</code> relative to {@link #appDir}
     * @param appXmlFilePath   path of <code>app.xml</code> relative to {@link #appDir}
     */
    public void buildJettyConfiguration(String jettyXmlFilePath, String appXmlFilePath) throws Exception {

        File jettyXmlFile = new File(appDir, jettyXmlFilePath);
        Document jettyXmlDocument = XmlUtils.loadXmlDocumentFromFile(jettyXmlFile);
        XmlUtils.checkRootElement(jettyXmlDocument, "Configure");

        File appXmlFile = new File(appDir, appXmlFilePath);
        Document appXmlDocument = XmlUtils.loadXmlDocumentFromFile(appXmlFile);
        XmlUtils.checkRootElement(appXmlDocument, "Configure");

        this.buildJettyConfiguration(metadata, jettyXmlDocument, appXmlDocument);

        // see http://permalink.gmane.org/gmane.comp.ide.eclipse.jetty.user/1745
        Map<String, String> outputProperties = new HashMap<>();
        outputProperties.put(OutputKeys.DOCTYPE_PUBLIC, "-//Mort Bay Consulting//DTD Configure//EN");
        outputProperties.put(OutputKeys.DOCTYPE_SYSTEM, "http://jetty.mortbay.org/configure.dtd");

        XmlUtils.flush(appXmlDocument, new FileOutputStream(appXmlFile), outputProperties);
        XmlUtils.flush(jettyXmlDocument, new FileOutputStream(jettyXmlFile), outputProperties);
    }
}
