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

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.osgi.service.component.ComponentContext;

/**
 * Enhancement engine implementation that delegate the analysis work to a Temis Luxid Annotation Factory
 * service.
 */
@Component(immediate = false, metatype = true, label = "%stanbol.TemisEnhancementEngine.name", description = "%stanbol.TemisEnhancementEngine.description")
@Service
public class TemisEnhancementEngine implements EnhancementEngine, ServiceProperties {

    @Property
    public static final String SERVICE_URL_PROPERTY = "stanbol.temis.service.url";

    @Property
    public static final String SERVICE_ACCOUNT_ID_PROPERTY = "stanbol.temis.service.account.id";

    @Property
    public static final String SERVICE_ACCOUNT_PASSWORD_PROPERTY = "stanbol.temis.service.account.password";

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_CONTENT_EXTRACTION}
     */
    public static final Integer defaultOrder = ORDERING_CONTENT_EXTRACTION;

    protected static final String TEXT_PLAIN_MIMETYPE = "text/plain";

    protected String url;

    protected String id;

    protected String password;

    protected void activate(ComponentContext ce) throws IOException {
        @SuppressWarnings("unchecked")
        Dictionary<String,String> properties = ce.getProperties();
        url = properties.get(SERVICE_URL_PROPERTY);
        id = properties.get(SERVICE_ACCOUNT_ID_PROPERTY);
        password = properties.get(SERVICE_ACCOUNT_PASSWORD_PROPERTY);

        // check the connection to fail early in case of bad configuration parameters
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {

    }

    public int canEnhance(ContentItem ci) {
        // TODO: check what format are supported by Luxid instead of constraining to text/plain
        String mimeType = ci.getMimeType().split(";", 2)[0];
        if (TEXT_PLAIN_MIMETYPE.equalsIgnoreCase(mimeType)) {
            return ENHANCE_SYNCHRONOUS;
        }
        return CANNOT_ENHANCE;
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING,
            (Object) defaultOrder));
    }
}
