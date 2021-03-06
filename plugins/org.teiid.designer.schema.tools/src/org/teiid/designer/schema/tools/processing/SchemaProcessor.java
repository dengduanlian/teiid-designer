/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.schema.tools.processing;

import java.util.List;
import java.util.Map;

import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.XSDTypeDefinition;
import org.teiid.designer.schema.tools.model.schema.SchemaModel;
import org.teiid.designer.schema.tools.model.schema.SchemaObject;
import org.teiid.designer.schema.tools.processing.internal.ElementContentTraversalContext;
import org.teiid.designer.schema.tools.processing.internal.SchemaProcessorImpl;
/**
 *Derives a SchemaModel from one or more XSDSchema.  The SchemaModel 
 *produced will combine the elements and optionally the types defined
 *in all the provided XSDSchema into a model that creates relates 
 *parents and children within and across schemas. 
 *
 * @since 8.0
 */
public interface SchemaProcessor {

	/**
	 * Clears the results of any processing and resets the processor for
	 * additional processing with no relation to previous usage.
	 */
	public abstract void clear();

	/**
	 * Begins processing on the passed Schemas. Can be called repeatedly to add
	 * additional results to a previous run. The results of each run will
	 * produced relationships with the previous runs if they exist.
	 * 
	 * @param schemas
	 */
	public abstract void processSchemas(XSDSchema[] schemas) throws SchemaProcessingException;

	/**
	 * Convert the schemaURIs to an array of XSDSchema for the
	 * {@link SchemaProcessorImpl}
	 * 
	 */
	public abstract void processSchemaURIs(List schemaURIs) throws SchemaProcessingException;

	/**
	 * Returns a Map of Namespaces keyed by namespace prefix derived from the 
	 * schemas provided to the SchemaProcessor.
	 * @return - the namespace Map
	 */
	public abstract Map getNamespaces();
	
	/**
	 * Loads the SchemaProcessor with a Map of namespaces. 
	 */
	public abstract void setNamespaces(Map namespaces);

	/**
	 * Gets the SchemaModel derived from the schemas provided to the SchemaProcessor
	 * @return - the SchemaModel
	 */
	public abstract SchemaModel getSchemaModel();
	
	/**
	 * Configures the SchemaProcessor to represent Types Definitions as well as 
	 * Elements.  By default the SchemaProcessor does not represent Types, so it
	 *  is only required to call this method if you need Types. 
	 * @param representTypes True to make the SchemaProcessor represent Types
	 */
	public abstract void representTypes(boolean representTypes);

	public abstract void processType(XSDTypeDefinition type, ElementContentTraversalContext traverseCtx2, XSDSchema schema)
			throws SchemaProcessingException;

	public abstract void processElementText(SchemaObject element);

	public void processElement(XSDElementDeclaration elem, ElementContentTraversalContext traverseCtx, XSDSchema schema)
			throws SchemaProcessingException;
}
