/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.teiid.query.function;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.teiid.core.CoreConstants;
import org.teiid.core.types.ArrayImpl;
import org.teiid.core.types.BinaryType;
import org.teiid.core.types.DataTypeManagerService;
import org.teiid.core.util.PropertiesUtils;
import org.teiid.designer.runtime.version.spi.ITeiidServerVersion;
import org.teiid.designer.runtime.version.spi.TeiidServerVersion.Version;
import org.teiid.designer.udf.IFunctionDescriptor;
import org.teiid.designer.udf.IFunctionLibrary;
import org.teiid.metadata.AbstractMetadataRecord;
import org.teiid.metadata.FunctionMethod;
import org.teiid.metadata.FunctionMethod.Determinism;
import org.teiid.metadata.FunctionMethod.PushDown;
import org.teiid.metadata.Procedure;
import org.teiid.query.util.CommandContext;
import org.teiid.runtime.client.Messages;
import org.teiid.runtime.client.TeiidClientException;


/**
 * The FunctionDescriptor describes a particular function instance enough
 * to invoke the function.
 */
public class FunctionDescriptor implements Serializable, Cloneable, IFunctionDescriptor {
	private static final long serialVersionUID = 5374103983118037242L;

	private static final boolean ALLOW_NAN_INFINITY = PropertiesUtils.getBooleanProperty(System.getProperties(), "org.teiid.allowNanInfinity", false); //$NON-NLS-1$

	private final ITeiidServerVersion teiidVersion;
	private Class<?>[] types;
	private Class<?> returnType;	
    private boolean requiresContext;
    private FunctionMethod method;
    private String schema; //TODO: remove me - we need to create a proper schema for udf and system functions
    private Object metadataID;
    private boolean hasWrappedArgs;
    private boolean calledWithVarArgArrayParam; //TODO: could store this on the function and pass to invoke
    
    // This is transient as it would be useless to invoke this method in 
    // a different VM.  This function descriptor can be used to look up 
    // the real VM descriptor for execution.
    private transient Method invocationMethod;

    private ClassLoader classLoader;

    private Procedure procedure;

	FunctionDescriptor(ITeiidServerVersion teiidVersion, FunctionMethod method, Class<?>[] types,
			Class<?> outputType, Method invocationMethod,
			boolean requiresContext, ClassLoader classloader) {
		this.teiidVersion = teiidVersion;
        this.types = types;
		this.returnType = outputType;
        this.invocationMethod = invocationMethod;
        this.requiresContext = requiresContext;
        this.method = method;
        this.classLoader = classloader;
	}
	
	public Object newInstance() {
		try {
			return invocationMethod.getDeclaringClass().newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(Messages.gs(Messages.TEIID.TEIID30602, method.getName(), method.getInvocationClass()));
		} catch (IllegalAccessException e) {
			throw new RuntimeException(Messages.gs(Messages.TEIID.TEIID30602, method.getName(), method.getInvocationClass()));
		}
	}
	
	public void setHasWrappedArgs(boolean hasWrappedArgs) {
		this.hasWrappedArgs = hasWrappedArgs;
	}
	
	public String getSchema() {
		return schema;
	}
	
	public void setSchema(String schema) {
		this.schema = schema;
	}
	
	public String getName() {
		return this.method.getName();				
	}
	
	public String getFullName() {
		if (CoreConstants.SYSTEM_MODEL.equals(this.schema)) {
			return getName();
		}
		return this.schema + AbstractMetadataRecord.NAME_DELIM_CHAR + getName();
	}
    
    public PushDown getPushdown() {
        return this.method.getPushdown();
    }
    
	public Class<?>[] getTypes() {
		return this.types;
	}
	
	public Class<?> getReturnType() {
		return this.returnType;
	}		
	
    Method getInvocationMethod() {
        return this.invocationMethod;
    }
       
    public boolean requiresContext() {
        return this.requiresContext;
    }

    public Procedure getProcedure() {
        return procedure;
    }

    public void setProcedure(Procedure procedure) {
        this.procedure = procedure;
    }

	@Override
	public String toString() {
		StringBuffer str = new StringBuffer(this.method.getName());
		str.append("("); //$NON-NLS-1$
		for(int i=0; i<types.length; i++) {
			if(types[i] != null) { 
				str.append(types[i].getName());
			} else {
				str.append("null"); //$NON-NLS-1$
			}		
			if(i<(types.length-1)) {
				str.append(", "); //$NON-NLS-1$
			}
		}
		str.append(") : "); //$NON-NLS-1$
        if(returnType == null) { 
            str.append("null"); //$NON-NLS-1$
        } else {    
    		str.append(returnType.getName());
        }    
		return str.toString();
	}

    public boolean isNullDependent() {
        return !this.method.isNullOnNull();
    }
    
    public Determinism getDeterministic() {
        return this.method.getDeterminism();
    }

    @Override
	public FunctionDescriptor clone() {
        try {
            return (FunctionDescriptor) super.clone();
        } catch (CloneNotSupportedException e) {
             throw new RuntimeException(e);
        }
    }
    
    public FunctionMethod getMethod() {
		return method;
	}

    void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

	public Object getMetadataID() {
		return this.metadataID;
	}

	public void setMetadataID(Object metadataID) {
		this.metadataID = metadataID;
	}
	
	public void checkNotPushdown() throws Exception {
	    // Check for function we can't evaluate
	    if(getPushdown() == PushDown.MUST_PUSHDOWN) {
	         throw new TeiidClientException(Messages.gs(Messages.TEIID.TEIID30341, getFullName()));
	    }
	}

	/**
	 * Invoke the function described in the function descriptor, using the
	 * values provided.  Return the result of the function.
	 * @param teiidVersion
	 * @param values Values that should match 1-to-1 with the types described in the
	 * function descriptor
	 * @param context 
	 * @param functionTarget TODO
	 * @param fd Function descriptor describing the name and types of the arguments
	 * @return Result of invoking the function
	 */
	public Object invokeFunction(Object[] values, CommandContext context, Object functionTarget) throws Exception {
        if (!isNullDependent()) {
        	for (int i = requiresContext?1:0; i < values.length; i++) {
				if (values[i] == null) {
					return null;
				}
			}
        }

        // If descriptor is missing invokable method, find this VM's descriptor
        // give name and types from fd
        if(invocationMethod == null) {
        	 throw new TeiidClientException(Messages.gs(Messages.TEIID.TEIID30382, getFullName()));
        }
        
        // Invoke the method and return the result
        try {
        	if (hasWrappedArgs) {
        		for (int i = 0; i < values.length; i++) {
        			Object val = values[i];
        			if (val != null && types[i] == DataTypeManagerService.DefaultDataTypes.VARBINARY.getTypeClass()) {
            			values[i] = ((BinaryType)val).getBytesDirect();
        			}
        		}
        	}
        	if (method.isVarArgs()) {
        		if (calledWithVarArgArrayParam) {
        			ArrayImpl av = (ArrayImpl)values[values.length -1];
        			if (av != null) {
        				Object[] vals = av.getValues();
        				values[values.length - 1] = vals;
	    				if (hasWrappedArgs && types[types.length - 1] == DataTypeManagerService.DefaultDataTypes.VARBINARY.getTypeClass()) {
	    					vals = Arrays.copyOf(vals, vals.length);
	        				for (int i = 0; i < vals.length; i++) {
	        					if (vals[i] != null) {
	        						vals[i] = ((BinaryType)vals[i]).getBytesDirect();
	        					}
	        				}
	        				values[values.length - 1] = vals;
	        			}
	    				Class<?> arrayType = invocationMethod.getParameterTypes()[types.length - 1];
						if (arrayType.getComponentType() != Object.class
	    						&& vals.getClass() != arrayType) {
	    					Object varArgs = Array.newInstance(arrayType.getComponentType(), vals.length);
	    					for (int i = 0; i < vals.length; i++) {
			        			Array.set(varArgs, i, vals[i]);
			        		}
	        				values[values.length -1] = varArgs;
	    				}
        			}
        		} else {
	        		int i = invocationMethod.getParameterTypes().length;
	        		Object[] newValues = Arrays.copyOf(values, i);
	        		Object varArgs = null;
	        		if (invocationMethod.getParameterTypes()[i - 1].getComponentType() != Object.class) {
		        		int varArgCount = values.length - i + 1;
		        		varArgs = Array.newInstance(invocationMethod.getParameterTypes()[i - 1].getComponentType(), varArgCount);
		        		for (int j = 0; j < varArgCount; j++) {
		        			Array.set(varArgs, j, values[i-1+j]);
		        		}
	        		} else {
	        			varArgs = Arrays.copyOfRange(values, i - 1, values.length);
	        		}
	        		newValues[i - 1] = varArgs;
	        		values = newValues;
        		}
        	}
            Object result = null;
            ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
            try {
                if (this.classLoader != null) {
                    Thread.currentThread().setContextClassLoader(this.classLoader);
                }
                result = invocationMethod.invoke(functionTarget, values);
            } finally {
                Thread.currentThread().setContextClassLoader(originalCL);
            }
            if (context != null && getDeterministic().ordinal() <= Determinism.USER_DETERMINISTIC.ordinal()) {
            	context.setDeterminismLevel(getDeterministic());
            }
            return importValue(teiidVersion, result, getReturnType());
        } catch(ArithmeticException e) {
    		 throw new TeiidClientException(e, Messages.gs(Messages.TEIID.TEIID30384, getFullName()));
        } catch(InvocationTargetException e) {
             throw new TeiidClientException(e.getTargetException(), Messages.gs(Messages.TEIID.TEIID30384, getFullName()));
        } catch(IllegalAccessException e) {
             throw new TeiidClientException(e, Messages.gs(Messages.TEIID.TEIID30385, method.toString()));
        } catch (Exception e) {
        	 throw new TeiidClientException(e);
		}
	}

	public static Object importValue(ITeiidServerVersion teiidVersion, Object result, Class<?> expectedType)
			throws Exception {
		if (!ALLOW_NAN_INFINITY) {
			if (result instanceof Double) {
		    	Double floatVal = (Double)result;
		    	if (Double.isInfinite(floatVal) || Double.isNaN(floatVal)) {
		    		throw new ArithmeticException("Infinite or invalid result");  //$NON-NLS-1$
		    	}
		    } else if (result instanceof Float) {
		    	Float floatVal = (Float)result;
		    	if (Float.isInfinite(floatVal) || Float.isNaN(floatVal)) {
		    		throw new ArithmeticException("Infinite or invalid result");  //$NON-NLS-1$
		    	}
		    }
		}
		DataTypeManagerService dataTypeManager = DataTypeManagerService.getInstance(teiidVersion);
		result = dataTypeManager.convertToRuntimeType(result, expectedType != DataTypeManagerService.DefaultDataTypes.OBJECT.getTypeClass());
		if (expectedType.isArray() && result instanceof ArrayImpl) {
			return result;
		}
		result = dataTypeManager.transformValue(result, expectedType);
		if (result instanceof String) {
			String s = (String)result;
			if (s.length() > DataTypeManagerService.MAX_STRING_LENGTH) {
				return s.substring(0, DataTypeManagerService.MAX_STRING_LENGTH);
			}
		}
		return result;
	}    
	
	public boolean isCalledWithVarArgArrayParam() {
		return calledWithVarArgArrayParam;
	}
	
	public void setCalledWithVarArgArrayParam(boolean calledWithVarArgArrayParam) {
		this.calledWithVarArgArrayParam = calledWithVarArgArrayParam;
	}
	
	public boolean isSystemFunction(String name) {
		return this.getName().equalsIgnoreCase(name) && CoreConstants.SYSTEM_MODEL.equals(this.getSchema());
	}
	
	public boolean isSystemFunction(IFunctionLibrary.FunctionName name) {
        return name.equalsIgnoreCase(getName()) && CoreConstants.SYSTEM_MODEL.equals(this.getSchema());
    }
	
}
