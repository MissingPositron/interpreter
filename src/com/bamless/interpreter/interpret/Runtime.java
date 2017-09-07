package com.bamless.interpreter.interpret;

import com.bamless.interpreter.semantic.SymbolTable;

public class Runtime {
	private SymbolTable<Object> environmet;
	
	public Runtime() {
		environmet = new SymbolTable<>();
	}
	
	public SymbolTable<Object> getEnv() {
		return environmet;
	}

}
