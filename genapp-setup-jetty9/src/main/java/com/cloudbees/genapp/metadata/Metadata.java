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
package com.cloudbees.genapp.metadata;

import com.cloudbees.genapp.resource.Resource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * The Metadata class stores Resources entries by name in a map.
 */

public class Metadata {
    private Map<String, Resource> resources;
    
    public Metadata(Map<String, Resource> resources) {
        this.resources = new TreeMap<String, Resource>();
        this.resources.putAll(resources);
    }

    public Map<String, Resource> getResources() {
        return resources;
    }

    public static class Builder {

        /**
         * @param metadataFile The metadata.json file
         * @return A new Metadata instance, containing all resources parsed
         * from the JSON metadata given as input.
         * @throws java.io.IOException
         */
        public static Metadata fromFile(File metadataFile) throws IOException {
            FileInputStream metadataInputStream = new FileInputStream(metadataFile);
            try {
                return fromStream(metadataInputStream);
            } finally {
                metadataInputStream.close();
            }
        }

        /**
         * @param metadataInputStream An InputStream to read the JSON metadata from.
         * @return A new Metadata instance, containing all resources parsed
         * from the JSON metadata given as input.
         * @throws IOException
         */
        public static Metadata fromStream(InputStream metadataInputStream) throws IOException {
            ObjectMapper metadataObjectMapper = new ObjectMapper();

            JsonNode metadataRootNode = metadataObjectMapper.readTree(metadataInputStream);
            Builder metadataBuilder = new Builder();

            return metadataBuilder.buildResources(metadataRootNode);
        }

        /**
         * Parses resources and returns them in a new MetadataBuilder instance.
         * @param metadataRootNode The root node of the Json metadata to be parsed.
         * @return A new MetadataBuilder instance containing all parsed resources.
         **/
        private Metadata buildResources(JsonNode metadataRootNode) {
            Map<String, Resource> resources = new TreeMap<String, Resource>();

            for (Iterator<Map.Entry<String, JsonNode>> resourceFields = metadataRootNode.fields();
                 resourceFields.hasNext(); ) {

                Map.Entry<String, JsonNode> resourceEntry = resourceFields.next();
                JsonNode resourceContent = resourceEntry.getValue();
                Map<String, String> resourceMetadata = new HashMap<String, String>();

                for (Iterator<Map.Entry<String, JsonNode>> resourceProperties = resourceContent.fields();
                     resourceProperties.hasNext(); ) {
                    Map.Entry<String, JsonNode> resourceProperty = resourceProperties.next();
                    String resourceEntryName = resourceProperty.getKey();
                    JsonNode resourceEntryValueNode = resourceProperty.getValue();

                    // We check if the is well-formed.
                    if (resourceEntryValueNode.isTextual()) {
                        String resourceEntryValue = resourceEntryValueNode.asText();
                        resourceMetadata.put(resourceEntryName, resourceEntryValue);
                        Resource resource = Resource.Builder.buildResource(resourceMetadata);
                        if (resource != null) {
                            resources.put(resource.getName(), resource);
                        }
                    }
                }
            }
            return new Metadata(resources);
        }
    }
}