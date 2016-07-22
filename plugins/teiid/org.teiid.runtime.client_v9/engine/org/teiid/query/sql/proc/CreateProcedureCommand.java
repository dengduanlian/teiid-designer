/* Generated By:JJTree: Do not edit this line. CreateProcedureCommand.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.teiid.query.sql.proc;

import java.util.Collections;
import java.util.List;
import org.teiid.designer.annotation.Since;
import org.teiid.designer.query.sql.lang.ICommand;
import org.teiid.designer.query.sql.proc.ICreateProcedureCommand;
import org.teiid.designer.runtime.version.spi.TeiidServerVersion.Version;
import org.teiid.query.parser.LanguageVisitor;
import org.teiid.query.parser.TeiidParser;
import org.teiid.query.sql.lang.Command;
import org.teiid.query.sql.symbol.ElementSymbol;
import org.teiid.query.sql.symbol.Expression;
import org.teiid.query.sql.symbol.GroupSymbol;

/**
 *
 */
@Since(Version.TEIID_8_0)
public class CreateProcedureCommand extends Command
    implements ICreateProcedureCommand<Block, GroupSymbol, Expression, LanguageVisitor> {

    // top level block for the procedure
    protected Block block;

    private List<? extends Expression> projectedSymbols;

    private List<? extends Expression> resultSetColumns;

    private GroupSymbol virtualGroup;

    private ElementSymbol returnVariable;

    private int updateType = ICommand.TYPE_UNKNOWN;

    /**
     * @param p
     * @param id
     */
    public CreateProcedureCommand(TeiidParser p, int id) {
        super(p, id);
    }

    /**
     * Return type of command to make it easier to build switch statements by command type.
     * @return The type of this command
     */
    @Override
    public int getType() {
        return TYPE_UPDATE_PROCEDURE;   
    }

    /**
     * @return the block
     */
    @Override
    public Block getBlock() {
        return block;
    }

    /**
     * @param block the block to set
     */
    @Override
    public void setBlock(Block block) {
        this.block = block;
    }

    @Override
    @Since(Version.TEIID_8_5)
    public boolean returnsResultSet() {
        if (isTeiidVersionOrGreater(Version.TEIID_8_5))
            return this.resultSetColumns != null && !this.resultSetColumns.isEmpty();

        return super.returnsResultSet();
    }

    @Override
    public List<? extends Expression> getResultSetColumns() {
        return resultSetColumns;
    }
    
    /**
     * @param resultSetColumns
     */
    public void setResultSetColumns(List<? extends Expression> resultSetColumns) {
        this.resultSetColumns = resultSetColumns;
    }

    /**
     * Get the ordered list of all elements returned by this query.  These elements
     * may be ElementSymbols or ExpressionSymbols but in all cases each represents a 
     * single column.
     * @return Ordered list of SingleElementSymbol
     */
    @Override
    public List<Expression> getProjectedSymbols(){
        if(this.projectedSymbols != null){
            return (List<Expression>) this.projectedSymbols;
        }
        //user may have not entered any query yet
        return Collections.EMPTY_LIST;
    }  

    /**
     * @param projSymbols
     */
    public void setProjectedSymbols(List<? extends Expression> projSymbols) {
        projectedSymbols = projSymbols;
    }

    /**
     * @return virtual group
     */
    public GroupSymbol getVirtualGroup() {
        return this.virtualGroup;
    }

    /**
     * @param virtualGroup
     */
    public void setVirtualGroup(GroupSymbol virtualGroup) {
        this.virtualGroup = virtualGroup;
    }

    /**
     * @return return variable
     */
    public ElementSymbol getReturnVariable() {
        return returnVariable;
    }

    /**
     * @param symbol
     */
    public void setReturnVariable(ElementSymbol symbol) {
        this.returnVariable = symbol;
    }

    /**
     * @return update type
     */
    public int getUpdateType() {
        return updateType;
    }

    /**
     * @param type
     */
    public void setUpdateType(int type) {
        if (isTeiidVersionOrGreater(Version.TEIID_8_5))
            //we select the count as the last operation
            this.resultSetColumns = getUpdateCommandSymbol();
        else
            this.resultSetColumns = Collections.emptyList();

        this.updateType = type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((this.block == null) ? 0 : this.block.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        CreateProcedureCommand other = (CreateProcedureCommand)obj;
        if (this.block == null) {
            if (other.block != null) return false;
        } else if (!this.block.equals(other.block)) return false;
        return true;
    }

    /** Accept the visitor. **/
    @Override
    public void acceptVisitor(LanguageVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public CreateProcedureCommand clone() {
        CreateProcedureCommand clone = new CreateProcedureCommand(this.parser, this.id);

        if(getBlock() != null)
            clone.setBlock(getBlock().clone());
        if(getSourceHint() != null)
            clone.setSourceHint(getSourceHint());
        if(getOption() != null)
            clone.setOption(getOption().clone());
        if (this.projectedSymbols != null)
            clone.projectedSymbols = cloneList(getProjectedSymbols());
        if (this.resultSetColumns != null)
            clone.resultSetColumns = cloneList(this.resultSetColumns);
        if (this.virtualGroup != null)
            clone.virtualGroup = this.virtualGroup.clone();
        if (this.returnVariable != null)
            clone.returnVariable = this.returnVariable;

        this.copyMetadataState(clone);
        return clone;
    }

}
/* JavaCC - OriginalChecksum=14790ffe7d56203cb640dd53367c0c33 (do not edit this line) */
