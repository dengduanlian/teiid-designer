/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.transformation.ddl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.teiid.core.designer.ModelerCoreException;
import org.teiid.core.designer.util.CoreArgCheck;
import org.teiid.core.designer.util.CoreStringUtil;
import org.teiid.core.designer.util.ModelType;
import org.teiid.core.designer.util.StringConstants;
import org.teiid.core.designer.util.StringUtilities;
import org.teiid.designer.core.ModelerCore;
import org.teiid.designer.core.index.IndexConstants.INDEX_NAME;
import org.teiid.designer.core.util.ModelContents;
import org.teiid.designer.core.workspace.ModelResource;
import org.teiid.designer.core.workspace.ModelUtil;
import org.teiid.designer.core.workspace.ModelWorkspaceException;
import org.teiid.designer.extension.ExtensionPlugin;
import org.teiid.designer.extension.ModelExtensionAssistantAggregator;
import org.teiid.designer.extension.definition.ModelObjectExtensionAssistant;
import org.teiid.designer.extension.properties.ModelExtensionPropertyDefinition;
import org.teiid.designer.metamodels.relational.AccessPattern;
import org.teiid.designer.metamodels.relational.BaseTable;
import org.teiid.designer.metamodels.relational.Column;
import org.teiid.designer.metamodels.relational.DirectionKind;
import org.teiid.designer.metamodels.relational.ForeignKey;
import org.teiid.designer.metamodels.relational.Index;
import org.teiid.designer.metamodels.relational.NullableType;
import org.teiid.designer.metamodels.relational.PrimaryKey;
import org.teiid.designer.metamodels.relational.Procedure;
import org.teiid.designer.metamodels.relational.ProcedureParameter;
import org.teiid.designer.metamodels.relational.ProcedureUpdateCount;
import org.teiid.designer.metamodels.relational.SearchabilityType;
import org.teiid.designer.metamodels.relational.Table;
import org.teiid.designer.metamodels.relational.UniqueConstraint;
import org.teiid.designer.metamodels.relational.UniqueKey;
import org.teiid.designer.metamodels.relational.extension.RelationalModelExtensionAssistant;
import org.teiid.designer.metamodels.relational.extension.RestModelExtensionAssistant;
import org.teiid.designer.metamodels.relational.extension.RestModelExtensionConstants;
import org.teiid.designer.metamodels.relational.util.RelationalUtil;
import org.teiid.designer.metamodels.transformation.TransformationMappingRoot;
import org.teiid.designer.relational.RelationalConstants;
import org.teiid.designer.transformation.TransformationPlugin;
import org.teiid.designer.transformation.util.TransformationHelper;
import org.teiid.designer.type.IDataTypeManagerService.DataTypeName;
//import org.teiid.query.ui.sqleditor.component.QueryDisplayFormatter;

/**
 * Generator for converting a teiid xmi model into DDL
 */
public class TeiidModelToDdlGenerator implements TeiidDDLConstants, TeiidReservedConstants, RelationalConstants  {
	
	private StringBuilder ddlBuffer = new StringBuilder();

    private boolean includeTables = true;

    private boolean includeProcedures = true;
    
    private boolean includeFKs = true;
    
    private boolean isVirtual = false;
    
    private RelationalModelExtensionAssistant assistant;
    
    private List<IStatus> issues = new ArrayList<IStatus>();
    
    private Set<String> namespaces = new HashSet<String>();
    
    private ModelExtensionAssistantAggregator medAggregator = ExtensionPlugin.getInstance().getModelExtensionAssistantAggregator();

    private boolean ignoreTeiidProcedures = false;
	private boolean includeNIS;
    private boolean includeNativeType;
    
    
    /**
     * 
     */
	public TeiidModelToDdlGenerator() {
		this(true, true);
	}
	
    /**
     * Constructor that allows setting a flag that will prevent teiid-specific procedures to NOT have ddl generated for them.
     * 
     * 
     * @param ignoreTeiidProcedures
     */
	public TeiidModelToDdlGenerator(boolean ignoreTeiidProcedures) {
		super();
		
		this.ignoreTeiidProcedures = ignoreTeiidProcedures;
	}
	
    /**
     * Constructor that allows including NIS and/or Native Type
     * 
     * @param includeNIS
     * @param includeNativeType
     */
	public TeiidModelToDdlGenerator(boolean includeNIS, boolean includeNativeType) {
		super();
		
		this.includeNIS = includeNIS;
		this.includeNativeType = includeNativeType;
	}

	/**
	 * @param modelResource
	 * @return the generated DDL for the given model
	 * @throws ModelWorkspaceException
	 */
	public String generate(ModelResource modelResource) throws ModelWorkspaceException {
	    CoreArgCheck.isNotNull(modelResource);

		final ModelContents contents = ModelContents.getModelContents(modelResource);
		isVirtual = modelResource.getModelType().getValue() == ModelType.VIRTUAL;
		
		Collection<Index> indexes = getIndexes(modelResource);

		append(StringConstants.NEW_LINE);
		
		for( Object obj : contents.getAllRootEObjects() ) {
			String statement = getStatement((EObject)obj, indexes);
			if( ! StringUtilities.isEmpty(statement) ) {
				append(statement);
				append(StringConstants.NEW_LINE);
			}
		}
		if( ! namespaces.isEmpty() ) {
			for( String nsString : namespaces ) {
				// needs to look like this
				// SET NAMESPACE 'http://www.teiid.org/translator/excel/2014' AS teiid_excel;

				ddlBuffer.insert(0, nsString + NEW_LINE);
			}
			ddlBuffer.insert(0, NEW_LINE);
		}
		return ddlBuffer.toString();
	}
	
	public String getStatement(EObject eObj, Collection<Index> allIndexes) {
		if( eObj instanceof Table ) {
			
			Collection<Index> indexes = getIndexesForTable((Table)eObj, allIndexes);
			
			if( isVirtual ) {
				// generate DDL for a View including SQL statement
				if( ((Table)eObj).isSupportsUpdate() ) {
					// Need to process SELECT, INSERT, UPDATE, DELETE statements
					String select =  view((Table)eObj, indexes);
					String insert = insert((Table)eObj);
					String update = update((Table)eObj);
					String delete = delete((Table)eObj);
					StringBuilder sb = new StringBuilder(select);
					if( insert != null ) {
						sb.append(NEW_LINE).append(insert);
					}
					if( update != null ) {
						sb.append(NEW_LINE).append(update);
					}
					if( delete != null ) {
						sb.append(NEW_LINE).append(delete);
					}
					return sb.toString();
					
				} else {
					return view((Table)eObj, indexes);
				}
			} else {
				// Generate simple CREATE FOREIGN TABLE
				return table((Table)eObj, indexes);
			}
		} else if( eObj instanceof Procedure) {
			// Generate CREATE FOREIGN PROCEDURE 
			return procedure((Procedure)eObj);
		}
		
		return null;
	}
	
	private String getColumnDdl(Column col) {
        
        StringBuilder sb = new StringBuilder();

        sb.append(getName(col));
        sb.append(SPACE);

        String teiidDdlDataType = resolveExportedDataType(col.getType());
        if( teiidDdlDataType == null ) {
        	addIssue(IStatus.ERROR, "Error finding " + getName(col.getType()) + ".  Type set to 'string'"); //$NON-NLS-1$
        	teiidDdlDataType = DataTypeName.STRING.name();
        }
        sb.append(getColumnDatatypeDdl(teiidDdlDataType, col.getLength(), col.getPrecision(), col.getScale()));

        String properties = getColumnProperties(col);
        if (! StringUtilities.isEmpty(properties)) sb.append(SPACE).append(properties);

        String options = getColumnOptions(col);
        if( !StringUtilities.isEmpty(options) ) sb.append(SPACE).append(options);
        
		return sb.toString();
	}
	
	private String resolveExportedDataType(EObject dataTypeEObject) {
		String dataTypeName = ModelerCore.getBuiltInTypesManager().getName(dataTypeEObject);
		
        if( dataTypeName == null ) {
        	addIssue(IStatus.ERROR, "Error finding " + getName(dataTypeEObject) + ".  Type set to 'string'"); //$NON-NLS-1$
        	return DataTypeName.STRING.name();
        }
        
		if( dataTypeName.equalsIgnoreCase(DataTypeName.VARBINARY.name()) ) {
			return dataTypeName;
		}
		
		String runtimeTypeName = ModelerCore.getBuiltInTypesManager().getRuntimeTypeName(dataTypeEObject);
		
		if( runtimeTypeName == null) {
			// Check with 
			runtimeTypeName = ModelerCore.getDatatypeManager().getRuntimeTypeName(dataTypeEObject);
		}
		
		if( runtimeTypeName != null && runtimeTypeName.equalsIgnoreCase("XMLLITERAL")) {
			return DataTypeName.XML.name();
		}
		
		return runtimeTypeName;
	}
	
	private String getColumnDatatypeDdl(String name, int length, int precision, int scale) {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		
		final boolean isLengthType = ModelerCore.getTeiidDataTypeManagerService().isLengthDataType(name);
		final boolean isPrecisionType = ModelerCore.getTeiidDataTypeManagerService().isPrecisionDataType(name);
		final boolean isScaleType = ModelerCore.getTeiidDataTypeManagerService().isScaleDataType(name);
		
		if( isLengthType ) {
			if( length > 0 ) {
				sb.append(OPEN_BRACKET).append(length).append(CLOSE_BRACKET);
			}
		} else if( isPrecisionType && precision > 0 ) {
			sb.append(OPEN_BRACKET).append(precision);
			if( isScaleType && scale > 0 ) {
				sb.append(COMMA).append(SPACE).append(scale).append(CLOSE_BRACKET);
			} else {
				sb.append(CLOSE_BRACKET);
			}
		}
		return sb.toString();
	}
	
	private String getParameterDatatypeDdl(String name, int length, int precision, int scale) {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		
		final boolean isLengthType = ModelerCore.getTeiidDataTypeManagerService().isLengthDataType(name);
		final boolean isPrecisionType = ModelerCore.getTeiidDataTypeManagerService().isPrecisionDataType(name);
		final boolean isScaleType = ModelerCore.getTeiidDataTypeManagerService().isScaleDataType(name);
		
		if( isLengthType ) {
			if( length > 0 ) {
				sb.append(OPEN_BRACKET).append(length).append(CLOSE_BRACKET);
			}
		} else if( isPrecisionType && precision > 0 ) {
			sb.append(OPEN_BRACKET).append(precision);
			if( isScaleType && scale > 0 ) {
				sb.append(COMMA).append(SPACE).append(scale).append(CLOSE_BRACKET);
			} else {
				sb.append(CLOSE_BRACKET);
			}
		}
		return sb.toString();
	}
	
	private String getParameterDdl(ProcedureParameter param) {
        
        StringBuilder sb = new StringBuilder();

        if( param.getDirection().getValue() != DirectionKind.RETURN ) {
        	String directionStr = param.getDirection().getLiteral();
        	sb.append(directionStr).append(SPACE);
        } else {
            //So the exporter will treat OUT's as straight OUT parameters and if a RETURN parameter exists, 
            // then it'll be treated as an OUT parameter with a "result" added. (See below)
        	String directionStr = DirectionKind.OUT_LITERAL.getLiteral();
        	sb.append(directionStr).append(SPACE);
        }
        
        sb.append(getName(param));
        sb.append(SPACE);
        
        String teiidDdlDataType = resolveExportedDataType(param.getType());
        sb.append(getParameterDatatypeDdl(teiidDdlDataType, param.getLength(), param.getPrecision(), param.getScale()));
        
        if( param.getNullable().getValue() == NullableType.NO_NULLS ) {
        	sb.append(SPACE).append(NOT_NULL);
        }
        
        //So the exporter will treat OUT's as straight OUT parameters and if a RETURN parameter exists, 
        // then it'll be treated as an OUT parameter with a "result" added.
        if( param.getDirection().getValue() == DirectionKind.RETURN) {
        	sb.append(SPACE).append(RESULT);
        }
        
		return sb.toString();
	}
	
	private String getName(EObject eObj) {
		String emfName = ModelerCore.getModelEditor().getName(eObj);

		if( (emfName.startsWith(SQUOTE) && emfName.endsWith(SQUOTE)) || 
			(emfName.startsWith(DQUOTE) && emfName.endsWith(DQUOTE))  ) {
			return emfName; // already quoted
		}
		
		if( TeiidSQLConstants.isReservedWord(emfName) ) {
			emfName = DQUOTE + emfName + DQUOTE;
		}
		
		return emfName;
	}
	
	private String getDescription(EObject eObj) {
    	try {
			return ModelerCore.getModelEditor().getDescription(eObj);
		} catch (ModelerCoreException e) {
			addIssue(IStatus.ERROR, "Error finding description for " + getName(eObj), e); //$NON-NLS-1$
		}
    	
    	return null;
	}
	
    private void append(Object o) {
        ddlBuffer.append(o);
    }
	
    private String table(Table table, Collection<Index> tableIndexes) {
        if (! includeTables)
            return null;
        
        StringBuilder sb = new StringBuilder();
        
    	sb.append(CREATE_FOREIGN_TABLE).append(SPACE);

        sb.append(getName(table));
        sb.append(SPACE + OPEN_BRACKET);
		@SuppressWarnings("unchecked")
		List<Column> columns = table.getColumns();
		int nColumns = columns.size();
		int count = 0;
		for( Column col : columns ) {
			if( count == 0 ) sb.append(NEW_LINE);
			
			String columnStr = getColumnDdl(col);
			count++;
			sb.append(TAB).append(columnStr);
			
			if( count < nColumns ) sb.append(COMMA + NEW_LINE);
		}
	
		// Add PK/FK/UC's
		if( table instanceof BaseTable) {
			String constraints = getContraints((BaseTable)table, tableIndexes);
			if( ! StringUtilities.isEmpty(constraints) ) {
				sb.append(COMMA);
				sb.append(constraints);
			}
		}

		sb.append(NEW_LINE + CLOSE_BRACKET);
		
		String options = getTableOptions(table);
		if( !StringUtilities.isEmpty(options)) {
			sb.append(SPACE).append(options);
		}
		
		sb.append(SEMI_COLON);
		
		sb.append(NEW_LINE);

		return sb.toString();
    }
    
    private String view(Table table, Collection<Index> indexes) {
        if (! includeTables)
            return null;
        
        boolean isGlobalTempTable = false;
        
        StringBuilder sb = new StringBuilder();
        
    	try {
            if( hasOption(table, BASE_TABLE_EXT_PROPERTIES.VIEW_TABLE_GLOBAL_TEMP_TABLE)) {
            	String value = getOption(table, BASE_TABLE_EXT_PROPERTIES.VIEW_TABLE_GLOBAL_TEMP_TABLE);
            	if( value.toLowerCase().equals(Boolean.TRUE.toString()) ) {
            		isGlobalTempTable = true;
            	}
            }
		} catch (Exception e) {
			addIssue(IStatus.ERROR, "Error finding options for " + getName(table), e); //$NON-NLS-1$
		}

		// generate DDL for a Table
    	if( isGlobalTempTable ) {
    		sb.append(CREATE_GLOBAL_TEMPORARY_TABLE).append(SPACE);
    	} else {
    		// generate DDL for a View including SQL statement
    		sb.append(CREATE_VIEW).append(SPACE);
    	}

        sb.append(getName(table));
        sb.append(SPACE + OPEN_BRACKET);
		@SuppressWarnings("unchecked")
		List<Column> columns = table.getColumns();
		int nColumns = columns.size();
		int count = 0;
		for( Column col : columns ) {
			if( count == 0 ) sb.append(NEW_LINE);
			
			String columnStr = getColumnDdl(col);
			count++;
			sb.append(TAB).append(columnStr);
			
			if( count < nColumns ) sb.append(COMMA + NEW_LINE);

		}
		
		// Add PK/FK/UC's
		if( table instanceof BaseTable) {
			String constraints = getContraints((BaseTable)table, indexes);
			if( !StringUtilities.isEmpty(constraints) ) {
				sb.append(COMMA);
				sb.append(constraints);
			}
		}
		
		sb.append(NEW_LINE + CLOSE_BRACKET);
		
		String options = getTableOptions(table);
		if( !StringUtilities.isEmpty(options)) {
			sb.append(SPACE).append(options);
		}
		
		if( !isGlobalTempTable ) {
			TransformationMappingRoot tRoot = (TransformationMappingRoot)TransformationHelper.getTransformationMappingRoot(table);
			String sqlString = TransformationHelper.getSelectSqlUserString(tRoot);
			if( sqlString != null ) {
	//			QueryDisplayFormatter formatter = new QueryDisplayFormatter(sqlString);
	//			String formatedSQL = formatter.getFormattedSql();
	//			sb.append(SPACE).append(NEW_LINE + Reserved.AS).append(NEW_LINE + TAB).append(formatedSQL);
				sb.append(SPACE).append(NEW_LINE + Reserved.AS).append(NEW_LINE + TAB).append(sqlString);
			}
		}
		
		sb.append(SEMI_COLON + NEW_LINE);
		
		return sb.toString();
    }
    
    
    private String insert(Table table) {
        if (! includeTables)
            return null;
        
		TransformationMappingRoot tRoot = (TransformationMappingRoot)TransformationHelper.getTransformationMappingRoot(table);
		String sqlString = TransformationHelper.getInsertSqlUserString(tRoot);
		
		if( StringUtilities.isEmpty(sqlString) ) return null;
        
        StringBuilder sb = new StringBuilder();
        
    	sb.append(CREATE_TRIGGER_ON).append(SPACE);

        sb.append(getName(table));
        
        sb.append(SPACE).append(INSTEAD_OF).append(SPACE).append(INSERT).append(SPACE).append(AS);
        
        sb.append(SPACE).append(NEW_LINE + TAB).append(sqlString);
        
		sb.append(SEMI_COLON + NEW_LINE);
        
        return sb.toString();
    }
    
    private String update(Table table) {
        if (! includeTables)
            return null;
        
		TransformationMappingRoot tRoot = (TransformationMappingRoot)TransformationHelper.getTransformationMappingRoot(table);
		String sqlString = TransformationHelper.getUpdateSqlUserString(tRoot);
		
		if( StringUtilities.isEmpty(sqlString) ) return null;
        
        StringBuilder sb = new StringBuilder();
        
    	sb.append(CREATE_TRIGGER_ON).append(SPACE);

        sb.append(getName(table));
        
        sb.append(SPACE).append(INSTEAD_OF).append(SPACE).append(UPDATE).append(SPACE).append(AS);
        
        sb.append(SPACE).append(NEW_LINE + TAB).append(sqlString);
        
		sb.append(SEMI_COLON + NEW_LINE);
        
        return sb.toString();
    }
    
    private String delete(Table table) {
        if (! includeTables)
            return null;
        
		TransformationMappingRoot tRoot = (TransformationMappingRoot)TransformationHelper.getTransformationMappingRoot(table);
		String sqlString = TransformationHelper.getDeleteSqlUserString(tRoot);
		
		if( StringUtilities.isEmpty(sqlString) ) return null;
        
        StringBuilder sb = new StringBuilder();
        
    	sb.append(CREATE_TRIGGER_ON).append(SPACE);

        sb.append(getName(table));
        
        sb.append(SPACE).append(INSTEAD_OF).append(SPACE).append(DELETE).append(SPACE).append(AS);
        
        sb.append(SPACE).append(NEW_LINE + TAB).append(sqlString);
        
		sb.append(SEMI_COLON + NEW_LINE);
        
        return sb.toString();
    }
    
    /*
     * 
	 *   Source Procedure ("CREATE FOREIGN PROCEDURE") - a stored procedure in source
	 *   Source Function ("CREATE FOREIGN FUNCTION") - A function that is supported by the source, where Teiid will pushdown to source instead of evaluating in Teiid engine
	 *   Virtual Procedure ("CREATE VIRTUAL PROCEDURE") - Similar to stored procedure, however this is defined using the Teiid's Procedure language and evaluated in the Teiid's engine.
	 *   Function/UDF ("CREATE VIRTUAL FUNCTION") - A user defined function, that can be defined using the Teiid procedure language or can have the implementation defined using a JAVA Class.
     */
    private String procedure(Procedure procedure) {
        if (! includeProcedures)
            return null;
        
        if( ignoreTeiidProcedures && isTeiidProcedure(procedure.getName()) ) return null;
        
        
        StringBuilder sb = new StringBuilder();
        boolean isFunction = procedure.isFunction();

		// generate DDL for a Table
        if( isFunction ) {
        	if( isVirtual ) sb.append(CREATE_VIRTUAL_FUNCTION).append(SPACE);
        	else sb.append(CREATE_FOREIGN_FUNCTION).append(SPACE);
        } else {
        	if( isVirtual ) sb.append(CREATE_VIRTUAL_PROCEDURE).append(SPACE);
        	else sb.append(CREATE_FOREIGN_PROCEDURE).append(SPACE);
        }
        sb.append(getName(procedure));
        sb.append(SPACE + OPEN_BRACKET);
		@SuppressWarnings("unchecked")
		List<ProcedureParameter> params = procedure.getParameters();
		int nParams = params.size();
		int count = 0;
		
		for( ProcedureParameter param : params ) {
			if( count == 0 ) sb.append(NEW_LINE);
			
			String paramStr = getParameterDdl(param);
			count++;
			sb.append(TAB).append(paramStr);
			
			String options = getOptions(param);
			if( !StringUtilities.isEmpty(options)) {
				sb.append(SPACE).append(options);
			}
			
			if( count < nParams ) sb.append(COMMA + NEW_LINE);
		}
		
		sb.append(NEW_LINE + CLOSE_BRACKET);
		
		// Depending on the procedure type, need to append either one of the following:
		//   > returns datatype
		//   > returns a result set, either named or not
		//   > an AS <SQL STATEMENT> if a virtual procedure
		//   > ???
	
		// Add the RETURNS clause to handle the result set
		// CREATE VIRTUAL PROCEDURE testProc (p1 string(4000)) RETURNS TABLE ( xml_out xml)
		// CREATE VIRTUAL PROCEDURE getTweets(query varchar) RETURNS (created_on varchar(25), from_user varchar(25), to_user varchar(25))
		// CREATE FOREIGN PROCEDURE func (x integer, y integer) returns table (z integer);
		// CREATE FOREIGN PROCEDURE func (x integer, y integer) returns integer;
	    // CREATE VIRTUAL FUNCTION celsiusToFahrenheit(celsius decimal) RETURNS decimal OPTIONS (JAVA_CLASS 'org.something.TempConv',  JAVA_METHOD 'celsiusToFahrenheit');
	    // CREATE VIRTUAL FUNCTION sumAll(arg integer) RETURNS integer OPTIONS (JAVA_CLASS 'org.something.SumAll',  JAVA_METHOD 'addInput', AGGREGATE 'true', VARARGS 'true', "NULL-ON-NULL" 'true');
		
		if( procedure.getResult() != null ) {
			sb.append(SPACE + RETURNS);
			// Get options for RETURNS type
			String options = getOptions(procedure.getResult());
			if( !StringUtilities.isEmpty(options)) {
				sb.append(SPACE).append(options);
			}
			
			sb.append(NEW_LINE + TAB + TABLE + SPACE);
			
			sb.append(OPEN_BRACKET);
			
			count = 0;
			int nCols = procedure.getResult().getColumns().size();
			for( Object col : procedure.getResult().getColumns() ) {
				if( count == 0 ) sb.append(NEW_LINE);
				
				Column nextCol = (Column)col;
				count++;
				String columnStr = getColumnDdl(nextCol);
				sb.append(TAB + TAB).append(columnStr);
				if( count < nCols ) sb.append(COMMA + NEW_LINE);

			}
			sb.append(NEW_LINE + CLOSE_BRACKET);
		}
		
		String options = getProcedureOptions(procedure);
		if( !StringUtilities.isEmpty(options)) {
			sb.append(SPACE).append(options);
		}


		if( isVirtual && !isFunction ) {
			TransformationMappingRoot tRoot = (TransformationMappingRoot)TransformationHelper.getTransformationMappingRoot(procedure);
			String existingSQL = TransformationHelper.getSelectSqlString(tRoot);
			if( StringUtilities.isEmpty(existingSQL) ) {
				existingSQL = "<SQL UNDEFINED>";
			}
			String sqlString = existingSQL.replace(CREATE_VIRTUAL_PROCEDURE, StringConstants.EMPTY_STRING);
			
			if( sqlString != null ) {
				if( sqlString.indexOf('\n') == 0 ) {
					sqlString = sqlString.replace(StringConstants.NEW_LINE, StringConstants.EMPTY_STRING);
				}
				sb.append(NEW_LINE + TAB).append(Reserved.AS).append(NEW_LINE).append(sqlString);
				if( ! sqlString.endsWith(SEMI_COLON)) sb.append(SEMI_COLON);
				sb.append(NEW_LINE);
			}
		} else {
			sb.append(SEMI_COLON + NEW_LINE);
		}

		return sb.toString();
    }

    private String getColumnProperties(Column col) {
        StringBuffer sb = new StringBuffer();

        //
        // NULLABLE / NOT NULL
        //
        NullableType nullableType = col.getNullable();
        if (nullableType.equals(NullableType.NO_NULLS_LITERAL))
            sb.append(NOT_NULL).append(SPACE);

        //
        // DEFAULT
        //
        String defaultValue = col.getDefaultValue();
        if (!StringUtilities.isEmpty(defaultValue)) {
        	defaultValue = convertDefaultValue(defaultValue);
        	defaultValue = ensureSingleQuotedString(defaultValue);
            sb.append(TeiidSQLConstants.Reserved.DEFAULT).append(SPACE).append(defaultValue).append(SPACE);
        }

        //
        // AUTO_INCREMENT
        //
        boolean autoIncremented = col.isAutoIncremented();
        if (autoIncremented)
            sb.append(AUTO_INCREMENT).append(SPACE);

        if (col.getOwner() instanceof BaseTable) {
//            BaseTable table = (BaseTable) col.getOwner();
//
//            //
//            // PRIMARY KEY
//            //
//            PrimaryKey key = table.getPrimaryKey();
//            if (key != null) {
//                @SuppressWarnings("rawtypes")
//				EList columns = key.getColumns();
//                if (columns != null && columns.contains(col))
//                    sb.append(PRIMARY_KEY).append(SPACE);
//            }
//
//            //
//            // UNIQUE
//            //
//            EList<UniqueConstraint> uniqueConstraints = table.getUniqueConstraints();
//            if (uniqueConstraints != null && ! uniqueConstraints.isEmpty()) {
//                for (UniqueConstraint uc : uniqueConstraints) {
//                    if (uc.getColumns().contains(col)) {
//                        sb.append(TeiidSQLConstants.Reserved.UNIQUE).append(SPACE);
//                        break; // Don't care if column is in more than 1 unique constraint
//                    }
//                }
//            }

            //
            // INDEX
            //
//            if (! col.getIndexes().isEmpty())
//                sb.append(TeiidSQLConstants.NonReserved.INDEX).append(SPACE);
        }

        return sb.toString().trim();
    }
    
    public String ensureSingleQuotedString(String input) {
    	if( StringUtilities.isSingleQuoted(input) ) {
    		return input;
    	} else {
    		String copyOfInput = input;
    		copyOfInput.replace("\'", "\\\\'");
    		StringBuilder result = new StringBuilder();
    		result.append(SQUOTE + copyOfInput + SQUOTE);
    		return result.toString();
    	}
    }
    
    private static String convertDefaultValue(String originalValue) {
    	String removedSQuotes = null;
    	if( !StringUtilities.isSingleQuoted(originalValue )) {
    		removedSQuotes = originalValue;
    	} else {
    		int len = originalValue.length();
    		removedSQuotes = originalValue.substring(1, len-1);
    	}
		if( removedSQuotes.contains("\'")) {
			StringBuilder sb = new StringBuilder();
			for( char ch : removedSQuotes.toCharArray() ) {
				boolean addedQuote = false;
				if( ch == '\'') {
					sb.append('\'');
					addedQuote = true;
				}
				sb.append(ch);
			}
			return sb.toString();
		}
    	
    	return removedSQuotes;
    	
    }

    private String getColumnOptions(Column col) {
    	OptionsStatement options = new OptionsStatement();
    	if( this.includeNIS ) {
    		options.add(NAMEINSOURCE, col.getNameInSource(), null);
    	}
    	if( this.includeNativeType ) {
    		options.add(NATIVE_TYPE, col.getNativeType(), null);
    	}
    	options.add(CASE_SENSITIVE, Boolean.toString(col.isCaseSensitive()), Boolean.TRUE.toString());
    	options.add(SELECTABLE, Boolean.toString(col.isSelectable()), Boolean.TRUE.toString());
    	options.add(UPDATABLE, Boolean.toString(col.isUpdateable()), Boolean.TRUE.toString());
    	options.add(SIGNED, Boolean.toString(col.isSigned()), Boolean.TRUE.toString());
    	options.add(CURRENCY, Boolean.toString(col.isCurrency()), Boolean.FALSE.toString());
    	options.add(FIXED_LENGTH, Boolean.toString(col.isFixedLength()), Boolean.FALSE.toString());
    	
    	// DISTINCT VALUE COUNT
    	int distinctValueCt = col.getDistinctValueCount();

		if( distinctValueCt > -1 ) {
			options.add(DISTINCT_VALUES, Integer.toString(distinctValueCt), Integer.toString(0));
		} else if( distinctValueCt < -1 ) {
    		Integer obj = new Integer(distinctValueCt);
    		final float floatValue = Float.intBitsToFloat(obj & 0x7fffffff);
    		DecimalFormat myFormatter = new DecimalFormat("###");
    		String output = myFormatter.format(floatValue);
    		
			options.add(DISTINCT_VALUES, output, Integer.toString(0));
		}
    	// NULL VALUE COUNT
    	int nullValueCt = col.getNullValueCount();

		if( nullValueCt > -1 ) {
			options.add(NULL_VALUE_COUNT, Integer.toString(nullValueCt), Integer.toString(0));
		} else if( distinctValueCt < -1 ) {
    		Integer obj = new Integer(nullValueCt);
    		final float floatValue = Float.intBitsToFloat(obj & 0x7fffffff);
    		DecimalFormat myFormatter = new DecimalFormat("###");
    		String output = myFormatter.format(floatValue);
    		
			options.add(NULL_VALUE_COUNT, output, Integer.toString(0));
		}
    	
    	String desc = getDescription(col);
    	if( !StringUtilities.isEmpty(desc) ) {
    		options.add(ANNOTATION, desc, EMPTY_STRING);
    	}
    	if( !col.getSearchability().equals(SearchabilityType.SEARCHABLE) ) {
    		options.add(SEARCHABLE, col.getSearchability().getLiteral(), SearchabilityType.SEARCHABLE_LITERAL.toString());
    	}
    	
    	// Need to check with other assistants too
    	try {
			Map<String, String> props = getOptionsForObject(col);
			for( String key : props.keySet() ) {
				String value = props.get(key);
				options.add(key, value, null);
			}
		} catch (Exception e) {
			addIssue(IStatus.ERROR, "Error finding options for " + getName(col), e); //$NON-NLS-1$
		}

    	return options.toString();
    }
    
    private Map<String, String> getParameterOptions(ProcedureParameter parameter) {
    	Map<String, String> options = new HashMap<String, String>();
    	if( this.includeNIS && parameter.getNameInSource() != null) {
    		options.put(NAMEINSOURCE, parameter.getNameInSource());
    	}
    	
    	if( this.includeNativeType ) {
    		options.put(NATIVE_TYPE, parameter.getNativeType());
    	}
    	return options;
    }
    
    private Map<String, String> getPKOptions(PrimaryKey pk) {
    	Map<String, String> options = new HashMap<String, String>();
    	if( this.includeNIS && pk.getNameInSource() != null ) {
    		options.put(NAMEINSOURCE, pk.getNameInSource());
    	}
    	return options;
    }
    
    private Map<String, String> getUCOptions(UniqueConstraint uc) {
    	Map<String, String> options = new HashMap<String, String>();
    	if( this.includeNIS && uc.getNameInSource() != null) {
    		options.put(NAMEINSOURCE, uc.getNameInSource());
    	}
    	return options;
    }
    
    private Map<String, String> getFKOptions(ForeignKey fk) {
    	Map<String, String> options = new HashMap<String, String>();
    	if( this.includeNIS && fk.getNameInSource() != null) {
    		options.put(NAMEINSOURCE, fk.getNameInSource());
    	}
    	return options;
    }
    
    private Map<String, String> getAPOptions(AccessPattern ap) {
    	Map<String, String> options = new HashMap<String, String>();
    	if( this.includeNIS && ap.getNameInSource() != null) {
    		options.put(NAMEINSOURCE, ap.getNameInSource());
    	}
    	return options;
    }
    
    private Map<String, String> getIndexOptions(Index ap) {
    	Map<String, String> options = new HashMap<String, String>();
    	if( this.includeNIS && ap.getNameInSource() != null) {
    		options.put(NAMEINSOURCE, ap.getNameInSource());
    	}
    	return options;
    }
    
    private String getOptions(EObject eobject) {
    	OptionsStatement options = new OptionsStatement();
    	
    	String desc = getDescription(eobject);

    	if( !StringUtilities.isEmpty(desc) ) {
    		options.add(ANNOTATION, desc, EMPTY_STRING);
    	}
    	
    	// Need to check with other assistants too
    	try {
			Map<String, String> props = getOptionsForObject(eobject);
			for( String key : props.keySet() ) {
				String value = props.get(key);
				options.add(key, value, null);
			}
		} catch (Exception e) {
			addIssue(IStatus.ERROR,  "Error finding options for " + getName(eobject), e); //$NON-NLS-1$
		}
    	
    	return options.toString();
    }
    
    private String getContraints(BaseTable table, Collection<Index> indexes) {
    	StringBuffer sb = new StringBuffer();
    	
		boolean hasPK = table.getPrimaryKey() != null;
		boolean hasFKs = !table.getForeignKeys().isEmpty();
		boolean hasAPs = !table.getAccessPatterns().isEmpty();
		boolean hasIndexes = !indexes.isEmpty();
		
		int nColumns = 0;
		int count = 0;

		Collection<UniqueConstraint> uniqueConstraints = getUniqueUniqueContraints(table);
		boolean hasUCs = uniqueConstraints.size() > 0;
		
		if( hasPK ) {
			PrimaryKey pk = table.getPrimaryKey();
			// CONSTRAINT PK_ACCOUNTHOLDINGS PRIMARY KEY(TRANID),
			String pkName = getName(pk);
			StringBuilder theSB = new StringBuilder(NEW_LINE + TAB + CONSTRAINT + SPACE + pkName + SPACE + PRIMARY_KEY);
			nColumns = pk.getColumns().size();
			count = 0;
			for( Object col : pk.getColumns() ) {
				count++;
				if( count == 1 ) theSB.append(OPEN_BRACKET);
				theSB.append(getName((EObject)col));
				if( count < nColumns ) theSB.append(COMMA + SPACE);
				else theSB.append(CLOSE_BRACKET);
			}
			
	    	String options = getOptions(pk);
			if( !StringUtilities.isEmpty(options)) {
				theSB.append(SPACE).append(options);
			}
			
			sb.append(theSB.toString());
			
			if( (hasFKs && includeFKs) || hasUCs || hasAPs || hasIndexes) sb.append(COMMA);
		}
		
		// FK
		// CONSTRAINT CUSTOMER_ACCOUNT_FK FOREIGN KEY(CUSTID) REFERENCES ACCOUNT (CUSTID)
		if( hasFKs && includeFKs) {
			int nFKs = table.getForeignKeys().size();
			int countFK = 0;
			for( Object obj : table.getForeignKeys()) {
				countFK++;
				ForeignKey fk = (ForeignKey)obj;
				String fkName = getName(fk);
				StringBuilder theSB = new StringBuilder(NEW_LINE + TAB + CONSTRAINT + SPACE + fkName + SPACE + FOREIGN_KEY);
				nColumns = fk.getColumns().size();
				count = 0;
				for( Object col : fk.getColumns() ) {
					count++;
					if( count == 1 ) theSB.append(OPEN_BRACKET);
					theSB.append(getName((EObject)col));
					if( count < nColumns ) theSB.append(COMMA + SPACE);
					else theSB.append(CLOSE_BRACKET);
				}
				// REFERENCES
				if( fk.getTable() != null ) {
					UniqueKey uk = fk.getUniqueKey();
					BaseTable fkTableRef = (BaseTable)uk.getTable();
					
					String fkTableRefName = getName(fkTableRef);
					theSB.append(SPACE).append(REFERENCES).append(SPACE).append(fkTableRefName);
					if( uk instanceof UniqueConstraint ) {
						// Unique Constraint
						UniqueConstraint ucRef = fkTableRef.getUniqueConstraints().get(0);
						nColumns = ucRef.getColumns().size();
						count = 0;
						for( Object col : ucRef.getColumns() ) {
							count++;
							if( count == 1 ) theSB.append(OPEN_BRACKET);
							theSB.append(getName((EObject)col));
							if( count < nColumns ) theSB.append(COMMA + SPACE);
							else theSB.append(CLOSE_BRACKET);
						}
						// TODO: Not sure how to handle the case where there are multiple UC's.
						
					} else { 
						// Primary Key
						PrimaryKey pkRef = fkTableRef.getPrimaryKey();
						nColumns = pkRef.getColumns().size();
						count = 0;
						for( Object col : pkRef.getColumns() ) {
							count++;
							if( count == 1 ) theSB.append(OPEN_BRACKET);
							theSB.append(getName((EObject)col));
							if( count < nColumns ) theSB.append(COMMA + SPACE);
							else theSB.append(CLOSE_BRACKET);
						}
					}

				}
		    	String options = getOptions(fk);
				if( !StringUtilities.isEmpty(options)) {
					theSB.append(SPACE).append(options);
				}
				if( countFK < nFKs ) theSB.append(COMMA);
				sb.append(theSB.toString());

			}
			if( hasUCs || hasAPs || hasIndexes) sb.append(COMMA);
		}
		// UC's
		// CONSTRAINT PK_ACCOUNTHOLDINGS UNIQUE(TRANID)
		if( hasUCs ) {
			int nUCs = uniqueConstraints.size();
			int ucCount = 0;
			for( Object obj: uniqueConstraints ) {
				ucCount++;
				UniqueConstraint uc = (UniqueConstraint)obj;
				String name = getName(uc);

				StringBuilder theSB = new StringBuilder(NEW_LINE + TAB + CONSTRAINT + SPACE + name + SPACE + UNIQUE);
				nColumns = uc.getColumns().size();
				count = 0;
				for( Object col : uc.getColumns() ) {
					count++;
					if( count == 1 ) theSB.append(OPEN_BRACKET);
					theSB.append(getName((EObject)col));
					if( count < nColumns ) theSB.append(COMMA + SPACE);
					else theSB.append(CLOSE_BRACKET);
				}
				
		    	String options = getOptions(uc);
				if( !StringUtilities.isEmpty(options)) {
					theSB.append(SPACE).append(options);
				}
				
				if( ucCount < nUCs ) theSB.append(COMMA);
				sb.append(theSB.toString());
			}
			
			if( hasAPs || hasIndexes) sb.append(COMMA);
		}
		
		if( hasAPs ) {
			int nAPs = table.getAccessPatterns().size();
			int apCount = 0;
			for( Object obj: table.getAccessPatterns() ) {
				apCount++;
				AccessPattern ap = (AccessPattern)obj;
				String name = getName(ap);

				StringBuilder theSB = new StringBuilder(NEW_LINE + TAB + CONSTRAINT + SPACE + name + SPACE + ACCESSPATTERN);
				nColumns = ap.getColumns().size();
				count = 0;
				for( Object col : ap.getColumns() ) {
					count++;
					if( count == 1 ) theSB.append(OPEN_BRACKET);
					theSB.append(getName((EObject)col));
					if( count < nColumns ) theSB.append(COMMA + SPACE);
					else theSB.append(CLOSE_BRACKET);
				}
				
		    	String options = getOptions(ap);
				if( !StringUtilities.isEmpty(options)) {
					theSB.append(SPACE).append(options);
				}
				
				if( apCount < nAPs ) theSB.append(COMMA);
				sb.append(theSB.toString());
			}
			
			if( hasIndexes) sb.append(COMMA);
		}
		
		if( hasIndexes ) {
			int nIndexes = indexes.size();
			int indexCount = 0;
			for( Index index: indexes ) {
				indexCount++;
				String name = getName(index);

				StringBuilder theSB = new StringBuilder(NEW_LINE + TAB + CONSTRAINT + SPACE + name + SPACE + INDEX);
				nColumns = index.getColumns().size();
				count = 0;
				for( Object col : index.getColumns() ) {
					count++;
					if( count == 1 ) theSB.append(OPEN_BRACKET);
					theSB.append(getName((EObject)col));
					if( count < nColumns ) theSB.append(COMMA + SPACE);
					else theSB.append(CLOSE_BRACKET);
				}
				
		    	String options = getOptions(index);
				if( !StringUtilities.isEmpty(options)) {
					theSB.append(SPACE).append(options);
				}
				
				if( indexCount < nIndexes ) theSB.append(COMMA);
				sb.append(theSB.toString());
			}
		}
		
		return sb.toString();
    }
    
    private String getTableOptions(Table table) {
    	OptionsStatement options = new OptionsStatement();

    	if( this.includeNIS ) {
    		options.add(NAMEINSOURCE, table.getNameInSource(), null);
    	}
    	options.add(MATERIALIZED, Boolean.toString(table.isMaterialized()), Boolean.FALSE.toString());
    	options.add(UPDATABLE, Boolean.toString(table.isSupportsUpdate()), Boolean.FALSE.toString());
    	if( table.getCardinality() != 0 ) {
    		int cardValue = table.getCardinality();

    		if( cardValue > -1 ) {
    			options.add(CARDINALITY, Integer.toString(table.getCardinality()), Integer.toString(0));
    		} else if( cardValue < -1) {
        		Integer obj = new Integer(cardValue);
        		final float floatValue = Float.intBitsToFloat(obj & 0x7fffffff);
        		DecimalFormat myFormatter = new DecimalFormat("###");
        		String output = myFormatter.format(floatValue);
        		
    			options.add(CARDINALITY, output, Integer.toString(0));
    		}
    	}
    	if( table.getMaterializedTable() != null ) {
    		try {
    			Table matTable = table.getMaterializedTable();
				String tableName = matTable.getName();
				ModelResource mr = ModelUtil.getModel(matTable);
				String modelName = ModelUtil.getName(mr);
				options.add(MATERIALIZED_TABLE, modelName + StringConstants.DOT + tableName, null);
			} catch (ModelWorkspaceException e) {
				addIssue(IStatus.ERROR, "Error finding model for materialized table " + getName(table), e); //$NON-NLS-1$
			}
    	}
    	
    	// Need to check with other assistants too
    	try {
			Map<String, String> props = getOptionsForObject(table);
			for( String key : props.keySet() ) {
				if( key.equals(BASE_TABLE_EXT_PROPERTIES.VIEW_TABLE_GLOBAL_TEMP_TABLE) ) continue;
				String value = props.get(key);
				options.add(key, value, null);
			}
		} catch (Exception e) {
			addIssue(IStatus.ERROR, "Error finding options for " + getName(table), e); //$NON-NLS-1$
		}
    	
    	String desc = getDescription(table);
    	if( !StringUtilities.isEmpty(desc) ) {
    		options.add(ANNOTATION, desc, null);
    	}

    	return options.toString();
    }
    
    private Map<String, String> getOptionsForObject(EObject modelObject) throws Exception {
    	Map<String, String> options = new HashMap<String, String>();
    	
    	if( modelObject instanceof ProcedureParameter ) {
        	options.putAll( getParameterOptions((ProcedureParameter)modelObject));
    	} else if( modelObject instanceof PrimaryKey ) {
        	options.putAll( getPKOptions((PrimaryKey)modelObject));
    	} else if( modelObject instanceof UniqueConstraint ) {
        	options.putAll( getUCOptions((UniqueConstraint)modelObject));
    	} else if( modelObject instanceof ForeignKey ) {
        	options.putAll( getFKOptions((ForeignKey)modelObject));
    	} else if( modelObject instanceof AccessPattern ) {
        	options.putAll( getAPOptions((AccessPattern)modelObject));
    	}  else if( modelObject instanceof Index ) {
        	options.putAll( getIndexOptions((Index)modelObject));
    	} 

    	Collection<String> extensionNamespaces = medAggregator.getSupportedNamespacePrefixes(modelObject);
    	for( String ns : extensionNamespaces ) {
    		ModelObjectExtensionAssistant assistant = medAggregator.getModelObjectExtensionAssistant(ns);
    		if( assistant != null ) {
    			Collection<ModelExtensionPropertyDefinition> defns = assistant.getPropertyDefinitions(modelObject);
    			
    			if( defns.isEmpty()) continue;

    			// If relational, we're handling this via getPropetyValue()...
    			if(ns.equals(RELATIONAL_PREFIX)) {
    				addRelationalOption(options, BASE_TABLE_EXT_PROPERTIES.NATIVE_QUERY, modelObject, assistant);
    				addRelationalOption(options, BASE_TABLE_EXT_PROPERTIES.ALLOW_MATVIEW_MANAGEMENT, modelObject, assistant);
    				addRelationalOption(options, BASE_TABLE_EXT_PROPERTIES.MATVIEW_STATUS_TABLE, modelObject, assistant);
    				addRelationalOption(options, BASE_TABLE_EXT_PROPERTIES.MATVIEW_BEFORE_LOAD_SCRIPT, modelObject, assistant);
    				addRelationalOption(options, BASE_TABLE_EXT_PROPERTIES.MATVIEW_LOAD_SCRIPT, modelObject, assistant);
    				addRelationalOption(options, BASE_TABLE_EXT_PROPERTIES.MATVIEW_AFTER_LOAD_SCRIPT, modelObject, assistant);
    				addRelationalOption(options, BASE_TABLE_EXT_PROPERTIES.MATVIEW_SHARE_SCOPE, modelObject, assistant);
    				addRelationalOption(options, BASE_TABLE_EXT_PROPERTIES.MATERIALIZED_STAGE_TABLE, modelObject, assistant);
    				addRelationalOption(options, BASE_TABLE_EXT_PROPERTIES.ON_VDB_START_SCRIPT, modelObject, assistant);
    				addRelationalOption(options, BASE_TABLE_EXT_PROPERTIES.ON_VDB_DROP_SCRIPT, modelObject, assistant);
    				addRelationalOption(options, BASE_TABLE_EXT_PROPERTIES.MATVIEW_ONERROR_ACTION, modelObject, assistant);
    				addRelationalOption(options, BASE_TABLE_EXT_PROPERTIES.MATVIEW_TTL, modelObject, assistant);
    				
//    				propId = BASE_TABLE_EXT_PROPERTIES.VIEW_TABLE_GLOBAL_TEMP_TABLE;
//    				String globalTempTable = assistant.getOverriddenValue(modelObject, propId);
//    				if(!CoreStringUtil.isEmpty(globalTempTable)) {
//    					propId = propId.replace(RELATIONAL_PREFIX, TEIID_REL_PREFIX);
//    					options.put(propId, globalTempTable);
//    				}
    			} else if(ns.equals(SALESFORCE_PREFIX) )  {
        			for( ModelExtensionPropertyDefinition ext : defns) {
        				String propId = ext.getId();
        				String value = assistant.getOverriddenValue(modelObject, propId);

        				if( value != null ) {
        					String nsString = SET + SPACE + NAMESPACE + SPACE + SQUOTE + SF_URI + SQUOTE + SPACE + AS + SPACE + TEIID_SF_PREFIX;
        					namespaces.add(nsString);
        					propId = propId.replace(SALESFORCE_PREFIX, TEIID_SF_PREFIX);
        					options.put(propId, value);
        				}
        			}
    			} else if(ns.equals(TEIID_INFINISPAN_PREFIX)) {
        			for( ModelExtensionPropertyDefinition ext : defns) {
        				String propId = ext.getId();
        				String value = assistant.getOverriddenValue(modelObject, propId);

        				if( value != null ) {
        					String nsString = SET + SPACE + NAMESPACE + SPACE + SQUOTE + OBJECT_TEIID_SET_NAMESPACE + SQUOTE + SPACE + AS + SPACE + TEIID_INFINISPAN_PREFIX;
        					namespaces.add(nsString);
        					propId = propId.replace(TEIID_INFINISPAN_PREFIX, OBJECT_NS_PREFIX);
        					options.put(propId, value);
        				}
        			}
    			} else if( medAggregator.isImportedNamespacePrefix(ns) ) {
    				// Odata4, odata and sap-gateway share the same NS URI and shouldn't end up creating different
    				// SET NAMESPACE statement since all props are keyed on "teiid_odata" (i.e. not "teiid_odata4" for instances)
    				String prefix = ns;
    				if( ODATA4_PREFIX.equalsIgnoreCase(ns) ) {
    					prefix = ODATA_PREFIX;
    				} else if( SAP_GATEWAY_PREFIX.equalsIgnoreCase(ns) ) {
    					prefix = ODATA_PREFIX;
    				}
    				String teiidPrefix = TEIID_UNDERSCORE + prefix;
    			
    				for( ModelExtensionPropertyDefinition ext : defns) {
        				String propId = ext.getId();
        				String value = assistant.getOverriddenValue(modelObject, propId);

        				if( value != null ) {
        					// Add namespace
        					String nsURI = this.medAggregator.getNamespaceUri(ns);
        					if( nsURI != null ) {
        						String nsString = SET + SPACE + NAMESPACE + SPACE + SQUOTE + nsURI + SQUOTE + SPACE + AS + SPACE + teiidPrefix;
        						namespaces.add(nsString);
        					}
        					propId = propId.replace(ns, teiidPrefix);
        					options.put(propId, value);
        				}
    				}
    			} else if( !ns.equalsIgnoreCase(REST) ) {
        			for( ModelExtensionPropertyDefinition ext : defns) {
        				String propId = ext.getId();
        				String value = assistant.getOverriddenValue(modelObject, propId);

        				if( value != null ) {
        					// Add namespace
        					String nsURI = this.medAggregator.getNamespaceUri(ns);
        					if( nsURI != null ) {
        						String nsString = SET + SPACE + NAMESPACE + SPACE + SQUOTE + nsURI + SQUOTE + SPACE + AS + SPACE + ns;
        						namespaces.add(nsString);
        					}
        					options.put(propId, value);
        				}
        			}
    			}

    		}
    	}
    	
    	return options;
    	
    }
    
    private void addRelationalOption(Map<String, String> options, String propId, EObject modelObject, ModelObjectExtensionAssistant assistant) throws Exception {
		String value = assistant.getOverriddenValue(modelObject, propId);
		if(!CoreStringUtil.isEmpty(value)) {
			propId = propId.replace(RELATIONAL_PREFIX, TEIID_REL_PREFIX);
			options.put(propId, value);
		}
    }
    
    private boolean hasOption(EObject modelObject, String propId) throws Exception {

    	Collection<String> extensionNamespaces = medAggregator.getSupportedNamespacePrefixes(modelObject);
    	for( String ns : extensionNamespaces ) {
    		ModelObjectExtensionAssistant assistant = medAggregator.getModelObjectExtensionAssistant(ns);
    		if( assistant != null ) {
				String property = assistant.getOverriddenValue(modelObject, propId);
				if(!CoreStringUtil.isEmpty(property)) {
					return true;
				}
    		}
    	}
    	
    	return false;
    }
    
    private String getOption(EObject modelObject, String propId) throws Exception {

    	Collection<String> extensionNamespaces = medAggregator.getSupportedNamespacePrefixes(modelObject);
    	for( String ns : extensionNamespaces ) {
    		ModelObjectExtensionAssistant assistant = medAggregator.getModelObjectExtensionAssistant(ns);
    		if( assistant != null ) {
				String property = assistant.getOverriddenValue(modelObject, propId);
				if(!CoreStringUtil.isEmpty(property)) {
					return property;
				}
    		}
    	}
    	
    	return null;
    }
    
    @SuppressWarnings("unused")
	private void addOptionsForEObject(EObject eObj, StringBuilder sb) {
    	// Need to check with other assistants too
    	try {
    		OptionsStatement options = new OptionsStatement();
			Map<String, String> props = getOptionsForObject(eObj);
			for( String key : props.keySet() ) {
				String value = props.get(key);
				options.add(key, value, null);
			}
			if( !StringUtilities.isEmpty(options.toString())) {
				sb.append(SPACE).append(options);
			}
		} catch (Exception e) {
			addIssue(IStatus.ERROR, "Error finding options for " + getName(eObj), e); //$NON-NLS-1$
		}
    }
    
    private String getProcedureOptions(Procedure procedure) {
    	OptionsStatement options = new OptionsStatement();
    	
    	String desc = getDescription(procedure);
    	if( !StringUtilities.isEmpty(desc) ) {
    		options.add(ANNOTATION, desc, EMPTY_STRING);
    	}
    	
    	if( this.includeNIS ) {
    		options.add(NAMEINSOURCE, procedure.getNameInSource(), null);
    	}
    	
		String nativeQuery = getPropertyValue(procedure, PROCEDURE_EXT_PROPERTIES.NATIVE_QUERY);
		if(!CoreStringUtil.isEmpty(nativeQuery)) {
			options.add(NATIVE_QUERY_PROP, nativeQuery, null);
		}
		
		// Physical Model only
		if( !isVirtual ) {
			String nonPreparedValue =  getPropertyValue(procedure, PROCEDURE_EXT_PROPERTIES.NON_PREPARED);
			setBooleanProperty(NON_PREPARED_PROP, nonPreparedValue, false, options);
		}
		// Functions have many additional extension properties
		boolean isFunction = procedure.isFunction();
		if(isFunction) {
			String updateCount =  procedure.getUpdateCount().toString();
			if( updateCount.equals(ProcedureUpdateCount.ZERO_LITERAL.toString())) {
				options.add(UPDATECOUNT, ZERO, ZERO);
			} else if( updateCount.equals(ProcedureUpdateCount.ONE_LITERAL.toString())) {
				options.add(UPDATECOUNT, ONE, ZERO);
			} else if( updateCount.equals(ProcedureUpdateCount.MULTIPLE_LITERAL.toString())) {
				options.add(UPDATECOUNT, TWO, ZERO);
			}

			String value =  getPropertyValue(procedure, PROCEDURE_EXT_PROPERTIES.FUNCTION_CATEGORY);
			options.add(FUNCTION_CATEGORY_PROP, value, null);
			
			value =  getPropertyValue(procedure, PROCEDURE_EXT_PROPERTIES.JAVA_CLASS);
			options.add(JAVA_CLASS, value, null);
			
			value =  getPropertyValue(procedure, PROCEDURE_EXT_PROPERTIES.JAVA_METHOD);
			options.add(JAVA_METHOD, value, null);

			value =  getPropertyValue(procedure, PROCEDURE_EXT_PROPERTIES.VARARGS);
			setBooleanProperty(VARARGS_PROP, value, false, options);

			
			value =  getPropertyValue(procedure, PROCEDURE_EXT_PROPERTIES.NULL_ON_NULL);
			setBooleanProperty(NULL_ON_NULL_PROP, value, false, options);

			
			/*
			    NONDETERMINISTIC
			    COMMAND_DETERMINISTIC
			    SESSION_DETERMINISTIC
			    USER_DETERMINISTIC
			    VDB_DETERMINISTIC
			    DETERMINISTIC
			 */
			value =  getPropertyValue(procedure, PROCEDURE_EXT_PROPERTIES.DETERMINISTIC);
			boolean isDeterministic = Boolean.parseBoolean(value);
			if( isDeterministic ) {
				options.add(DETERMINISM_PROP, DETERMINISM_OPT_DETERMINISTIC, DETERMINISM_OPT_NONDETERMINISTIC);
			} else {
				options.add(DETERMINISM_PROP, DETERMINISM_OPT_NONDETERMINISTIC, DETERMINISM_OPT_NONDETERMINISTIC);
			}

			value =  getPropertyValue(procedure, PROCEDURE_EXT_PROPERTIES.AGGREGATE);
			if( value != null ) {
				boolean booleanValue = Boolean.parseBoolean(value);
				if( booleanValue ) {
					setBooleanProperty(AGGREGATE_PROP, value, false, options);
					
					value =  getPropertyValue(procedure, PROCEDURE_EXT_PROPERTIES.ANALYTIC);
					setBooleanProperty(ANALYTIC_PROP, value, false, options);
					
					value =  getPropertyValue(procedure, PROCEDURE_EXT_PROPERTIES.ALLOWS_ORDER_BY);
					setBooleanProperty(ALLOWS_ORDER_BY_PROP, value, false, options);
					
					value =  getPropertyValue(procedure, PROCEDURE_EXT_PROPERTIES.USES_DISTINCT_ROWS);
					setBooleanProperty(USES_DISTINCT_ROWS_PROP, value, false, options);
					
					value =  getPropertyValue(procedure, PROCEDURE_EXT_PROPERTIES.ALLOWS_DISTINCT);
					setBooleanProperty(ALLOWS_DISTINCT_PROP, value, false, options);
					
					value =  getPropertyValue(procedure, PROCEDURE_EXT_PROPERTIES.DECOMPOSABLE);
					setBooleanProperty(DECOMPOSABLE_PROP, value, false, options);
				}
			}
		} else {
		    options.add(UPDATECOUNT, ONE, ZERO);
			// REST PROPERTIES??
			String value =  getRestPropertyValue(procedure, RestModelExtensionConstants.PropertyIds.URI);
			if( value != null ) namespaces.add(REST_TEIID_SET_NAMESPACE);
			
			options.add(REST_URI, value, null);
			
			value =  getRestPropertyValue(procedure, RestModelExtensionConstants.PropertyIds.REST_METHOD);
			if( value != null ) namespaces.add(REST_TEIID_SET_NAMESPACE);
			
			options.add(REST_METHOD, value, null);
		}
		
    	// Need to check with other assistants too
    	try {
			Map<String, String> props = getOptionsForObject(procedure);
			for( String key : props.keySet() ) {
				String value = props.get(key);
				options.add(key, value, null);
			}
		} catch (Exception e) {
			addIssue(IStatus.ERROR, "Error finding options for " + getName(procedure), e); //$NON-NLS-1$
		}
		
		return options.toString();
    }
    
    public Set<String> getNamespaceStatements(EObject modelObject) throws Exception {

    	Set<String> nsStatements = new HashSet<String>();
    	
    	Collection<String> extensionNamespaces = medAggregator.getSupportedNamespacePrefixes(modelObject);
    	for( String ns : extensionNamespaces ) {
    		ModelObjectExtensionAssistant assistant = medAggregator.getModelObjectExtensionAssistant(ns);
    		Collection<ModelExtensionPropertyDefinition> defns = assistant.getPropertyDefinitions(modelObject);
    		
    		if( assistant != null ) {
    			if(ns.equals(RELATIONAL_PREFIX)) {
    				continue;
    			} else if(ns.equals(SALESFORCE_PREFIX) )  {
        			for( ModelExtensionPropertyDefinition ext : defns) {
        				String propId = ext.getId();
        				String value = assistant.getOverriddenValue(modelObject, propId);

        				if( value != null ) {
        					String nsString = SET + SPACE + NAMESPACE + SPACE + SQUOTE + SF_URI + SQUOTE + SPACE + AS + SPACE + TEIID_SF_PREFIX + NEW_LINE;
        					nsStatements.add(nsString);
        				}
        			}
    			} else if(ns.equals(TEIID_INFINISPAN_PREFIX)) {
	    			for( ModelExtensionPropertyDefinition ext : defns) {
	    				String propId = ext.getId();
	    				String value = assistant.getOverriddenValue(modelObject, propId);
	
	    				if( value != null ) {
	    					String nsString = SET + SPACE + NAMESPACE + SPACE + SQUOTE + OBJECT_TEIID_SET_NAMESPACE + SQUOTE + SPACE + AS + SPACE + TEIID_INFINISPAN_PREFIX + NEW_LINE;
	    					nsStatements.add(nsString);
	    				}
	    			}
    			} else if( medAggregator.isImportedNamespacePrefix(ns) ) {
					// Odata4, odata and sap-gateway share the same NS URI and shouldn't end up creating different
					// SET NAMESPACE statement since all props are keyed on "teiid_odata" (i.e. not "teiid_odata4" for instances)
					String prefix = ns;
					if( ODATA4_PREFIX.equalsIgnoreCase(ns) ) {
						prefix = ODATA_PREFIX;
					} else if( SAP_GATEWAY_PREFIX.equalsIgnoreCase(ns) ) {
						prefix = ODATA_PREFIX;
					}
					String teiidPrefix = TEIID_UNDERSCORE + prefix;
				
					for( ModelExtensionPropertyDefinition ext : defns) {
	    				String propId = ext.getId();
	    				String value = assistant.getOverriddenValue(modelObject, propId);
	
	    				if( value != null ) {
	    					// Add namespace
	    					String nsURI = this.medAggregator.getNamespaceUri(ns);
	    					if( nsURI != null ) {
	    						String nsString = SET + SPACE + NAMESPACE + SPACE + SQUOTE + nsURI + SQUOTE + SPACE + AS + SPACE + teiidPrefix + NEW_LINE;
	    						nsStatements.add(nsString);
	    					}
	    				}
					}
				} else if( !ns.equalsIgnoreCase(REST) ) {
        			for( ModelExtensionPropertyDefinition ext : defns) {
        				String propId = ext.getId();
        				String value = assistant.getOverriddenValue(modelObject, propId);

        				if( value != null ) {
        					// Add namespace
        					String nsURI = this.medAggregator.getNamespaceUri(ns);
        					if( nsURI != null ) {
        						String nsString = SET + SPACE + NAMESPACE + SPACE + SQUOTE + nsURI + SQUOTE + SPACE + AS + SPACE + ns + NEW_LINE;
        						nsStatements.add(nsString);
        					}
        				}
        			}
    			}
    		}
    	}
    	
    	//8888
    	
    	return nsStatements;
    }
    
    private RelationalModelExtensionAssistant getRelationalModelExtensionAssistant() {
    	if( assistant == null ) {
    		assistant = RelationalUtil.getRelationalExtensionAssistant();
    	}
    	
    	return assistant;
    }
    
    private String getPropertyValue(EObject eObj, String propertyID ) {
    	
    	try {
			return getRelationalModelExtensionAssistant().getPropertyValue(eObj, propertyID);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return null;
    }
    
    public void setIsVirtual(boolean isVirtualModel) {
    	isVirtual = isVirtualModel;
    }
    
    private String getRestPropertyValue(EObject eObj, String propertyID ) {
    	
    	try {
			return RestModelExtensionAssistant.getRestProperty(eObj, propertyID);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return null;
    }
    
    private void setBooleanProperty(String propID, String stringValue, boolean defaultValue, OptionsStatement options) {
		if( stringValue != null ) {
			boolean booleanValue = Boolean.parseBoolean(stringValue);
			options.add(propID, String.valueOf(booleanValue), String.valueOf(defaultValue));
		}
    }

    private String escapeStringValue(String str, String tick) {
        return StringUtilities.replaceAll(str, tick, tick + tick);
    }
    
    private String escapeSinglePart(String token) {
        if (TeiidSQLConstants.isReservedWord(token)) {
            return TeiidSQLConstants.Tokens.ID_ESCAPE_CHAR + token + TeiidSQLConstants.Tokens.ID_ESCAPE_CHAR;
        }
        boolean escape = true;
        char start = token.charAt(0);
        if (HASH.equals(Character.toString(start)) || AMPERSAND.equals(Character.toString(start)) || StringUtilities.isLetter(start)) {
            escape = false;
            for (int i = 1; !escape && i < token.length(); i++) {
                char c = token.charAt(i);
                escape = !StringUtilities.isLetterOrDigit(c) && c != '_';
            }
        }
        if (escape) {
            return TeiidSQLConstants.Tokens.ID_ESCAPE_CHAR + escapeStringValue(token, SPEECH_MARK) + TeiidSQLConstants.Tokens.ID_ESCAPE_CHAR;
        }
        return token;
    }
    
    /*
     * Utility to check a unique constraint and determine if it is redundant. Basically if the uc columns match a PK with the same columns
     */
    private Collection<UniqueConstraint> getUniqueUniqueContraints(BaseTable table) {
    	EList<?> ucs = table.getUniqueConstraints();
    	Collection<UniqueConstraint> uniqueConstraints = new ArrayList<UniqueConstraint>();
    	
    	PrimaryKey pk = table.getPrimaryKey();
    	
    	for( Object obj: ucs) {
    		UniqueConstraint uc = (UniqueConstraint)obj;
    		if( pk != null ) {
	    		EList<?> pkColumns = pk.getColumns();
	    		EList<?> ucColumns = uc.getColumns();
	    		
	    		if( pkColumns.size() == ucColumns.size() ) {
	    			boolean matchesAll = true;
	    			for( Object col : ucColumns ) {
	    				if( ! pkColumns.contains(col) ) {
	    					matchesAll = false;
	    				}
	    			}
	    			if( !matchesAll )
	    				uniqueConstraints.add(uc);
	    		} else {
	    			uniqueConstraints.add(uc);
	    		}
    		} else {
    			uniqueConstraints.add(uc);
    		}
    	}
    	
    	return uniqueConstraints;
    }
    
    public boolean isTeiidProcedure(String name) {
    	// Check for invokeHttp(), invoke(), getFiles(), getTextFiles() and saveFile()
    	if( name.equalsIgnoreCase(TEIID_PROCEDURE_NAMES.INVOKE) ||
    		name.equalsIgnoreCase(TEIID_PROCEDURE_NAMES.INVOKE_HTTP) ||
    		name.equalsIgnoreCase(TEIID_PROCEDURE_NAMES.GET_FILES) ||
    		name.equalsIgnoreCase(TEIID_PROCEDURE_NAMES.GET_TEXT_FILES) ||
    		name.equalsIgnoreCase(TEIID_PROCEDURE_NAMES.SAVE_FILE) ) {
    		return true;
    	}
    	
    	return false;
    }

    
    private void addIssue(int severity, String message) {
    	issues.add(new Status(severity, TransformationPlugin.PLUGIN_ID, message));
    }
    
    private void addIssue(int severity, String message, Throwable e) {
    	issues.add(new Status(severity, TransformationPlugin.PLUGIN_ID, message, e));
    }
    

    public void setIncludeTables(boolean includeTables) {
		this.includeTables = includeTables;
	}

	public void setIncludeProcedures(boolean includeProcedures) {
		this.includeProcedures = includeProcedures;
	}

	public void setIncludeFKs(boolean includeFKs) {
		this.includeFKs = includeFKs;
	}
	
    public void setIncludeNIS(boolean includeNIS) {
		this.includeNIS = includeNIS;
	}

	public void setIncludeNativeType(boolean includeNativeType) {
		this.includeNativeType = includeNativeType;
	}
	
	private Collection<Index> getIndexesForTable(Table table, Collection<Index> allIndexes) {
	    Collection<Index> indexes = new ArrayList<Index>();
	    
		@SuppressWarnings("unchecked")
		List<Column> tableColumns = table.getColumns();
		
	    for( Index index : allIndexes ) {
	    	boolean addIndex = false;
	    	
	    	@SuppressWarnings("unchecked")
			List<Column> colRefs = index.getColumns();
	    	for( Column colRef : colRefs ) {
		    	for( Column col : tableColumns ) {
		    		if( col == colRef ) {
		    			addIndex = true;
		    		}
		    	}
	    	}
	    	
	    	if( addIndex ) {
	    		indexes.add(index);
	    	}
	    }
	    
	    return indexes;
	}
	
	private Collection<Index> getIndexes(ModelResource mr) throws ModelWorkspaceException {
	    CoreArgCheck.isNotNull(mr);
	    
	    Collection<Index> indexes = new ArrayList<Index>();
	    
		final ModelContents contents = ModelContents.getModelContents(mr);

		append(StringConstants.NEW_LINE);
		
		for( Object obj : contents.getAllRootEObjects() ) {
			if(obj instanceof Index ) {
				indexes.add((Index)obj);
			}
		}
		
		return indexes;
	}

	class OptionsStatement {
    	boolean hasOptions;
    	StringBuilder sb;
    	
    	public OptionsStatement() {
    		super();
    		sb = new StringBuilder();
    		sb.append(Reserved.OPTIONS).append(OPEN_BRACKET);
    	}
    	
    	public void add(String key, String value, String defaultValue) {
    		if( StringUtilities.isEmpty(value) ) return;

    		if(! StringUtilities.areDifferent(value, defaultValue)) return;

    		if( hasOptions ) sb.append(COMMA + SPACE);
    		
    		hasOptions = true;
    		
            sb.append(escapeSinglePart(key)).append(SPACE);
            if (Reserved.FALSE.equalsIgnoreCase(value) || Reserved.TRUE.equalsIgnoreCase(value)) {
                sb.append(QUOTE_MARK + value.toUpperCase() + QUOTE_MARK);
                return;
            }

            // Default to a string value which should be placed in quotes
            sb.append(QUOTE_MARK + value + QUOTE_MARK);
    		
//    		sb.append(key).append(SPACE).append(value);

    	}
    	
    	@Override
        public String toString() {
    		sb.append(CLOSE_BRACKET);
    		
    		if( !hasOptions) return null;
    		
    		return sb.toString();
    	}
    }
    
    /*
     * Need a utility class and methods to determine the procedure type so we can construct the appropriate  DDL
     * TODO: finish this method and implement in the procedure(Procedure) method
     */
    class ProcedureHandler {
    	Procedure proc;
    	// FOREIGN PROCEDURE can have a result set or an out parameter
    	// CREATE FOREIGN PROCEDURE func (x integer, y IN integer) returns table (z integer);
    	// CREATE FOREIGN PROCEDURE func (x integer, y IN integer) returns integer;
    	
    	// CREATE FOREIGN FUNCTION func (x integer, y integer) returns boolean OPTIONS ("teiid_rel:native-query"'$1 << $2');
    	
    	// CREATE VIRTUAL FUNCTION sumAll(arg integer) RETURNS integer OPTIONS (JAVA_CLASS 'org.something.SumAll',  JAVA_METHOD 'addInput', AGGREGATE 'true', VARARGS 'true', "NULL-ON-NULL" 'true');
    	
    	// CREATE VIRTUAL PROCEDURE getTweets(query varchar) 
    	//			RETURNS (created_on varchar(25), from_user varchar(25), to_user varchar(25), profile_image_url varchar(25), source varchar(25), text varchar(140))
    	//		AS
    	//	 SELECT * FROM twitterFeedSummary;


    	public ProcedureHandler(Procedure procedure) {
    		this.proc = procedure;
    	}
    	
    	
    }
}
