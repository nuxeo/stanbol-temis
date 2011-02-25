package org.nuxeo.stanbol.temis.engine;

import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.nuxeo.stanbol.temis.impl.Fault;

/**
 * Exception thrown when encountering unexpected remote server fault on the Temis webservice.
 */
public class TemisEnhancementEngineException extends EngineException {

    private static final long serialVersionUID = 1L;

    protected final Fault fault;

    public TemisEnhancementEngineException(Fault fault) {
        super(fault.getMessage());
        this.fault = fault;
    }

    public TemisEnhancementEngineException(String message) {
        super(message);
        fault = null;
    }

}
