/*
 * JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.teiid.query.resolver.v85;

import org.teiid.designer.runtime.version.spi.ITeiidServerVersion;
import org.teiid.designer.runtime.version.spi.TeiidServerVersion.Version;
import org.teiid.query.resolver.v8.Test8XMLResolver;

/**
 *
 */
@SuppressWarnings( "javadoc" )
public class Test85XMLResolver extends Test8XMLResolver {

    protected Test85XMLResolver(Version teiidVersion) {
        super(teiidVersion);
    }

    public Test85XMLResolver() {
        this(Version.TEIID_8_5);
    }

}
