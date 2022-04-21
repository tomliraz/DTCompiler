package edu.ufl.cise.plc;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
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
	List<String> importStatements;

	public CodeGenVisitor(String packageName) {
		this.packageName = packageName;
		this.importStatements = new ArrayList<String>();
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

		case BOOLEAN -> "Boolean";
		case COLOR -> "ColorTuple";
		// case Type.CONSOLE-> "";
		case FLOAT -> "Float";
		// case Type.IMAGE -> ;
		case INT -> "Integer";
		case STRING -> "String";
		// case Type.VOID -> ;
		default -> throw new IllegalArgumentException("Unexpected type value: " + consoleExpr.getCoerceTo().name());
		};

		if (coerceType.equals("ColorTuple"))
			((StringBuilder) arg).append("( " + coerceType + ") " + "ConsoleIO.readValueFromConsole( \""
					+ consoleExpr.getCoerceTo().name() + "\", \"Enter Red, Green, and Blue values:\")");
		else
			((StringBuilder) arg).append("( " + coerceType + ") " + "ConsoleIO.readValueFromConsole( \""
					+ consoleExpr.getCoerceTo().name() + "\", \"Enter " + coerceType + ":\")");

		addImportStatement("import edu.ufl.cise.plc.runtime.ConsoleIO;\n");
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
		} else if (unaryExpression.getOp().getKind() == Kind.COLOR_OP) {
			addImportStatement("import edu.ufl.cise.plc.runtime.ColorTuple");
			if (unaryExpression.getExpr().getType() == Type.INT || unaryExpression.getExpr().getType() == Type.COLOR) {
				((StringBuilder) arg).append("ColorTuple." + unaryExpression.getOp().getText() + "(");
				unaryExpression.getExpr().visit(this, arg);
				((StringBuilder) arg).append(")");
			} else if (unaryExpression.getExpr().getType() == Type.IMAGE) {
				addImportStatement("import edu.ufl.cise.plc.runtime.ImageOps");
				((StringBuilder) arg).append("ImageOps.extract");
				((StringBuilder) arg).append(unaryExpression.getOp().getText().substring(3) + "(");
				unaryExpression.getExpr().visit(this, arg);
				((StringBuilder) arg).append(")");
			}
		}
		((StringBuilder) arg).append(";\n");
		return arg;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		((StringBuilder) arg).append("( ");
		if (binaryExpr.getRight().getType() == Type.STRING && binaryExpr.getLeft().getType() == Type.STRING) {
			binaryExpr.getLeft().visit(this, arg);
			Kind op = binaryExpr.getOp().getKind();

			if (op == Kind.EQUALS) {
				((StringBuilder) arg).append(".equals( ");
				binaryExpr.getRight().visit(this, arg);
				((StringBuilder) arg).append(" )");
			} else if (op == Kind.NOT_EQUALS) {
				((StringBuilder) arg).append(".equals( ");
				binaryExpr.getRight().visit(this, arg);
				((StringBuilder) arg).append(" ) == false");
			} else {
				throw new IllegalArgumentException(
						"only = and != are valid for strings, this should never run though.");
			}
		} else if (binaryExpr.getRight().getType() == Type.COLOR && binaryExpr.getLeft().getType() == Type.COLOR) {
			addImportStatement("import edu.ufl.cise.plc.runtime.ImageOps;\n");
			((StringBuilder) arg)
					.append("ImageOps.binaryTupleOp(ImageOps.OP." + binaryExpr.getOp().getKind().toString() + ", ");
			binaryExpr.getLeft().visit(this, arg);
			((StringBuilder) arg).append(", ");
			binaryExpr.getRight().visit(this, arg);
			((StringBuilder) arg).append(")");
		} else if (binaryExpr.getRight().getType() == Type.IMAGE && binaryExpr.getLeft().getType() == Type.IMAGE) {
			addImportStatement("import edu.ufl.cise.plc.runtime.ImageOps;\n");
			((StringBuilder) arg)
					.append("ImageOps.binaryImageImageOp(ImageOps.OP." + binaryExpr.getOp().getKind().toString());
			((StringBuilder) arg).append(", ");
			binaryExpr.getLeft().visit(this, arg);
			((StringBuilder) arg).append(", ");
			binaryExpr.getRight().visit(this, arg);
			((StringBuilder) arg).append(")");
		} else if (binaryExpr.getRight().getType() == Type.COLOR && binaryExpr.getLeft().getType() == Type.IMAGE) {
			addImportStatement("import edu.ufl.cise.plc.runtime.ImageOps;\n");
			((StringBuilder) arg)
					.append("ImageOps.binaryImageScalarOp(ImageOps.OP." + binaryExpr.getOp().getKind().toString());
			((StringBuilder) arg).append(", ");
			binaryExpr.getLeft().visit(this, arg);
			((StringBuilder) arg).append(", ColorTuple.makePackedColor(ColorTuple.getRed(");
			binaryExpr.getRight().visit(this, arg);
			((StringBuilder) arg).append("), ColorTuple.getGreen(");
			binaryExpr.getRight().visit(this, arg);
			((StringBuilder) arg).append("), ColorTuple.getBlue(");
			binaryExpr.getRight().visit(this, arg);
			((StringBuilder) arg).append("))");
		} else if (binaryExpr.getRight().getType() == Type.INT && binaryExpr.getLeft().getType() == Type.IMAGE) {
			addImportStatement("import edu.ufl.cise.plc.runtime.ImageOps;\n");
			((StringBuilder) arg)
					.append("(ImageOps.binaryImageScalarOp(ImageOps.OP." + binaryExpr.getOp().getKind().toString());
			((StringBuilder) arg).append(", ");
			binaryExpr.getLeft().visit(this, arg);
			((StringBuilder) arg).append(", ");
			binaryExpr.getRight().visit(this, arg);
			((StringBuilder) arg).append(")");
		} else {
			binaryExpr.getLeft().visit(this, arg);
			((StringBuilder) arg).append(" " + binaryExpr.getOp().getText() + " ");
			binaryExpr.getRight().visit(this, arg);
		}

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
		((StringBuilder) arg).append("( ( ");
		conditionalExpr.getCondition().visit(this, arg);
		((StringBuilder) arg).append(" ) ? ");
		conditionalExpr.getTrueCase().visit(this, arg);
		((StringBuilder) arg).append(" : ");
		conditionalExpr.getFalseCase().visit(this, arg);
		((StringBuilder) arg).append(" )");
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
		if (assignmentStatement.getExpr().getCoerceTo() != null) {
			if (assignmentStatement.getExpr().getCoerceTo() == Type.STRING) {
				((StringBuilder) arg).append("(String) ");
			} else {
				((StringBuilder) arg)
						.append("(" + assignmentStatement.getExpr().getCoerceTo().name().toLowerCase() + ") ");
			}
		}
		assignmentStatement.getExpr().visit(this, arg);
		((StringBuilder) arg).append(";\n");
		return arg;
	}

	@Override
	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		
		if(writeStatement.getSource().getType() == Type.IMAGE && writeStatement.getDest().getType() == Type.CONSOLE) {
			addImportStatement("import edu.ufl.cise.plc.runtime.ConsoleIO;\n");
			((StringBuilder) arg).append("ConsoleIO.displayImageOnScreen(" + writeStatement.getSource().getText());
			((StringBuilder) arg).append(")");
		} else if(writeStatement.getSource().getType() == Type.IMAGE && writeStatement.getDest().getType() == Type.STRING) {
			addImportStatement("import edu.ufl.cise.plc.runtime.FileURLIO;\n");
			((StringBuilder) arg).append("FileURLIO.writeImage(" + writeStatement.getSource().getText());
			((StringBuilder) arg).append(", " + writeStatement.getDest().getText());
			((StringBuilder) arg).append(")");
		} else if(writeStatement.getDest().getType() == Type.STRING) {
			addImportStatement("import edu.ufl.cise.plc.runtime.FileURLIO;\n");
			((StringBuilder) arg).append("FileURLIO.writeValue(" + writeStatement.getSource().getText());
			((StringBuilder) arg).append(", " + writeStatement.getDest().getText());
			((StringBuilder) arg).append(")");
		}
		else {
			addImportStatement("import edu.ufl.cise.plc.runtime.ConsoleIO;\n");
			((StringBuilder) arg).append("ConsoleIO.console.println(");
			writeStatement.getSource().visit(this, arg);
			((StringBuilder) arg).append(")");
		}
		((StringBuilder) arg).append(";\n");
		return arg;
	}

	@Override
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
		((StringBuilder) arg).append(readStatement.getName() + " = ");

		if (readStatement.getTargetDec().getType() == Type.IMAGE) {
			((StringBuilder) arg).append(" = FileURLIO.readImage(");
			readStatement.getSource().visit(this, arg);
			
			if (readStatement.getTargetDec().getDim() != null) {
				((StringBuilder) arg).append(", ");
				readStatement.getTargetDec().getDim().visit(this, arg);
			}
			
			((StringBuilder) arg).append(");\nFileURLIO.closeFiles()");
		} else {
			readStatement.getSource().visit(this, arg);
		}
		((StringBuilder) arg).append(";\n");
		return arg;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		StringBuilder code = new StringBuilder();
		addImportStatement("package " + packageName + ";\n");

		if (program.getReturnType() == Type.STRING) {
			code.append("public class " + program.getName() + " {\n" + "\tpublic static String apply(");
		} else {
			code.append("public class " + program.getName() + " {\n" + "\tpublic static "
					+ program.getReturnType().name().toLowerCase() + " apply(");
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
		String imports = "";

		for (String statement : importStatements) {
			imports += statement;
		}

		return imports + code.toString();
	}

	@Override
	public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
		if (nameDef.getType() == Type.STRING) {
			((StringBuilder) arg).append("String " + nameDef.getName());
		} else if (nameDef.getType() == Type.IMAGE) {
			addImportStatement("import java.awt.image.BufferedImage;\n");
			((StringBuilder) arg).append("BufferedImage " + nameDef.getName());
		} else if (nameDef.getType() == Type.COLOR) {
			addImportStatement("import edu.ufl.cise.plc.runtime.ColorTuple;\n");
			((StringBuilder) arg).append("ColorTuple " + nameDef.getName());
		} else {
			((StringBuilder) arg).append(nameDef.getType().name().toLowerCase() + " " + nameDef.getName());
		}
		return arg;
	}

	@Override
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		return visitNameDef(nameDefWithDim, arg);
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
		declaration.getNameDef().visit(this, arg);
		if (declaration.getType() != Type.IMAGE && declaration.getType() != Type.COLOR) {
			if (declaration.getOp() != null) {
				((StringBuilder) arg).append(" = ");
				if (declaration.getExpr().getCoerceTo() != null) {
					if (declaration.getExpr().getCoerceTo() == Type.STRING) {
						((StringBuilder) arg).append("(String) ");
					} else {
						((StringBuilder) arg)
								.append("(" + declaration.getExpr().getCoerceTo().name().toLowerCase() + ") ");
					}
				}
				declaration.getExpr().visit(this, arg);

			}
		} else if (declaration.getType() == Type.IMAGE) {
			((StringBuilder) arg).append(" = ");
			if (declaration.getOp() != null) {
				addImportStatement("import edu.ufl.cise.plc.runtime.FileURLIO;\n");

				if (declaration.getNameDef().getDim() == null) {
					((StringBuilder) arg).append("FileURLIO.readImage(");
					declaration.getExpr().visit(this, arg);
					((StringBuilder) arg).append(")");
				} else {
					((StringBuilder) arg).append("FileURLIO.readImage(");
					declaration.getExpr().visit(this, arg);
					((StringBuilder) arg).append(", ");
					declaration.getDim().visit(this, arg);
					((StringBuilder) arg).append(")");
				}
			} else {
				if (declaration.getNameDef().getDim() != null) {
					((StringBuilder) arg).append("new BufferedImage(");
					declaration.getDim().visit(this, arg);
					((StringBuilder) arg).append(", BufferedImage.TYPE_INT_RGB)");
				} else {
					throw new Exception("Type checker should have handled this.");
				}
			}
		} else {
			if (declaration.getOp() != null) {
				((StringBuilder) arg).append(" = ");
				declaration.getExpr().visit(this, arg);
			}
		}
		((StringBuilder) arg).append(";\n");
		return arg;
	}

	@Override
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException("thats messed up, this isnt in this assignment. -_-");
		// return arg;

	}

	private void addImportStatement(String importStatement) {
		if (!importStatements.contains(importStatement)) {
			importStatements.add(importStatement);
		}
	}

}
