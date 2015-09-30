/*
 * JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.teiid.query.metadata.v810;

import org.teiid.designer.runtime.version.spi.TeiidServerVersion.Version;
import org.teiid.query.metadata.v89.Test89MetadataValidator;

/**
 *
 */
@SuppressWarnings( "javadoc" )
public class Test810MetadataValidator extends Test89MetadataValidator {

    protected Test810MetadataValidator(Version teiidVersion) {
        super(teiidVersion);
    }

    public Test810MetadataValidator() {
        this(Version.TEIID_8_10);
    }
}
