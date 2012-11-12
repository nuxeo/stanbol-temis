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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

@XmlRootElement(name = "entity")
public class Entity {

    protected Pattern iptcName = Pattern.compile("^(\\d+) - (.+?)(-DEPRECATED)?$");

    protected Integer iptcNewsCode = null;

    protected String id;

    protected String parentId;

    protected String path;

    protected ArrayList<Occurrence> occurrences = new ArrayList<Occurrence>();

    protected String name;

    protected final List<String> transliterations = new ArrayList<String>();

    protected boolean deprecated = false;

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
        Matcher matcher = iptcName.matcher(this.name);
        if (matcher.matches()) {
            this.iptcNewsCode = Integer.valueOf(matcher.group(1));
            this.name = WordUtils.capitalize(matcher.group(2));
            if (matcher.groupCount() == 3) {
                this.deprecated  = true;
            }
        }
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
        if (occurrences.isEmpty()) {
            return null;
        }
        List<String> identifiers = new ArrayList<String>();
        for (Occurrence occ : occurrences) {
            identifiers.add(occ.getIdentitier());
        }
        return StringUtils.join(identifiers, " ");
    }

    /**
     * heuristic detection of entity annotation that mark transliterations
     */
    public boolean isTransliteration() {
        if (occurrences.isEmpty()) {
            // edge case that does not occur in practice
            return false;
        }
        for (Occurrence occ : occurrences) {
            if (name.equals(occ.getText().trim())) {
                return false;
            }
        }
        return true;
    }

    public boolean isTopic() {
        return path != null && path.startsWith("/Category/");
    }

    public Integer getIptcNewsCode() {
        return iptcNewsCode;
    }

    public boolean isDeprecated() {
        return deprecated;
    }
}
