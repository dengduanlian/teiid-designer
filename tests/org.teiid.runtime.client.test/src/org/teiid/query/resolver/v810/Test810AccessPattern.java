/*
 * JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.teiid.query.resolver.v810;

import org.teiid.designer.runtime.version.spi.TeiidServerVersion.Version;
import org.teiid.query.resolver.v89.Test89AccessPattern;

/**
 *
 */
@SuppressWarnings( "javadoc" )
public class Test810AccessPattern extends Test89AccessPattern {

    protected Test810AccessPattern(Version teiidVersion) {
        super(teiidVersion);
    }

    public Test810AccessPattern() {
        this(Version.TEIID_8_10);
    }
}
