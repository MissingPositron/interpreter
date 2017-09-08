package com.bamless.interpreter.semantic;

import com.bamless.interpreter.ErrUtils;
import com.bamless.interpreter.Position;
import com.bamless.interpreter.ast.expression.AssignExpression;
import com.bamless.interpreter.ast.expression.Expression;
import com.bamless.interpreter.ast.expression.VarLiteral;
import com.bamless.interpreter.ast.statement.BlockStatement;
import com.bamless.interpreter.ast.statement.ForStatement;
import com.bamless.interpreter.ast.statement.IfStatement;
import com.bamless.interpreter.ast.statement.PrintStatement;
import com.bamless.interpreter.ast.statement.Statement;
import com.bamless.interpreter.ast.statement.VarDecl;
import com.bamless.interpreter.ast.statement.WhileStatement;
import com.bamless.interpreter.ast.visitor.VoidVisitorAdapter;

public class SemanticAnalyzer extends VoidVisitorAdapter<Void> {
	/**
	 * Symbol table to keep track of declared variables.
	 * The boolean associated with the variable's identifier
	 * denotes if the variable is initialized or not
	 */
	private SymbolTable<Boolean> sym;
	
	public SemanticAnalyzer() {
		sym = new SymbolTable<>();
	}
	
	@Override
	public void visit(BlockStatement v, Void arg) {
		sym.enterScope();
		for(Statement s : v.getStmts()) {
			if(s instanceof Expression && !(s instanceof AssignExpression))
				ErrUtils.warn("Warning %s: computed value is not used", s.getPosition());
			s.accept(this, null);
		}
		sym.exitScope();
	}
	
	@Override
	public void visit(VarDecl v, Void arg) {
		try {
			sym.define(v.getId().getVal(), v.getInitializer() != null);
		} catch(IllegalArgumentException e) {
			semanticError(v.getPosition(), "double declaration of variable %s", v.getId().getVal());
		}
	}
	
	@Override
	public void visit(IfStatement v, Void arg) {
		v.getCondition().accept(this, null);
		v.getThenStmt().accept(this, null);
		if(v.getElseStmt() != null) {
			v.getElseStmt().accept(this, null);
		}
	}
	
	@Override
	public void visit(WhileStatement v, Void arg) {
		v.getCondition().accept(this, null);
		v.getBody().accept(this, null);
	}
	
	@Override
	public void visit(ForStatement f, Void arg) {
		if(f.getInit() != null)
			f.getInit().accept(this, null);
		if(f.getCond() != null)
			f.getCond().accept(this, null);
		if(f.getAct() != null)
			f.getCond().accept(this, null);
		
		f.getBody().accept(this, null);
	}
	
	@Override
	public void visit(PrintStatement p, Void arg) {
		p.getExpression().accept(this, null);
	}
	
	@Override
	public void visit(AssignExpression e, Void arg) {
		sym.set(e.getId().getVal(), true);
	}
	
	@Override
	public void visit(VarLiteral v, Void arg) {
		Boolean isInit = sym.lookup(v.getId().getVal());
		if(isInit == null) {
			semanticError(v.getId().getPosition(), "variable %s cannot be resolved", v.getId().getVal());
		}
		if(!isInit) {
			semanticError(v.getId().getPosition(), "the local variable %s may not have been initialized", v.getId().getVal());
		}
	}
	
	private void semanticError(Position pos, String format, Object... args) {
		throw new SemanticException(String.format("Semantic error at " + pos + ": " + format, args));
	}
}
