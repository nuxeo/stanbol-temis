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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.nuxeo.stanbol.temis.impl.ArrayOfAnnotationPlan;
import org.nuxeo.stanbol.temis.impl.Fault;
import org.nuxeo.stanbol.temis.impl.Output;
import org.nuxeo.stanbol.temis.impl.TemisWebService;
import org.nuxeo.stanbol.temis.impl.TemisWebServicePortType;
import org.osgi.service.component.ComponentContext;

/**
 * Enhancement engine implementation that delegate the analysis work to a Temis Luxid Annotation Factory
 * service.
 */
@Component(immediate = false, metatype = true, label = "%stanbol.TemisEnhancementEngine.name", description = "%stanbol.TemisEnhancementEngine.description")
@Service
public class TemisEnhancementEngine implements EnhancementEngine, ServiceProperties {

    public static final QName SERVICE_NAME = new QName("http://luxid.temis.com/ws", "TemisWebService");

    @Property
    public static final String SERVICE_WSDL_URL_PROPERTY = "stanbol.temis.service.wsdl.url";

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

    protected URL wsdlUrl;

    protected String id;

    protected String password;

    protected TemisWebServicePortType wsPort;

    protected void activate(ComponentContext ce) throws MalformedURLException, TemisAccessException {
        @SuppressWarnings("unchecked")
        Dictionary<String,String> properties = ce.getProperties();
        String urlString = properties.get(SERVICE_WSDL_URL_PROPERTY);
        id = properties.get(SERVICE_ACCOUNT_ID_PROPERTY);
        password = properties.get(SERVICE_ACCOUNT_PASSWORD_PROPERTY);

        if (urlString == null || id == null || password == null) {
            return;
        }
        wsdlUrl = new URL(urlString);

        // check the connection to fail early in case of bad configuration parameters
        TemisWebService tws = new TemisWebService(wsdlUrl, SERVICE_NAME);
        wsPort = tws.getWebAnnotationPort();
        wsPort.closeSession(connect());
    }

    protected void deactivate(ComponentContext ce) {
        wsdlUrl = null;
        id = null;
        password = null;
        wsPort = null;
    }

    public String connect() throws TemisAccessException {
        Holder<String> token = new Holder<String>();
        Holder<Fault> fault = new Holder<Fault>();
        wsPort.authenticate(id, password, token, fault);

        return token.value;
    }

    protected void handleFault(Holder<Fault> fault) throws TemisAccessException {
        if (fault.value != null) {
            throw new TemisAccessException(fault.value);
        }
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        String token = null;
        try {
            token = connect();
            Holder<ArrayOfAnnotationPlan> plans = new Holder<ArrayOfAnnotationPlan>();
            Holder<Fault> fault = new Holder<Fault>();
            wsPort.getPlans(token, plans, fault);
            handleFault(fault);
            String data = "";
            String consumer = "";
            Holder<Output> output = new Holder<Output>();
            wsPort.annotateString(token, plans.value.getReturn().get(0).getName(), data, consumer, output,
                fault);
            handleFault(fault);
        } catch (TemisAccessException e) {
            throw new EngineException(e);
        } finally {
            if (token != null) {
                wsPort.closeSession(token);
            }
        }
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
