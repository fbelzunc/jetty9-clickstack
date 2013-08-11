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

import com.cloudbees.genapp.metadata.Metadata;
import com.cloudbees.genapp.metadata.MetadataFinder;

import java.io.File;

/*
 * This class contains the main method to get the Genapp metadata and configure Tomcat 7.
 */

public class Setup {
    /**
     * The main method takes optional arguments for the location of the
     * context.xml file to modify, as well as the location of the metadata.json
     * file. Defaults are:
     * CONTEXT_XML_PATH = $app_dir/server/conf/context.xml
     * METADATA_PATH = $genapp_dir/metadata.json
     *
     * @param args Two optional args: [ CONTEXT_XML_PATH [ METADATA_PATH ]]
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        MetadataFinder metadataFinder = new MetadataFinder();
        Metadata metadata = metadataFinder.getMetadata();


        File appDir = new File(System.getenv("app_dir"));
        JettyAppXmlBuilder contextXmlBuilder = new JettyAppXmlBuilder(metadata, appDir);
        contextXmlBuilder.buildJettyConfiguration("/jetty9/webapps/app.xml");
    }
}
