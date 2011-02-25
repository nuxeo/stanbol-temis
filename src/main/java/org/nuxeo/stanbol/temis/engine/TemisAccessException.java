package org.nuxeo.stanbol.temis.engine;

import org.nuxeo.stanbol.temis.impl.Fault;

/**
 * Exception thrown when encountering unexpected remote server fault on the Temis webservice.
 */
public class TemisAccessException extends Exception {

    private static final long serialVersionUID = 1L;

    protected Fault fault;

    public TemisAccessException(Fault fault) {
        super(fault.getMessage());
        this.fault = fault;
    }

}
