package com.bamless.interpreter.ast.type;

import java.util.HashMap;

public abstract class Type {
	/**Singleton types*/
	public static final Type INT = new IntType();
	public static final Type FLOAT = new FloatType();
	public static final Type BOOLEAN = new BooleanType();
	public static final Type STRING = new StringType();
	public static final Type VOID = new VoidType();
	
	private static final HashMap<Type, ArrayType> arrayFromType = new HashMap<>();
	
	protected Type() {
	}
	
	public static Type valueOf(String type) {
		switch (type.toUpperCase()) {
		case "INT":
			return INT;
		case "FLOAT":
			return FLOAT;
		case "BOOLEAN":
			return BOOLEAN;
		case "STRING":
			return STRING;
		case "VOID":
			return VOID;
		}
		
		throw new IllegalArgumentException("Invalid type name");
	}
	
	public static ArrayType arrayType(Type t) {
		if(!arrayFromType.containsKey(t)) {
			ArrayType at = new ArrayType(t);
			arrayFromType.put(t, at);
		}
		return arrayFromType.get(t);
	}
	
	/**
	 * For any given operation, return the type result of applying that operation
	 * over the given types (or null if the operation cannot be applied over the types)
	 */
	public abstract Type plus(Type other);
	public abstract Type minus(Type other);
	public abstract Type mul(Type other);
	public abstract Type div(Type other);
	public abstract Type modulus(Type other);
	
	public abstract Type logicalOp(Type other);
	public abstract Type relationalOp(Type other);
	public abstract Type equalityOp(Type other);
	
	public abstract boolean canAssign(Type other);
	
	public abstract boolean isArray();
	
	public abstract String toString();
}
