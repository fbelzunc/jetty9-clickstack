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
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

import static org.junit.Assert.assertThat;
import static org.xmlmatchers.XmlMatchers.isEquivalentTo;
import static org.xmlmatchers.transform.XmlConverters.the;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class JettyConfigurationBuilderTest {

    File appDir = new File(System.getProperty("java.io.tmpdir"));
    private Document appXml;
    private Document jettyXml;

    @Before
    public void before() throws Exception {
        appXml = XmlUtils.loadXmlDocumentFromStream(Thread.currentThread().getContextClassLoader().getResourceAsStream("app.xml"));
        jettyXml = XmlUtils.loadXmlDocumentFromStream(Thread.currentThread().getContextClassLoader().getResourceAsStream("jetty.xml"));
    }

    @Test
    public void add_data_source_success_basic_config() throws Exception {

        // prepare
        String json = "{ \n" +
                "'cb-db': { \n" +
                "    'DATABASE_PASSWORD': 'test', \n" +
                "    'DATABASE_URL': 'mysql://mysql.mycompany.com:3306/test', \n" +
                "    'DATABASE_USERNAME': 'test', \n" +
                "    '__resource_name__': 'mydb', \n" +
                "    '__resource_type__': 'database' \n" +
                "}\n" +
                "}";
        Metadata metadata = Metadata.Builder.fromJsonString(json, true);
        JettyConfigurationBuilder contextXmlBuilder = new JettyConfigurationBuilder(metadata, appDir);

        Database database = metadata.getResource("mydb");

        // run
        contextXmlBuilder.addDatabase(database, appXml);

        // XmlUtils.flush(appXml, System.out);

        String xml = "" +
                "   <New class='org.eclipse.jetty.plus.jndi.Resource' id='mydb'> \n" +
                "      <Arg>jdbc/mydb</Arg> \n" +
                "      <Arg> \n" +
                "         <New class='org.apache.tomcat.jdbc.pool.DataSource'> \n" +
                "            <Set name='driverClassName'>com.mysql.jdbc.Driver</Set> \n" +
                "            <Set name='maxActive'>20</Set> \n" +
                "            <Set name='maxIdle'>10</Set> \n" +
                "            <Set name='minIdle'>1</Set> \n" +
                "            <Set name='password'>test</Set> \n" +
                "            <Set name='testOnBorrow'>true</Set> \n" +
                "            <Set name='testWhileIdle'>true</Set> \n" +
                "            <Set name='url'>jdbc:mysql://mysql.mycompany.com:3306/test</Set> \n" +
                "            <Set name='username'>test</Set> \n" +
                "            <Set name='validationInterval'>5000</Set> \n" +
                "            <Set name='validationQuery'>select 1</Set> \n" +
                "         </New> \n" +
                "      </Arg> \n" +
                "   </New> ";

        // verify
        Element dataSource = XmlUtils.getUniqueElement(appXml, "/Configure/New[@id='mydb']");

        assertThat(the(dataSource), isEquivalentTo(the(xml)));
    }

    @Test
    public void add_xforwarded_support() throws Exception {

        // prepare
        String json = "{ \n" +
                "}";
        Metadata metadata = Metadata.Builder.fromJsonString(json, true);
        JettyConfigurationBuilder contextXmlBuilder = new JettyConfigurationBuilder(metadata, appDir);


        // run
        contextXmlBuilder.addXForwardedForSupport(jettyXml);

        XmlUtils.flush(jettyXml, System.out);

        String xml = "" +
                "<Call name='addCustomizer'>" +
                "   <Arg>" +
                "      <New class='org.eclipse.jetty.server.ForwardedRequestCustomizer' />" +
                "   </Arg>" +
                "</Call> ";

        // verify
        Element addForwardedRequestCustomizer = XmlUtils.getUniqueElement(jettyXml, "/Configure/New[@id='httpConfig']/Call[@name='addCustomizer']");

        assertThat(the(addForwardedRequestCustomizer), isEquivalentTo(the(xml)));
    }
}
