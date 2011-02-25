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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.nuxeo.stanbol.temis.impl.AnnotationPlan;
import org.nuxeo.stanbol.temis.impl.ArrayOfAnnotationPlan;
import org.nuxeo.stanbol.temis.impl.Fault;
import org.nuxeo.stanbol.temis.impl.Output;
import org.nuxeo.stanbol.temis.impl.OutputPart;
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

    public static final String SIMPLE_XML_CONSUMER = "SimpleXML";

    public static final Log log = LogFactory.getLog(TemisEnhancementEngine.class);

    public static final QName SERVICE_NAME = new QName("http://luxid.temis.com/ws", "TemisWebService");

    @Property
    public static final String SERVICE_WSDL_URL_PROPERTY = "stanbol.temis.service.wsdl.url";

    @Property
    public static final String SERVICE_ACCOUNT_ID_PROPERTY = "stanbol.temis.service.account.id";

    @Property
    public static final String SERVICE_ACCOUNT_PASSWORD_PROPERTY = "stanbol.temis.service.account.password";

    @Property(value = {"Entities"})
    public static final String SERVICE_ANNOTATION_PLAN_PROPERTY = "stanbol.temis.service.annotation.plan";

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_CONTENT_EXTRACTION}
     */
    public static final Integer defaultOrder = ORDERING_CONTENT_EXTRACTION;

    protected static final String TEXT_PLAIN_MIMETYPE = "text/plain";

    protected String plan;

    protected String id;

    protected String password;

    protected TemisWebServicePortType wsPort;

    protected void activate(ComponentContext ce) throws MalformedURLException,
                                                TemisEnhancementEngineException {
        @SuppressWarnings("unchecked")
        Dictionary<String,String> properties = ce.getProperties();
        String urlString = properties.get(SERVICE_WSDL_URL_PROPERTY);
        id = properties.get(SERVICE_ACCOUNT_ID_PROPERTY);
        password = properties.get(SERVICE_ACCOUNT_PASSWORD_PROPERTY);
        plan = properties.get(SERVICE_ANNOTATION_PLAN_PROPERTY);
        if (urlString == null || id == null || password == null || plan == null) {
            throw new TemisEnhancementEngineException(
                    "Cannot activate TemisEnhancementEngine without configuration parameters");
        }

        // check the connection to fail early in case of bad configuration parameters
        TemisWebService tws = new TemisWebService(new URL(urlString), SERVICE_NAME);
        wsPort = tws.getWebAnnotationPort();
        String sessionId = connect();
        try {
            // check that the requested plan is available to the authenticated user
            Holder<ArrayOfAnnotationPlan> plans = new Holder<ArrayOfAnnotationPlan>();
            Holder<Fault> fault = new Holder<Fault>();
            wsPort.getPlans(sessionId, plans, fault);
            handleFault(fault);
            boolean foundPlan = false;
            List<String> availablePlanNames = new ArrayList<String>();
            for (AnnotationPlan availablePlan : plans.value.getReturn()) {
                if (availablePlan.getName().equals(plan)) {
                    foundPlan = true;
                    break;
                }
                availablePlanNames.add(availablePlan.getName());
            }
            if (!foundPlan) {
                throw new TemisEnhancementEngineException(String.format(
                    "The requested plan '%s' is does not belong to the list of available plans: '%s'", plan,
                    StringUtils.join(availablePlanNames, ", ")));
            }
        } finally {
            wsPort.closeSession(sessionId);
        }
    }

    protected void deactivate(ComponentContext ce) {
        wsPort = null;
        plan = null;
    }

    public String connect() throws TemisEnhancementEngineException {
        Holder<String> token = new Holder<String>();
        Holder<Fault> fault = new Holder<Fault>();
        wsPort.authenticate(id, password, token, fault);
        handleFault(fault);
        return token.value;
    }

    protected void handleFault(Holder<Fault> fault) throws TemisEnhancementEngineException {
        if (fault.value != null && fault.value.getMessage() != null && !fault.value.getMessage().isEmpty()) {
            throw new TemisEnhancementEngineException(fault.value);
        }
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        String token = null;
        try {
            token = connect();
            Holder<Fault> fault = new Holder<Fault>();
            // TODO: read charset from the request instead of harcoding UTF-8 requirement
            String data = IOUtils.toString(ci.getStream(), "UTF-8");
            Holder<Output> output = new Holder<Output>();
            wsPort.annotateString(token, plan, data, SIMPLE_XML_CONSUMER, output, fault);
            handleFault(fault);
            for (OutputPart part : output.value.getParts()) {
                log.info(part.getName());
                log.info(part.getMime());
                log.info(part.getText());
            }
        } catch (IOException e) {
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
