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

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.nuxeo.stanbol.temis.impl.AnnotationPlan;
import org.nuxeo.stanbol.temis.impl.ArrayOfAnnotationPlan;
import org.nuxeo.stanbol.temis.impl.Fault;
import org.nuxeo.stanbol.temis.impl.Output;
import org.nuxeo.stanbol.temis.impl.OutputPart;
import org.nuxeo.stanbol.temis.impl.TemisWebService;
import org.nuxeo.stanbol.temis.impl.TemisWebServicePortType;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

/**
 * Enhancement engine implementation that delegate the analysis work to a Temis Luxid Annotation Factory
 * service.
 * 
 * The connection properties can be looked up from environment variables (upper case OSGi property names with
 * '_' instead of '.') if the properties are left undefined in the OSGi configuration.
 */
@Component(configurationFactory = true, immediate = true, metatype = true, policy = ConfigurationPolicy.REQUIRE, specVersion = "1.1", inherit = true, label = "%stanbol.TemisEnhancementEngine.name", description = "%stanbol.TemisEnhancementEngine.description")
@Service
@Properties(value = {@Property(name = EnhancementEngine.PROPERTY_NAME, value = "temis"),
                     @Property(name = Constants.SERVICE_RANKING, intValue = 0)})
public class TemisLuxidEnhancementEngine extends
        AbstractEnhancementEngine<ConfigurationException,RuntimeException> implements EnhancementEngine,
        ServiceProperties {

    public static final String SIMPLE_XML_CONSUMER = "SimpleXML";

    public static final Log log = LogFactory.getLog(TemisLuxidEnhancementEngine.class);

    public static final QName SERVICE_NAME = new QName("http://luxid.temis.com/ws", "TemisWebService");

    @Property
    public static final String SERVICE_WSDL_URL_PROPERTY = "stanbol.temis.luxid.service.wsdl.url";

    @Property
    public static final String SERVICE_ACCOUNT_ID_PROPERTY = "stanbol.temis.luxid.service.account.id";

    @Property
    public static final String SERVICE_ACCOUNT_PASSWORD_PROPERTY = "stanbol.temis.luxid.service.account.password";

    @Property
    public static final String SERVICE_ANNOTATION_PLAN_PROPERTY = "stanbol.temis.luxid.service.annotation.plan";

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_CONTENT_EXTRACTION}
     */
    public static final Integer defaultOrder = ORDERING_CONTENT_EXTRACTION;

    protected static final String TEXT_PLAIN_MIMETYPE = "text/plain";

    public static final String LUXID_NS = "http://www.temis.com/luxid#";

    protected String annotationPlan;

    protected String accountId;

    protected String accountPassword;

    protected TemisWebServicePortType wsPort;

    /**
     * Load a required property value from OSGi context with fall-back on environment variable.
     * 
     * @throws ConfigurationException
     *             if no configuration is found in either contexts.
     */
    protected String getFromPropertiesOrEnv(Dictionary<String,String> properties, String propertyName) throws ConfigurationException {
        String envVariableName = propertyName.replaceAll("\\.", "_").toUpperCase();
        String propertyValue = System.getenv(envVariableName);
        if (properties.get(propertyName) != null && !properties.get(propertyName).trim().isEmpty()) {
            propertyValue = properties.get(propertyName);
        }
        if (propertyValue == null || propertyValue.trim().isEmpty()) {
            throw new ConfigurationException(propertyName, String.format("%s is a required property",
                propertyName));
        }
        return propertyValue;
    }

    @Override
    protected void activate(ComponentContext ce) throws ConfigurationException {
        super.activate(ce);
        @SuppressWarnings("unchecked")
        Dictionary<String,String> properties = ce.getProperties();
        String urlString = getFromPropertiesOrEnv(properties, SERVICE_WSDL_URL_PROPERTY);
        accountId = getFromPropertiesOrEnv(properties, SERVICE_ACCOUNT_ID_PROPERTY);
        accountPassword = getFromPropertiesOrEnv(properties, SERVICE_ACCOUNT_PASSWORD_PROPERTY);
        annotationPlan = getFromPropertiesOrEnv(properties, SERVICE_ANNOTATION_PLAN_PROPERTY);

        // check the connection to fail early in case of bad configuration parameters
        String sessionId = null;
        try {
            TemisWebService tws = new TemisWebService(new URL(urlString), SERVICE_NAME);
            wsPort = tws.getWebAnnotationPort();
            sessionId = connect();
            // check that the requested annotationPlan is available to the authenticated user
            Holder<ArrayOfAnnotationPlan> plans = new Holder<ArrayOfAnnotationPlan>();
            Holder<Fault> fault = new Holder<Fault>();
            wsPort.getPlans(sessionId, plans, fault);
            handleFault(fault);
            boolean foundPlan = false;
            List<String> availablePlanNames = new ArrayList<String>();
            for (AnnotationPlan availablePlan : plans.value.getReturn()) {
                if (availablePlan.getName().equals(annotationPlan)) {
                    foundPlan = true;
                    break;
                }
                availablePlanNames.add(availablePlan.getName());
            }
            if (!foundPlan) {
                throw new TemisEnhancementEngineException(String.format(
                    "The requested annotationPlan '%s' is does not belong to"
                            + " the list of available plans: '%s'", annotationPlan,
                    StringUtils.join(availablePlanNames, ", ")));
            }
        } catch (TemisEnhancementEngineException e) {
            log.error(e, e);
            throw new ConfigurationException(SERVICE_WSDL_URL_PROPERTY, e.getMessage());
        } catch (MalformedURLException e) {
            throw new ConfigurationException(SERVICE_WSDL_URL_PROPERTY, e.getMessage());
        } finally {
            if (sessionId != null) {
                wsPort.closeSession(sessionId);
            }
        }
    }

    @Override
    protected void deactivate(ComponentContext ce) {
        super.deactivate(ce);
        wsPort = null;
        annotationPlan = null;
    }

    public String connect() throws TemisEnhancementEngineException {
        Holder<String> token = new Holder<String>();
        Holder<Fault> fault = new Holder<Fault>();
        wsPort.authenticate(accountId, accountPassword, token, fault);
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
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        MGraph g = ci.getMetadata();
        try {
            token = connect();
            Holder<Fault> fault = new Holder<Fault>();
            // TODO: read charset from the request instead of hardcoding UTF-8 requirement
            // TODO: extract ~3 sentences context for each annotation is possible
            String text = IOUtils.toString(ci.getStream(), "UTF-8");
            Holder<Output> output = new Holder<Output>();
            wsPort.annotateString(token, annotationPlan, text, SIMPLE_XML_CONSUMER, output, fault);
            handleFault(fault);
            for (OutputPart part : output.value.getParts()) {
                if ("DOCUMENT".equals(part.getName()) && "text/xml".equals(part.getMime())) {
                    Doc result = Doc.readFrom(part.getText());
                    for (Entity entity : result.getEntities()) {
                        UriRef entityAnnotation = EnhancementEngineHelper.createEntityEnhancement(ci, this);
                        String entityPath = entity.getPath();
                        UriRef entityUri = new UriRef(LUXID_NS + entityPath);
                        String entityLabel = entityPath.substring(entityPath.lastIndexOf('/') + 1);

                        // add the link to the referred entity
                        g.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_REFERENCE, entityUri));
                        g.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_LABEL, literalFactory
                                .createTypedLiteral(entityLabel)));
                        Set<UriRef> stanbolTypes = getStanbolTypes(entityPath);
                        for (UriRef entityType : stanbolTypes) {
                            g.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_TYPE, entityType));
                        }

                        // register entity occurrences
                        for (Occurrence occurrence : entity.getOccurrences()) {
                            UriRef textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, this);
                            for (UriRef entityType : stanbolTypes) {
                                g.add(new TripleImpl(textAnnotation, DC_TYPE, entityType));
                            }
                            String context = findContext(text, occurrence.getBegin(), occurrence.getEnd());
                            String selectedText = occurrence.getText();
                            g.add(new TripleImpl(textAnnotation, ENHANCER_SELECTED_TEXT, literalFactory
                                    .createTypedLiteral(selectedText)));
                            g.add(new TripleImpl(textAnnotation, ENHANCER_SELECTION_CONTEXT, literalFactory
                                    .createTypedLiteral(context)));
                            g.add(new TripleImpl(textAnnotation, ENHANCER_START, literalFactory
                                    .createTypedLiteral(context.indexOf(selectedText))));
                            g.add(new TripleImpl(textAnnotation, ENHANCER_END,
                                    literalFactory.createTypedLiteral(context.indexOf(selectedText)
                                                                      + selectedText.length())));
                            g.add(new TripleImpl(entityAnnotation, DC_RELATION, textAnnotation));
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new EngineException(e);
        } catch (JAXBException e) {
            throw new EngineException(e);
        } finally {
            if (token != null) {
                wsPort.closeSession(token);
            }
        }
    }

    protected String findContext(String text, int begin, int end) {
        if (begin < 0) {
            begin = 0;
        }
        if (text.length() < end) {
            end = text.length();
        }
        String prefix = shorten(text.substring(0, begin), 30, true);
        String suffix = shorten(text.substring(end), 30, false);
        String selected = text.substring(begin, end);
        return String.format("%s %s %s", prefix, selected, suffix);
    }

    protected String shorten(String content, int maxWords, boolean reverse) {
        if (content == null) {
            return "";
        }
        List<String> tokens = Arrays.asList(content.split(" "));
        if (tokens.size() > maxWords) {
            if (reverse) {
                Collections.reverse(tokens);
            }
            tokens = new ArrayList<String>(tokens.subList(0, maxWords));
            if ((!reverse && content.startsWith(" ")) || (reverse && content.endsWith(" "))) {
                // re-add missing space removed by split
                tokens.add(0, " ");
            }
            if (reverse) {
                Collections.reverse(tokens);
            }
            return StringUtils.join(tokens, " ");
        }
        return content;
    }

    protected Set<UriRef> getStanbolTypes(String entityPath) {
        Set<UriRef> types = new TreeSet<UriRef>();

        // TODO: un-hard-code mapping: use a configuration file or an OSGi property
        Map<String,UriRef> typeMap = new HashMap<String,UriRef>();
        typeMap.put("/Entity/Person", OntologicalClasses.DBPEDIA_PERSON);
        typeMap.put("/Entity/Media", OntologicalClasses.DBPEDIA_ORGANISATION);
        typeMap.put("/Entity/Organisation", OntologicalClasses.DBPEDIA_ORGANISATION);
        typeMap.put("/Entity/Company", OntologicalClasses.DBPEDIA_ORGANISATION);
        typeMap.put("/Entity/Location", OntologicalClasses.DBPEDIA_PLACE);

        while (entityPath.lastIndexOf('/') != -1) {
            entityPath = entityPath.substring(0, entityPath.lastIndexOf('/'));
            UriRef type = typeMap.get(entityPath);
            if (type != null) {
                types.add(type);
            }
        }
        return types;
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
