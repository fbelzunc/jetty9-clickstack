package com.cloudbees.genapp.metadata;

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

import com.cloudbees.genapp.Strings2;
import com.cloudbees.genapp.resource.Resource;
import com.cloudbees.genapp.resource.RuntimeProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * The Metadata class is instanciated by a MetadataFinder instance. It stores GenApp resources,
 * environment variables (given by -P with the SDK), and runtime parameters (given by -R with the SDK).
 * It also makes them accessible for other classes to (typically) write configuration files whithin ClickStacks.
 */

public class Metadata {
    private Map<String, Resource> resources;
    private Map<String, String> environment;
    private Map<String, RuntimeProperty> runtimeProperties;

    /**
     * This constructor is used by the Builder subclass to create a new Metadata instance
     *
     * @param resources         A map of the GenApp resources
     * @param environment       A map of the environment variables
     * @param runtimeProperties A map of RuntimeProperties
     */
    protected Metadata(Map<String, Resource> resources, Map<String, String> environment,
                       Map<String, RuntimeProperty> runtimeProperties) {
        this.resources = resources;
        this.environment = environment;
        this.runtimeProperties = runtimeProperties;
    }

    public Metadata() {
        this.resources = new HashMap<String, Resource>();
        this.environment = new HashMap<String, String>();
        this.runtimeProperties = new HashMap<String, RuntimeProperty>();
    }

    public <R extends Resource> R getResource(String resourceName) {
        return (R) resources.get(resourceName);
    }

    public Map<String, Resource> getResources() {
        return resources;
    }

    public String getEnvironmentVariable(String variableName) {
        return environment.get(variableName);
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    /**
     * @throws NullPointerException if parent property does not exist
     */
    @Nullable
    public Map<String, String> getRuntimeProperty(String section) {
        RuntimeProperty runtimeProperty = runtimeProperties.get(section);
        if (runtimeProperty == null) {
            return null;
        }
        return runtimeProperty.getParameters();
    }

    @Nullable
    public String getRuntimeParameter(String parent, String propertyName) {
        RuntimeProperty runtimeProperty = runtimeProperties.get(parent);
        if (runtimeProperty == null) {
            return null;
        }
        return runtimeProperty.getParameter(propertyName);
    }

    public String getRuntimeParameter(String parent, String propertyName, String defaultValue) {
        RuntimeProperty runtimeProperty = runtimeProperties.get(parent);
        if (runtimeProperty == null) {
            return defaultValue;
        }
        String value = runtimeProperty.getParameter(propertyName);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public void setRuntimeParameter(String parameter, String value) {
        String section = Strings2.substringBeforeFirst(parameter, '.');
        String property = Strings2.substringAfterFirst(parameter, '.');
        if (section == null) {
            throw new IllegalArgumentException("no key found in '" + parameter + "'");
        }

        if (property == null)
            throw new IllegalArgumentException("no property found in '" + parameter + "'");

        setRuntimeParameter(section, property, value);
    }

    public void setRuntimeParameter(String section, String property, String value) {
        RuntimeProperty runtimeProperty = this.runtimeProperties.get(section);
        if (runtimeProperty == null) {
            runtimeProperty = new RuntimeProperty(section);
            runtimeProperties.put(section, runtimeProperty);
        }
        runtimeProperty.getParameters().put(property, value);
    }

    /**
     * The Builder class creates a new Metadata instance from a metadata.json file.
     */

    public static class Builder {

        /**
         * This method parses a metadata.json file and returns a new Metadata instance containing the
         * metadata that has been parsed.
         *
         * @param metadataFile The absolute path to the metadata.json file to be parsed.
         * @return A new Metadata instance, containing the parameters from the metadata.json file.
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
         * This method is called from the fromFile method to parse json from a stream.
         *
         * @param metadataInputStream An InputStream to read the JSON metadata from.
         * @return A new Metadata instance, containing all resources parsed
         *         from the JSON metadata given as input.
         * @throws IOException
         */
        public static Metadata fromStream(InputStream metadataInputStream) throws IOException {
            ObjectMapper metadataObjectMapper = new ObjectMapper();

            JsonNode metadataRootNode = metadataObjectMapper.readTree(metadataInputStream);

            return fromJson(metadataRootNode);
        }

        /**
         * This method is called from the fromStream method to parse json from a stream.
         *
         * @param metadataRootNode the JSON metadata from.
         * @return A new Metadata instance, containing all resources parsed
         *         from the JSON metadata given as input.
         * @throws IOException
         */
        public static Metadata fromJson(JsonNode metadataRootNode) throws IOException {

            Builder metadataBuilder = new Builder();

            return metadataBuilder.buildResources(metadataRootNode);
        }

        /**
         * This method is called from the fromStream method to parse json from a stream.
         *
         * @param metadata the JSON metadata.
         * @return A new Metadata instance, containing all resources parsed
         *         from the JSON metadata given as input.
         * @throws IOException
         */
        public static Metadata fromJsonString(String metadata, boolean allowSingleQuotes) throws IOException {

            ObjectMapper metadataObjectMapper = new ObjectMapper();
            metadataObjectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

            JsonNode metadataRootNode = metadataObjectMapper.readTree(metadata);

            return fromJson(metadataRootNode);
        }

        /**
         * This method is called from the fromStream method to parse JSON metadata into a new Metadata instance.
         *
         * @param metadataRootNode The root node of the JSON metadata to be parsed.
         * @return A new Metadata instance containing all parsed metadata.
         */
        private Metadata buildResources(JsonNode metadataRootNode) {

            Map<String, Resource> resources = new TreeMap<String, Resource>();
            Map<String, String> environment = new TreeMap<String, String>();
            Map<String, RuntimeProperty> runtimeProperties = new TreeMap<String, RuntimeProperty>();

            /**
             *  We iterate over all children of the root node, determining if they're resources,
             *  runtime parameters or are part of the "app" section.
             */
            for (Iterator<Map.Entry<String, JsonNode>> fields = metadataRootNode.fields();
                 fields.hasNext(); ) {

                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode content = entry.getValue();
                String id = entry.getKey();
                Map<String, String> entryMetadata = new HashMap<String, String>();

                // We then iterate over all the key-value pairs present in the children node, and store them.
                for (Iterator<Map.Entry<String, JsonNode>> properties = content.fields();
                     properties.hasNext(); ) {
                    Map.Entry<String, JsonNode> property = properties.next();
                    String entryName = property.getKey();
                    JsonNode entryValueNode = property.getValue();

                    // We check if the entry is well-formed (i.e can be output to a String meaningfully).
                    if (entryValueNode.isTextual() || entryValueNode.isInt()) {
                        String entryValue = entryValueNode.asText();
                        entryMetadata.put(entryName, entryValue);
                    }

                    // We get environment variables from the metadata when we iterate over app.env
                    if (id.equals("app") && entryName.equals("env")) {
                        for (Iterator<Map.Entry<String, JsonNode>> envVariables = entryValueNode.fields();
                             envVariables.hasNext(); ) {
                            Map.Entry<String, JsonNode> envVariable = envVariables.next();
                            String envName = envVariable.getKey();
                            JsonNode envValue = envVariable.getValue();
                            if (envValue.isTextual()) {
                                environment.put(envName, envValue.asText());
                            }
                        }
                    }
                }

                Resource resource = Resource.Builder.buildResource(entryMetadata);
                // We check if the children node we are currently iterating upon is a resource.
                if (resource != null) {
                    resources.put(resource.getName(), resource);
                    // Otherwise, if it wasn't a resource nor the "app" field, it is composed of runtime parameters.
                } else if (!id.equals("app")) {
                    runtimeProperties.put(id, new RuntimeProperty(id, entryMetadata));
                }
            }
            return new Metadata(resources, environment, runtimeProperties);
        }
    }
}