/* Generated By:JJTree: Do not edit this line. IfStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.teiid.query.sql.proc;

import org.teiid.designer.query.sql.proc.IIfStatement;
import org.teiid.query.parser.LanguageVisitor;
import org.teiid.query.parser.TeiidParser;
import org.teiid.query.sql.lang.Criteria;

/**
 *
 */
public class IfStatement extends Statement implements IIfStatement<LanguageVisitor> {

    // the IF block
    private Block ifBlock;
    
    // the ELSE block
    private Block elseBlock;
    
    // criteria on the if block
    private Criteria condition;

    /**
     * @param p
     * @param id
     */
    public IfStatement(TeiidParser p, int id) {
        super(p, id);
    }

    /**
     * Return the type for this statement, this is one of the types
     * defined on the statement object.
     * @return The statement type
     */
    @Override
    public StatementType getType() {
        return StatementType.TYPE_IF;
    }

    /**
     * Get the statement's IF block.
     * @return The IF <code>Block</code> object.
     */
    public Block getIfBlock() {
        return ifBlock;
    }
    
    /**
     * Set the statement's IF block.
     * @param block The IF <code>Block</code> object.
     */
    public void setIfBlock(Block block) {
        this.ifBlock = block;
    }

    /**
     * Get the statement's ELSE block.
     * @return The ELSE <code>Block</code> object.
     */
    public Block getElseBlock() {
        return elseBlock;
    }
    
    /**
     * Set the statement's ELSE block.
     * @param block The ELSE <code>Block</code> object.
     */
    public void setElseBlock(Block block) {
        elseBlock = block;
    }
    
    /**
     * Return a boolean indicating if the statement has an else block.
     * @return A boolean indicating if the statement has an else block
     */
    public boolean hasElseBlock() {
        return (elseBlock != null);
    }

    /**
     * Get the condition that determines which block needs to be executed.
     * @return The <code>Criteria</code> to determine block execution
     */
    public Criteria getCondition() {
        return condition;
    }
    
    /**
     * Set the condition that determines which block needs to be executed.
     * @param criteria The <code>Criteria</code> to determine block execution
     */
    public void setCondition(Criteria criteria) {
        this.condition = criteria;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((this.condition == null) ? 0 : this.condition.hashCode());
        result = prime * result + ((this.elseBlock == null) ? 0 : this.elseBlock.hashCode());
        result = prime * result + ((this.ifBlock == null) ? 0 : this.ifBlock.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        IfStatement other = (IfStatement)obj;
        if (this.condition == null) {
            if (other.condition != null) return false;
        } else if (!this.condition.equals(other.condition)) return false;
        if (this.elseBlock == null) {
            if (other.elseBlock != null) return false;
        } else if (!this.elseBlock.equals(other.elseBlock)) return false;
        if (this.ifBlock == null) {
            if (other.ifBlock != null) return false;
        } else if (!this.ifBlock.equals(other.ifBlock)) return false;
        return true;
    }

    /** Accept the visitor. **/
    @Override
    public void acceptVisitor(LanguageVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public IfStatement clone() {
        IfStatement clone = new IfStatement(this.parser, this.id);

        if(getCondition() != null)
            clone.setCondition(getCondition().clone());
        if(getIfBlock() != null)
            clone.setIfBlock(getIfBlock().clone());
        if(getElseBlock() != null)
            clone.setElseBlock(getElseBlock().clone());

        return clone;
    }

}
/* JavaCC - OriginalChecksum=bb19833978a016bb6733f82348868799 (do not edit this line) */