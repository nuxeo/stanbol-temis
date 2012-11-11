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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;

@XmlRootElement(name = "entity")
public class Entity {

    private String id;

    private String parentId;

    private String path;

    private ArrayList<Occurrence> occurrences = new ArrayList<Occurrence>();

    private String name;

    protected final List<String> transliterations = new ArrayList<String>();

    @XmlAttribute
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlAttribute
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    @XmlAttribute
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
        this.name = path.substring(path.lastIndexOf('/') + 1).trim();
    }

    @XmlElementWrapper(name = "occurrences")
    @XmlElement(name = "occurrence")
    public ArrayList<Occurrence> getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(ArrayList<Occurrence> occurrences) {
        this.occurrences = occurrences;
    }

    /**
     * @return a string that summarizes the occurrence information to detect
     *         entity annotations that refer to the same occurrences in the
     *         document: this is useful to detect transliterations.
     */
    public String getOccurrencesFingerprint() {
        List<String> identifiers = new ArrayList<String>();
        for (Occurrence occ : occurrences) {
            identifiers.add(occ.getIdentitier());
        }
        return StringUtils.join(identifiers, " ");
    }

    /**
     * @return
     */
    public boolean isTransliteration() {
        if (occurrences.isEmpty()) {
            // edge case that does not occur in practice
            return false;
        }
        for (Occurrence occ: occurrences) {
            if (name.equals(occ.getText().trim())) {
                return false;
            }
        }
        return true;
    }
}
