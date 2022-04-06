package edu.ufl.cise.plc;

import java.util.List;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.ast.ASTVisitor;
import edu.ufl.cise.plc.ast.AssignmentStatement;
import edu.ufl.cise.plc.ast.BinaryExpr;
import edu.ufl.cise.plc.ast.BooleanLitExpr;
import edu.ufl.cise.plc.ast.ColorConstExpr;
import edu.ufl.cise.plc.ast.ColorExpr;
import edu.ufl.cise.plc.ast.ConditionalExpr;
import edu.ufl.cise.plc.ast.ConsoleExpr;
import edu.ufl.cise.plc.ast.Dimension;
import edu.ufl.cise.plc.ast.FloatLitExpr;
import edu.ufl.cise.plc.ast.IdentExpr;
import edu.ufl.cise.plc.ast.IntLitExpr;
import edu.ufl.cise.plc.ast.NameDef;
import edu.ufl.cise.plc.ast.NameDefWithDim;
import edu.ufl.cise.plc.ast.PixelSelector;
import edu.ufl.cise.plc.ast.Program;
import edu.ufl.cise.plc.ast.ReadStatement;
import edu.ufl.cise.plc.ast.ReturnStatement;
import edu.ufl.cise.plc.ast.StringLitExpr;
import edu.ufl.cise.plc.ast.UnaryExpr;
import edu.ufl.cise.plc.ast.UnaryExprPostfix;
import edu.ufl.cise.plc.ast.VarDeclaration;
import edu.ufl.cise.plc.ast.WriteStatement;
import edu.ufl.cise.plc.ast.Types.Type;

public class CodeGenVisitor implements ASTVisitor {

	String packageName;
	String importStatements;

	public CodeGenVisitor(String packageName) {
		this.packageName = packageName;
	}

	@Override
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
		if (booleanLitExpr.getValue())
			((StringBuilder) arg).append("true");
		else
			((StringBuilder) arg).append("false");
		return arg;
	}

	@Override
	public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
		((StringBuilder) arg).append("\"\"\"\n" + stringLitExpr.getValue() + "\"\"\"");
		return arg;
	}

	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {

		if (intLitExpr.getCoerceTo() != null && intLitExpr.getCoerceTo() != Type.INT) {

			((StringBuilder) arg).append("(" + intLitExpr.getCoerceTo().name().toLowerCase() + ") ");
		}

		((StringBuilder) arg).append(intLitExpr.getValue());
		return arg;
	}

	@Override
	public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
		if (floatLitExpr.getCoerceTo() != null && floatLitExpr.getCoerceTo() != Type.FLOAT) {

			((StringBuilder) arg).append("( " + floatLitExpr.getCoerceTo().name().toLowerCase() + " ) ");

		}

		((StringBuilder) arg).append(floatLitExpr.getValue() + "f");
		return arg;
	}

	@Override
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException("thats messed up, this isnt in this assignment. -_-");
		// return arg;
	}

	@Override
	public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {

		String coerceType = switch (consoleExpr.getCoerceTo()) {
		// :)
		case BOOLEAN -> "Boolean";
		// case Type.COLOR -> ;
		// case Type.CONSOLE-> "";
		case FLOAT -> "Float";
		// case Type.IMAGE -> ;
		case INT -> "Integer";
		case STRING -> "String";
		// case Type.VOID -> ;
		default -> throw new IllegalArgumentException("Unexpected type value: " + consoleExpr.getCoerceTo().name());
		};

		((StringBuilder) arg).append("( " + coerceType + ") " // SUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUS---semicolon---------------v
				+ "ConsoleIO.readValueFromConsole( \"" + consoleExpr.getCoerceTo().name() + "\", \"Enter " + coerceType
				+ ":\")");
		
		importStatements += "import edu.ufl.cise.plc.runtime.ConsoleIO;\n";
		return arg;
	}

	@Override
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException("thats messed up, this isnt in this assignment. -_-");
		// return arg;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr unaryExpression, Object arg) throws Exception {
		if (unaryExpression.getOp().getKind() == Kind.BANG || unaryExpression.getOp().getKind() == Kind.MINUS) {
			((StringBuilder) arg).append("( " + unaryExpression.getOp().getText() + " "); 
					unaryExpression.getExpr().visit(this, arg);
					((StringBuilder) arg).append(" )");
		} else {
			throw new IllegalArgumentException("thats messed up, this isnt in this assignment. -_-");
		}
		return arg;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		((StringBuilder) arg).append("( ");
		binaryExpr.getLeft().visit(this, arg);
		((StringBuilder) arg).append(" " + binaryExpr.getOp().getText() + " ");
		binaryExpr.getRight().visit(this, arg);
		((StringBuilder) arg).append(" )");
		return arg;
	}

	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
		if (identExpr.getCoerceTo() != null && identExpr.getCoerceTo() != identExpr.getType()) {

			if (identExpr.getCoerceTo() == Type.STRING) {
				((StringBuilder) arg).append("(String) ");
			} else {
				((StringBuilder) arg).append("(" + identExpr.getCoerceTo().name().toLowerCase() + ") ");
			}
		}

		((StringBuilder) arg).append(identExpr.getFirstToken().getText());
		return arg;
	}

	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
		((StringBuilder) arg).append("( ");
		conditionalExpr.getCondition().visit(this, arg);
		((StringBuilder) arg).append(" ) ? ");
		conditionalExpr.getTrueCase().visit(this, arg);
		((StringBuilder) arg).append(" : ");
		conditionalExpr.getFalseCase().visit(this, arg);
		return arg;
	}

	@Override
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException("thats messed up, this isnt in this assignment. -_-");
		// return arg;
	}

	@Override
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException("thats messed up, this isnt in this assignment. -_-");
		// return arg;
	}

	@Override
	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
		((StringBuilder) arg).append(assignmentStatement.getName() + " = ");
		assignmentStatement.getExpr().visit(this, arg);
		((StringBuilder) arg).append(";\n");
		return arg;
	}

	@Override
	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		if (Type.CONSOLE == writeStatement.getDest().getType()) {
			((StringBuilder) arg).append("ConsoleIO.console.println( ");
			writeStatement.getSource().visit(this, arg);
			((StringBuilder) arg).append(" );\n");
		}
		return arg;
	}

	@Override
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {

		((StringBuilder) arg).append(readStatement.getName() + " = ");
		readStatement.getSource().visit(this, arg);
		((StringBuilder) arg).append(";\n");
		return arg;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		StringBuilder code = new StringBuilder();
		importStatements = "package " + packageName + ";\n";

		if (program.getReturnType() == Type.STRING) {
			code.append("public class " + program.getName() + " {\n" + "\tpublic static String apply(");
		} else {
			code.append("public class " + program.getName() + " {\n" + "\tpublic static " + program.getReturnType().name().toLowerCase()
					+ " apply(");
		}
		

		List<NameDef> params = program.getParams();

		for (int i = 0; i < params.size(); i++) {
			params.get(i).visit(this, code);
			if (i != params.size() - 1)
				code.append(", ");
		}
		code.append(") {\n");

		List<ASTNode> decsAndStatements = program.getDecsAndStatements();

		for (ASTNode node : decsAndStatements)
			node.visit(this, code);

		code.append("\t}\n}");

		return importStatements + code.toString();
	}

	@Override
	public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
		if (nameDef.getType() == Type.STRING) {
			((StringBuilder) arg).append("String " + nameDef.getName());
		} else {
			((StringBuilder) arg).append(nameDef.getType().name().toLowerCase() + " " + nameDef.getName());
		}
		return arg;
	}

	@Override
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException("thats messed up, this isnt in this assignment. -_-");
		// return arg;
	}

	@Override
	public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
		((StringBuilder) arg).append("return ");
		returnStatement.getExpr().visit(this, arg);
		((StringBuilder) arg).append(";\n");
		return arg;
	}

	@Override
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
		if (declaration.getOp() == null) {
			declaration.getNameDef().visit(this, arg);
			((StringBuilder) arg).append(";\n");
		} else if (declaration.getOp().getKind() == Kind.ASSIGN || declaration.getOp().getKind() == Kind.LARROW) {
			declaration.getNameDef().visit(this, arg);
			((StringBuilder) arg).append(" = ");
			declaration.getExpr().visit(this, arg);
			((StringBuilder) arg).append(";\n");
		} else {
			throw new UnsupportedOperationException("Not yet implemented");
		}
		return arg;
	}

	@Override
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException("thats messed up, this isnt in this assignment. -_-");
		// return arg;

	}

}
