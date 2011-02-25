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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "entity")
public class Entity {

    private String id;

    private String parentId;

    private String path;

    private ArrayList<Occurrrence> occurrences = new ArrayList<Occurrrence>();

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

    @XmlAttribute
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @XmlElementWrapper(name = "occurrences")
    @XmlElement(name = "occurrence")
    public ArrayList<Occurrrence> getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(ArrayList<Occurrrence> occurrences) {
        this.occurrences = occurrences;
    }

}
