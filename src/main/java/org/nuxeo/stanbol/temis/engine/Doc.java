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
import java.util.ArrayList;

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

    public static Doc readFrom(String xmlPayload) throws JAXBException {
        return readFrom(new ByteArrayInputStream(xmlPayload.getBytes()));
    }

    public static Doc readFrom(InputStream xmlStream) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Doc.class);
        Unmarshaller um = context.createUnmarshaller();
        return (Doc) um.unmarshal(xmlStream);
    }
}
