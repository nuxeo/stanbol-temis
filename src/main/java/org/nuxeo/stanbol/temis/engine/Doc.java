/* Copyright 2011 Nuxeo and contributors.
 * 
 * This file is licensed to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.stanbol.temis.engine;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "doc")
public class Doc {

    private ArrayList<Entity> entities = new ArrayList<Entity>();

    @XmlElementWrapper(name = "entities")
    @XmlElement(name = "entity")
    public ArrayList<Entity> getEntities() {
        return entities;
    }

    public void setEntities(ArrayList<Entity> entities) {
        this.entities = entities;
    }

    /**
     * Group entities referring to the same occurrence are merged and treated as
     * tranlisterations of one another.
     */
    public List<Entity> getMergedEntities() {
        List<Entity> mergedEntities = new ArrayList<Entity>();
        Map<String, List<Entity>> entityGroups = new LinkedHashMap<String, List<Entity>>();
        for (Entity entity : entities) {
            // Use the occurrence data as an entity identifier to detect
            // transliterations
            String fingerprint = entity.getOccurrencesFingerprint();
            if (fingerprint == null || entity.isTopic()) {
                // no occurrence, this is a topic or an entity type
                continue;
            } else {
                List<Entity> group = entityGroups.get(fingerprint);
                if (group == null) {
                    group = new ArrayList<Entity>();
                    entityGroups.put(fingerprint, group);
                }
                group.add(entity);
            }
        }
        for (List<Entity> group : entityGroups.values()) {
            mergedEntities.addAll(mergeTransliteration(group));
        }
        return mergedEntities;
    }

    public List<Entity> getTopicEntities() {
        List<Entity> topicEntities = new ArrayList<Entity>();
        for (Entity entity : entities) {
            if (entity.isTopic()) {
                topicEntities.add(entity);
            }
        }
        return topicEntities;
    }

    protected Collection<? extends Entity> mergeTransliteration(
            List<Entity> entityGroup) {
        if (entityGroup.size() == 1) {
            // don't merge transliterations for singletons
            return entityGroup;
        }
        List<Entity> mergedEntities = new ArrayList<Entity>();
        List<String> transliterations = new ArrayList<String>();
        for (Entity entity : entityGroup) {
            if (entity.isTransliteration()) {
                transliterations.add(entity.getName());
            } else {
                mergedEntities.add(entity);
            }
        }
        for (Entity mergedEntity : mergedEntities) {
            mergedEntity.transliterations.addAll(transliterations);
        }
        return mergedEntities;
    }

    public static Doc readFrom(String xmlPayload) throws JAXBException {
        return readFrom(new ByteArrayInputStream(
                xmlPayload.getBytes(Charset.forName("UTF-8"))));
    }

    public static Doc readFrom(InputStream xmlStream) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Doc.class);
        Unmarshaller um = context.createUnmarshaller();
        return (Doc) um.unmarshal(xmlStream);
    }
}
