/*
 * JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.teiid.query.sql;

import org.teiid.designer.runtime.version.spi.ITeiidServerVersion;
import org.teiid.query.parser.ParseInfo;
import org.teiid.query.sql.lang.Criteria;
import org.teiid.query.sql.lang.LanguageObject;
import org.teiid.query.sql.lang.Query;
import org.teiid.query.sql.lang.Select;
import org.teiid.query.sql.proc.Statement;
import org.teiid.query.sql.symbol.Expression;
import org.teiid.query.sql.visitor.SQLStringVisitor;

/**
 *
 */
@SuppressWarnings( {"javadoc", "nls"} )
public abstract class AbstractTestSQLStringVisitor extends AbstractTest<LanguageObject> {

    /**
     * @param teiidVersion 
     */
    public AbstractTestSQLStringVisitor(ITeiidServerVersion teiidVersion) {
        super(teiidVersion);
    }

    protected void helpTest(String sql, LanguageObject expectedNode) {
        helpTest(sql, sql, expectedNode);
    }

    @Override
    protected void helpTest(String sql, String expectedSql, LanguageObject expectedNode) {
        helpTest(sql, expectedSql, expectedNode, new ParseInfo());
    }

    @Override
    protected void helpTest(String sql, String expectedSql, LanguageObject expectedNode, ParseInfo info) {
        String actualString = null;
        try {
            SQLStringVisitor visitor = new SQLStringVisitor(teiidVersion);
            actualString = visitor.returnSQLString(expectedNode);
        } catch (Throwable e) {
            fail(e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        assertEquals("SQL strings do not match: ", expectedSql, actualString);
    }

    @Override
    protected void helpTestLiteral(Boolean expected, Class<?> expectedType, String sql, String expectedSql) {
        Select select = getFactory().newSelect();
        select.addSymbol(getFactory().wrapExpression(getFactory().newConstant(expected, expectedType)));

        Query query = getFactory().newQuery();
        query.setSelect(select);

        helpTest(sql, expectedSql, query);
    }

    protected void helpCriteriaTest(String crit, String expectedString, Criteria expectedCrit) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void helpTestExpression(String sql, String expectedString, Expression expected) throws Exception {
        helpTest(sql, expectedString, expected);
    }

    @Override
    protected void helpStmtTest(String stmt, String expectedString, Statement expectedStmt) throws Exception {
        helpTest(stmt, expectedString, expectedStmt);
    }
}
