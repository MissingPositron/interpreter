package com.bamless.interpreter.interpret.expeval;

import java.math.BigDecimal;

import com.bamless.interpreter.ast.expression.ArithmeticBinExpression;
import com.bamless.interpreter.ast.expression.ArithmeticBinExpression.ArithmeticBinOperation;
import com.bamless.interpreter.ast.expression.ArrayAccess;
import com.bamless.interpreter.ast.expression.AssignExpression;
import com.bamless.interpreter.ast.expression.FuncCallExpression;
import com.bamless.interpreter.ast.expression.Lvalue;
import com.bamless.interpreter.ast.expression.StringLiteral;
import com.bamless.interpreter.ast.expression.VarLiteral;
import com.bamless.interpreter.ast.type.Type.TypeID;
import com.bamless.interpreter.interpret.Interpreter;
import com.bamless.interpreter.interpret.RuntimeError;
import com.bamless.interpreter.interpret.memenv.MemoryEnvironment.Frame;
import com.bamless.interpreter.visitor.VisitorAdapter;

public class StringEval extends VisitorAdapter<String, Frame> {
	private Interpreter interpreter;
	
	public StringEval(Interpreter interpreter) {
		this.interpreter = interpreter;
	}
	
	@Override
	public String visit(ArithmeticBinExpression e, Frame frame) {
		if(e.getOperation() != ArithmeticBinOperation.PLUS)
			throw new RuntimeError("fatal error");
		
		TypeID leftType = e.getLeft().getType().getId();
		TypeID rightType = e.getRight().getType().getId();
		
		if(leftType == TypeID.FLOAT || leftType == TypeID.INT) {
			BigDecimal res = e.getLeft().accept(interpreter.arithmetic(), frame);
			
			String l = leftType == TypeID.FLOAT ? res.floatValue() + "" : res.intValue() + "";
			String r = e.getRight().accept(this, frame);
			return l + r;
		} else if(rightType == TypeID.FLOAT || rightType == TypeID.INT) {
			BigDecimal res = e.getRight().accept(interpreter.arithmetic(), frame);
			
			String l = e.getLeft().accept(this, frame);
			String r = rightType == TypeID.FLOAT ? res.floatValue() + "" : res.intValue() + "";
			return l + r;
		} else if(leftType == TypeID.BOOLEAN) {
			String l = e.getLeft().accept(interpreter.bool(), frame).toString();
			String r = e.getRight().accept(this, frame);
			return l + r;
		} else if(rightType == TypeID.BOOLEAN) {
			String l = e.getLeft().accept(this, frame);
			String r = e.getRight().accept(interpreter.bool(), frame).toString();
			return l + r;
		}
		
		return e.getLeft().accept(this, frame) + e.getRight().accept(this, frame);
	}
	
	@Override
	public String visit(VarLiteral v, Frame frame) {
		return frame.<String>retrieve(v);
	}
	
	@Override
	public String visit(ArrayAccess a, Frame frame) {
		return frame.<String>retrieve(a);
	}
	
	@Override
	public String visit(StringLiteral s, Frame frame) {
		return s.getValue();
	}
	
	@Override
	public String visit(AssignExpression e, Frame frame) {
		String res = e.getExpression().accept(this, frame);
		frame.set((Lvalue) e.getLvalue(), res);
		return res;
	}
	
	@Override
	public String visit(FuncCallExpression f, Frame frame) {
		interpreter.callFunction(f);
		return frame.<String>getReturnRegister();
	}
	
}