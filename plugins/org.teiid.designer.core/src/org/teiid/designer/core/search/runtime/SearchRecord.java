/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.core.search.runtime;

/**
 * SearchRecord
 *
 * @since 8.0
 */
public interface SearchRecord {

	/**
	 * Get the UUID of the entity
	 * @return the UUID of the entity
	 */
	String getUUID();

	/**
	 * Get type of the metadata record
	 * @return char representing type of the metadata record
	 */
	char getRecordType();
}
