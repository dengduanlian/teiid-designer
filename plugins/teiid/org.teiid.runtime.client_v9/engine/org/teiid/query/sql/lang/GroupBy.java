/* Generated By:JJTree: Do not edit this line. GroupBy.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.teiid.query.sql.lang;

import java.util.ArrayList;
import java.util.List;
import org.teiid.designer.annotation.Since;
import org.teiid.designer.query.sql.lang.IGroupBy;
import org.teiid.designer.runtime.version.spi.TeiidServerVersion.Version;
import org.teiid.query.parser.LanguageVisitor;
import org.teiid.query.parser.TeiidParser;
import org.teiid.query.sql.symbol.ElementSymbol;
import org.teiid.query.sql.symbol.Expression;

/**
 *
 */
public class GroupBy extends SimpleNode implements IGroupBy<Expression, LanguageVisitor> {

    /** The set of expressions for the data elements to be group. */
    private List<Expression> symbols = new ArrayList<Expression>();

    @Since(Version.TEIID_8_5)
    private boolean rollup;

    /**
     * @param p
     * @param id
     */
    public GroupBy(TeiidParser p, int id) {
        super(p, id);
    }

    /**
     * Returns an ordered list of the symbols in the GROUP BY
     * @return List of {@link ElementSymbol}s
     */
    @Override
    public List<Expression> getSymbols() {
        return symbols;
    }

    /**
     * Adds a new symbol to the list of symbols.
     * @param symbol Symbol to add to GROUP BY
     */
    @Override
    public void addSymbol( Expression symbol ) {
        if(symbol != null) {
            symbols.add(symbol);
        }
    }

    @Override
    public int getCount() {
        return symbols.size();
    }

    /**
     * @param symbols the symbols to set
     */
    public void setSymbols(List<Expression> symbols) {
        this.symbols = symbols;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((this.symbols == null) ? 0 : this.symbols.hashCode());
        result = prime * result + (this.rollup ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        GroupBy other = (GroupBy)obj;
        if (this.symbols == null) {
            if (other.symbols != null) return false;
        } else if (!this.symbols.equals(other.symbols)) return false;

        if (this.rollup != other.rollup)
            return false;

        return true;
    }

    /** Accept the visitor. **/
    @Override
    public void acceptVisitor(LanguageVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public GroupBy clone() {
        GroupBy clone = new GroupBy(this.parser, this.id);

        if(getSymbols() != null)
            clone.setSymbols(cloneList(getSymbols()));

        clone.setRollup(isRollup());

        return clone;
    }
    
    /**
     * @return rollup
     */
    public boolean isRollup() {
        if (isTeiidVersionOrGreater(Version.TEIID_8_5))
            return rollup;

        return false;
    }

    /**
     * @param rollup
     */
    public void setRollup(boolean rollup) {
        if (isTeiidVersionOrGreater(Version.TEIID_8_5))
            this.rollup = rollup;
    }

}
/* JavaCC - OriginalChecksum=6a7cff5a6c710d93183af9e3561ec65a (do not edit this line) */
