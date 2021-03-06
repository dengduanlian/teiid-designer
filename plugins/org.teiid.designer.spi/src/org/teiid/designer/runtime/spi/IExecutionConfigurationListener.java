/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.runtime.spi;


/**
 * The <code>IExecutionConfigurationListener</code> interface defines the API for a server registry listener.
 *
 * @since 8.0
 */
public interface IExecutionConfigurationListener {

    /**
     * @param event the event being processed (never <code>null</code>)
     */
    void configurationChanged( ExecutionConfigurationEvent event );

}
