package com.bamless.interpreter.parser;

import static com.bamless.interpreter.ast.expression.ArithmeticBinExpression.ArithmeticBinOperation.DIV;
import static com.bamless.interpreter.ast.expression.ArithmeticBinExpression.ArithmeticBinOperation.MINUS;
import static com.bamless.interpreter.ast.expression.ArithmeticBinExpression.ArithmeticBinOperation.MOD;
import static com.bamless.interpreter.ast.expression.ArithmeticBinExpression.ArithmeticBinOperation.MULT;
import static com.bamless.interpreter.ast.expression.ArithmeticBinExpression.ArithmeticBinOperation.PLUS;
import static com.bamless.interpreter.ast.expression.EqualityExpression.EqualityOperation.EQ;
import static com.bamless.interpreter.ast.expression.EqualityExpression.EqualityOperation.NEQ;
import static com.bamless.interpreter.ast.expression.LogicalExpression.BooleanBinOperation.AND;
import static com.bamless.interpreter.ast.expression.LogicalExpression.BooleanBinOperation.OR;
import static com.bamless.interpreter.ast.expression.RelationalExpression.RelationalOperation.GE;
import static com.bamless.interpreter.ast.expression.RelationalExpression.RelationalOperation.GT;
import static com.bamless.interpreter.ast.expression.RelationalExpression.RelationalOperation.LE;
import static com.bamless.interpreter.ast.expression.RelationalExpression.RelationalOperation.LT;
import static com.bamless.interpreter.ast.expression.IncrementOperator.INCR;
import static com.bamless.interpreter.ast.expression.IncrementOperator.DECR;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.bamless.interpreter.ast.ASTNode;
import com.bamless.interpreter.ast.Identifier;
import com.bamless.interpreter.ast.Position;
import com.bamless.interpreter.ast.Program;
import com.bamless.interpreter.ast.expression.ArithmeticBinExpression;
import com.bamless.interpreter.ast.expression.ArrayAccess;
import com.bamless.interpreter.ast.expression.AssignExpression;
import com.bamless.interpreter.ast.expression.BooleanLiteral;
import com.bamless.interpreter.ast.expression.EqualityExpression;
import com.bamless.interpreter.ast.expression.Expression;
import com.bamless.interpreter.ast.expression.FloatLiteral;
import com.bamless.interpreter.ast.expression.IntegerLiteral;
import com.bamless.interpreter.ast.expression.LogicalExpression;
import com.bamless.interpreter.ast.expression.LogicalNotExpression;
import com.bamless.interpreter.ast.expression.PostIncrementOperation;
import com.bamless.interpreter.ast.expression.PreIncrementOperation;
import com.bamless.interpreter.ast.expression.RelationalExpression;
import com.bamless.interpreter.ast.expression.StringLiteral;
import com.bamless.interpreter.ast.expression.VarLiteral;
import com.bamless.interpreter.ast.statement.ArrayDecl;
import com.bamless.interpreter.ast.statement.BlockStatement;
import com.bamless.interpreter.ast.statement.ForStatement;
import com.bamless.interpreter.ast.statement.IfStatement;
import com.bamless.interpreter.ast.statement.PrintStatement;
import com.bamless.interpreter.ast.statement.Statement;
import com.bamless.interpreter.ast.statement.VarDecl;
import com.bamless.interpreter.ast.statement.WhileStatement;
import com.bamless.interpreter.ast.type.Type;
import com.bamless.interpreter.lex.Lexer;
import com.bamless.interpreter.lex.Token;

public class ASTParser {
	private final static String LEX_FILE = "/lexical-spec.lex";
	
	private Lexer lex;
	
	public ASTParser() {
		lex = new Lexer(ClassLoader.class.getResourceAsStream(LEX_FILE), true, "//.+");
	}
	
	public ASTNode parse(File f) throws FileNotFoundException, IOException {
		lex.tokenize(f);
		return parse();
	}
	
	public ASTNode parse(InputStream src) throws IOException {
		lex.tokenize(src);
		return parse();
	}
	
	public ASTNode parse(String src) {
		lex.tokenize(src);
		return parse();
	}
	
	private ASTNode parse() {
		ASTNode root = program();
		
		if(!lex.isFinished()) {
			throw new ParseException(String.format("Syntax error at %s: unexpected token \"%s\"", 
					lex.peek().getPosition(), lex.peek().getValue()));
		}
		
		return root;
	}
	
	private ASTNode program() {
		Position start = new Position(0, 0);
		
		List<Statement> statements = new ArrayList<>();
		while(lex.hasNext()) {
			Token peek = lex.peek();
			
			//can only declare var 	inside a block
			if(peek.getType().equals("INT") || peek.getType().equals("BOOLEAN") || 
					peek.getType().equals("FLOAT") || peek.getType().equals("STRING")) {
				statements.add(varDecl());
			} else {
				statements.add(statement());
			}
		}
		
		return new Program(start, new BlockStatement(statements, start));
	}
	
	private Statement statement() {
		switch(lex.peek().getType()) {
		//statement
		case "IF":
			return ifStmt();
		case "WHILE":
			return whileStmt();
		case "FOR":
			return forStmt();
		case "{":
			return block();
		case "PRINT":
			return printStmt();
		default:
			Statement s = expression();
			return s;
		}
	}
	
	private Statement block() {
		Position start = require("{").getPosition();
		
		Token peek;
		List<Statement> statements = new ArrayList<>();
		while(!(peek = lex.peek()).getType().equals("}")) {
			//can only declare var 	inside a block
			if(peek.getType().equals("INT") || peek.getType().equals("BOOLEAN") || 
					peek.getType().equals("FLOAT") || peek.getType().equals("STRING")) {
				statements.add(varDecl());
			} else {
				statements.add(statement());
			}
		}
		
		require("}");
		
		return new BlockStatement(statements, start);
	}

	private Statement varDecl() {	
		Token typeTok = lex.next();
		Type t = Type.valueOf(typeTok.getType());
		
		Token idTok = require("IDENTIFIER");
		Identifier id = new Identifier(idTok.getPosition(), idTok.getValue());
		
		//Array delcaration
		if(lex.peek().getType().equals("[")) {
			List<Expression> dim = new ArrayList<>();
			while(lex.peek().getType().equals("[")) {
				require("[");
				
				t = Type.arrayType(t);
				dim.add(expression());
				
				require("]");
			}
			
			return new ArrayDecl(typeTok.getPosition(), t, dim, id);
		}
		//Normal declaration
		else {
			Expression initializer = null;
			if(lex.peek().getType().equals("=")) {
				require("=");
				initializer = new AssignExpression(new VarLiteral(id), expression());
			}
			
			return new VarDecl(typeTok.getPosition(), t, id, initializer);
		}
	}

	private Statement ifStmt() {
		Position start = require("IF").getPosition();
		
		require("(");
		Expression e = expression();
		require(")");
		
		Statement thenBody = statement();
		Statement elseBody = null;
		
		if(lex.peek().getType().equals("ELSE")) {
			lex.next();
			elseBody = statement();
		}
		
		return new IfStatement(e, thenBody, elseBody, start);
	}
	
	private Statement whileStmt() {
		Position start = require("WHILE").getPosition();
		
		require("(");
		Expression cond = expression();
		require(")");
		
		Statement body = statement();
		return new WhileStatement(cond, body, start);
	}
	
	private Statement forStmt() {
		Position start = require("FOR").getPosition();
		
		require("(");
		
		Expression init = null;
		if(!lex.peek().getType().equals(";"))
			init = expression();
		
		require(";");
		
		Expression cond = null;
		if(!lex.peek().getType().equals(";"))
			cond = expression();
		
		require(";");
		
		Expression action = null;
		if(!lex.peek().getType().equals(")"))
			action = expression();
		
		require(")");
		
		Statement body = statement();
		return new ForStatement(start, init, cond, action, body);
	}
	
	private Statement printStmt() {
		Position start = require("PRINT").getPosition();
		boolean parenthesized = lex.peek().getValue().equals("(");
		
		if(parenthesized) require("(");
		Expression e = expression();
		if(parenthesized) require(")");
		
		return new PrintStatement(start, e);
	}
	
	
	/* ************************* */
	/*        Expressions        */
	/* ************************* */
	
	private  Expression expression() {
		Expression left = logicalExpr();
		
		if(lex.peek().getType().endsWith("=")) {
			Token assignOp = lex.next();
			Expression right = expression();
			
			switch(assignOp.getType()) {
			case "=":
				left = new AssignExpression(left, right);
				break;
			case "+=":
				Expression add = new ArithmeticBinExpression(PLUS, left, right);
				left = new AssignExpression(left, add);
				break;
			case "-=":
				Expression min = new ArithmeticBinExpression(MINUS, left, right);
				left = new AssignExpression(left, min);
				break;
			case "*=":
				Expression mul = new ArithmeticBinExpression(MULT, left, right);
				left = new AssignExpression(left, mul);
				break;
			case "/=":
				Expression div = new ArithmeticBinExpression(DIV, left, right);
				left = new AssignExpression(left, div);
				break;
			case "%=":
				Expression mod = new ArithmeticBinExpression(MOD, left, right);
				left = new AssignExpression(left, mod);
				break;
			default:
				error("Expected assignment oprator but instead found \"%s\"", assignOp.getValue());
				return null;
			}
		}
		return left;
	}
	
	private Expression logicalExpr() {
		Expression left = equalityExpr();
		
		Token op;
		while((op = lex.peek()).getType().equals("OR_OP") || op.getType().equals("AND_OP")) {
			lex.next();
			Expression right = equalityExpr();
			
			switch(op.getType()) {
			case "OR_OP":
				left = new LogicalExpression(OR, left, right);
				break;
			case "AND_OP":
				left = new LogicalExpression(AND, left, right);
				break;
			}
		}
		return left;
	}
	
	private Expression equalityExpr() {
		Expression left = relationalExpr();
		
		Token op;
		while((op = lex.peek()).getType().equals("EQ_OP") || op.getType().equals("NEQ_OP")) {
			lex.next();
			Expression right = relationalExpr();
			
			switch(op.getType()) {
			case "EQ_OP":
				left = new EqualityExpression(EQ, left, right);
				break;
			case "NEQ_OP":
				left = new EqualityExpression(NEQ, left, right);
				break;
			}
		}
		return left;
	}
	
	private Expression relationalExpr() {
		Expression left = additiveExpr();
		
		Token op;
		while((op = lex.peek()).getType().equals("<") || op.getType().equals(">") ||
				op.getType().equals("GE_OP") || op.getType().equals("LE_OP")) {
			lex.next();
			Expression right = additiveExpr();
			
			switch(op.getType()) {
			case "<":
				left = new RelationalExpression(LT, left, right);
				break;
			case ">":
				left = new RelationalExpression(GT, left, right);
				break;
			case "LE_OP":
				left = new RelationalExpression(LE, left, right);
				break;
			case "GE_OP":
				left = new RelationalExpression(GE, left, right);
				break;
			}
		}
		return left;
	}
	
	private Expression additiveExpr() {
		Expression left = multiplicativeExpression();
		
		Token op;
		while((op = lex.peek()).getType().equals("+") || op.getType().equals("-")) {
			lex.next();
			Expression right = multiplicativeExpression();
			
			switch(op.getType()) {
			case "+":
				left = new ArithmeticBinExpression(PLUS, left, right);
				break;
			case "-":
				left = new ArithmeticBinExpression(MINUS, left, right);
				break;
			}
		}
		return left;
	}
	
	private Expression multiplicativeExpression() {
		Expression left = unaryExpr();
		
		Token op;
		while((op = lex.peek()).getType().equals("/") || op.getType().equals("*") ||
				op.getType().equals("%")) {
			lex.next();
			Expression right = unaryExpr();
			
			switch(op.getType()) {
			case "/":
				left = new ArithmeticBinExpression(DIV, left, right);
				break;
			case "*":
				left = new ArithmeticBinExpression(MULT, left, right);
				break;
			case "%":
				left = new ArithmeticBinExpression(MOD, left, right);
				break;
			}
		}
		return left;
	}

	private Expression unaryExpr() {
		if(lex.peek().getType().equals("!")) {
			Position pos = require("!").getPosition();
			return new LogicalNotExpression(unaryExpr(), pos);
		}
		if(lex.peek().getType().equals("+")) {
			require("+");
			return unaryExpr();
		}
		if(lex.peek().getType().equals("-")) {
			Position pos = require("-").getPosition();
			return new ArithmeticBinExpression(MULT, new IntegerLiteral(pos, -1), unaryExpr());
		}
		if(lex.peek().getType().equals("++")) {
			Position pos = require("++").getPosition();
			return new PreIncrementOperation(INCR, unaryExpr(), pos);
		}
		if(lex.peek().getType().equals("--")) {
			Position pos = require("--").getPosition();
			return new PreIncrementOperation(DECR, unaryExpr(), pos);
		}
		
		return postfixExpr();
	}
	
	private Expression postfixExpr() {
		Expression left = literal();
		
		Token op;
		while((op = lex.peek()).getType().equals("[") || op.getType().equals("++") || op.getType().equals("--")) {
			lex.next();
			
			switch(op.getType()) {
			case "[":
				left = new ArrayAccess(left, expression());
				require("]");
				break;
			case "++":
				left = new PostIncrementOperation(INCR, left);
				break;
			case "--":
				left = new PostIncrementOperation(DECR, left);
				break;
			}
		}
		
		return left;
	}
	
	private Expression literal() {
		Token litTok = lex.next();
		switch(litTok.getType()) {
		case "INT_CONST":
			try {
				return new IntegerLiteral(litTok.getPosition(), Integer.parseInt(litTok.getValue()));
			} catch(NumberFormatException ex) {
				error("int literal out of range");
			}
		case "FLOAT_CONST":
			float f = Float.parseFloat(litTok.getValue());
			if(Float.isInfinite(f)) error("float literal out of range");
			return new FloatLiteral(litTok.getPosition(), Float.parseFloat(litTok.getValue()));
		case "BOOL_CONST":
			return new BooleanLiteral(litTok.getPosition(), Boolean.parseBoolean(litTok.getValue()));
		case "STRING_CONST":
			String s = litTok.getValue();
			return new StringLiteral(litTok.getPosition(), unescapeJavaString(s.substring(1, s.length() - 1)));
		case "IDENTIFIER":
			return new VarLiteral(new Identifier(litTok.getPosition(), litTok.getValue()));
		case "(":
			Expression e = expression();
			require(")");
			return e;
		default:
			error("expected expression before \"%s\"", litTok.getValue());
			return null;
		}
	}
	
	private Token require(String tokType) {
		Token next = lex.next();
		if(!next.getType().equals(tokType)) {
			error("expected \"%s\" but instead found \"%s\"", tokType, next.getValue());
		}
		return next;
	}
	
	private void error(String format, Object... args) {
		//lex.curr() could return null. In that case just omit the "near or at" part of the error
		String tokenErr = "";
		Position pos = new Position(0, 0);
		if(lex.curr() != null) {
			tokenErr = "near or at token \"" + lex.curr().getValue() + "\" ";
			pos = lex.curr().getPosition();
		}
		throw new ParseException(String.format("Syntax error " + tokenErr + pos + ": " + format, args));
	}

	private String unescapeJavaString(String st) {
	    StringBuilder sb = new StringBuilder(st.length());
	    for (int i = 0; i < st.length(); i++) {
	        char ch = st.charAt(i);
	        if (ch == '\\') {
	            char nextChar = (i == st.length() - 1) ? '\\' : st.charAt(i + 1);
	            // Octal escape?
	            if (nextChar >= '0' && nextChar <= '7') {
	                String code = "" + nextChar;
	                i++;
	                if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
	                        && st.charAt(i + 1) <= '7') {
	                    code += st.charAt(i + 1);
	                    i++;
	                    if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
	                            && st.charAt(i + 1) <= '7') {
	                        code += st.charAt(i + 1);
	                        i++;
	                    }
	                }
	                sb.append((char) Integer.parseInt(code, 8));
	                continue;
	            }
	            switch (nextChar) {
	            case '\\':
	                ch = '\\';
	                break;
	            case 'b':
	                ch = '\b';
	                break;
	            case 'f':
	                ch = '\f';
	                break;
	            case 'n':
	                ch = '\n';
	                break;
	            case 'r':
	                ch = '\r';
	                break;
	            case 't':
	                ch = '\t';
	                break;
	            case '\"':
	                ch = '\"';
	                break;
	            case '\'':
	                ch = '\'';
	                break;
	            // Hex Unicode: u????
	            case 'u':
	                if (i >= st.length() - 5) {
	                    ch = 'u';
	                    break;
	                }
	                int code = Integer.parseInt(
	                        "" + st.charAt(i + 2) + st.charAt(i + 3)
	                                + st.charAt(i + 4) + st.charAt(i + 5), 16);
	                sb.append(Character.toChars(code));
	                i += 5;
	                continue;
	            }
	            i++;
	        }
	        sb.append(ch);
	    }
	    return sb.toString();
	}
	
}
