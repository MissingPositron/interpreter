package com.bamless.interpreter.parser.ast.visitor;

public interface Visitable {
	<T, A> T accept(GenericVisitor<T, A> v, A arg);
	<A> void accept(VoidVisitor<A> v, A arg);
}
