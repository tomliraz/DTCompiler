package edu.ufl.cise.plc;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
import edu.ufl.cise.plc.ast.Declaration;
import edu.ufl.cise.plc.ast.Dimension;
import edu.ufl.cise.plc.ast.Expr;
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
import edu.ufl.cise.plc.ast.Types.Type;
import edu.ufl.cise.plc.ast.UnaryExpr;
import edu.ufl.cise.plc.ast.UnaryExprPostfix;
import edu.ufl.cise.plc.ast.VarDeclaration;
import edu.ufl.cise.plc.ast.WriteStatement;

import static edu.ufl.cise.plc.ast.Types.Type.*;

public class TypeCheckVisitor implements ASTVisitor {

	SymbolTable symbolTable = new SymbolTable();
	Program root;

	record Pair<T0, T1> (T0 t0, T1 t1) {
	}; // may be useful for constructing lookup tables.

	private void check(boolean condition, ASTNode node, String message) throws TypeCheckException {
		if (!condition) {
			throw new TypeCheckException(message, node.getSourceLoc());
		}
	}

	// The type of a BooleanLitExpr is always BOOLEAN.
	// Set the type in AST Node for later passes (code generation)
	// Return the type for convenience in this visitor.
	@Override
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
		booleanLitExpr.setType(Type.BOOLEAN);
		return Type.BOOLEAN;
	}

	@Override
	public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
		stringLitExpr.setType(Type.STRING);
		return Type.STRING;
	}

	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
		intLitExpr.setType(Type.INT);
		return Type.INT;
	}

	@Override
	public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
		floatLitExpr.setType(Type.FLOAT);
		return Type.FLOAT;
	}

	@Override
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		colorConstExpr.setType(Type.COLOR);
		return Type.COLOR;
	}

	@Override
	public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
		consoleExpr.setType(Type.CONSOLE);
		return Type.CONSOLE;
	}

	// Visits the child expressions to get their type (and ensure they are correctly
	// typed)
	// then checks the given conditions.
	@Override
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
		Type redType = (Type) colorExpr.getRed().visit(this, arg);
		Type greenType = (Type) colorExpr.getGreen().visit(this, arg);
		Type blueType = (Type) colorExpr.getBlue().visit(this, arg);
		check(redType == greenType && redType == blueType, colorExpr, "color components must have same type");
		check(redType == Type.INT || redType == Type.FLOAT, colorExpr, "color component type must be int or float");
		Type exprType = (redType == Type.INT) ? Type.COLOR : Type.COLORFLOAT;
		colorExpr.setType(exprType);
		return exprType;
	}

	// Maps forms a lookup table that maps an operator expression pair into result
	// type.
	// This more convenient than a long chain of if-else statements.
	// Given combinations are legal; if the operator expression pair is not in the
	// map, it is an error.
	Map<Pair<Kind, Type>, Type> unaryExprs = Map.of(new Pair<Kind, Type>(Kind.BANG, BOOLEAN), BOOLEAN,
			new Pair<Kind, Type>(Kind.MINUS, FLOAT), FLOAT, new Pair<Kind, Type>(Kind.MINUS, INT), INT,
			new Pair<Kind, Type>(Kind.COLOR_OP, INT), INT, new Pair<Kind, Type>(Kind.COLOR_OP, COLOR), INT,
			new Pair<Kind, Type>(Kind.COLOR_OP, IMAGE), IMAGE, new Pair<Kind, Type>(Kind.IMAGE_OP, IMAGE), INT);

	// Visits the child expression to get the type, then uses the above table to
	// determine the result type
	// and check that this node represents a legal combination of operator and
	// expression type.
	@Override
	public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
		// !, -, getRed, getGreen, getBlue
		Kind op = unaryExpr.getOp().getKind();
		Type exprType = (Type) unaryExpr.getExpr().visit(this, arg);
		// Use the lookup table above to both check for a legal combination of operator
		// and expression, and to get result type.
		Type resultType = unaryExprs.get(new Pair<Kind, Type>(op, exprType));
		check(resultType != null, unaryExpr, "incompatible types for unaryExpr");
		// Save the type of the unary expression in the AST node for use in code
		// generation later.
		unaryExpr.setType(resultType);
		// return the type for convenience in this visitor.
		return resultType;
	}

	// This method has several cases. Work incrementally and test as you go.
	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		Kind op = binaryExpr.getOp().getKind();

		Expr leftExpr = binaryExpr.getLeft();
		Expr rightExpr = binaryExpr.getRight();

		Type leftType = (Type) leftExpr.visit(this, arg);
		Type rightType = (Type) rightExpr.visit(this, arg);

		Type inferredType = switch (op) {
		case AND, OR -> {
			check(leftType == Type.BOOLEAN, binaryExpr, "incompatible types for binaryExpr");
			check(rightType == Type.BOOLEAN, binaryExpr, "incompatible types for binaryExpr");
			yield Type.BOOLEAN;
		}
		case EQUALS, NOT_EQUALS -> {
			check(leftType == rightType, binaryExpr, "incompatible types for binaryExpr");
			yield Type.BOOLEAN;
		}
		case PLUS, MINUS -> {
			if (leftType == Type.INT) {
				if (rightType == Type.INT)
					yield Type.INT;
				else if (rightType == Type.FLOAT) {
					leftExpr.setCoerceTo(Type.FLOAT);
					yield Type.FLOAT;
				}
			} else if (leftType == Type.FLOAT) {
				if (rightType == Type.INT) {
					rightExpr.setCoerceTo(Type.FLOAT);
					yield Type.FLOAT;
				} else if (rightType == Type.FLOAT)
					yield Type.FLOAT;
			} else if (leftType == Type.COLOR) {
				if (rightType == Type.COLOR)
					yield Type.COLOR;
				else if (rightType == Type.COLORFLOAT) {
					leftExpr.setCoerceTo(Type.COLORFLOAT);
					yield Type.COLORFLOAT;
				}
			} else if (leftType == Type.COLORFLOAT) {
				if (rightType == Type.COLOR) {
					leftExpr.setCoerceTo(Type.COLORFLOAT);
					yield Type.COLORFLOAT;
				} else if (rightType == Type.COLORFLOAT)
					yield Type.COLORFLOAT;
			} else if (leftType == Type.IMAGE && rightType == Type.IMAGE)
				yield Type.IMAGE;

			throw new TypeCheckException("incompatible types for binaryExpr", binaryExpr.getSourceLoc());
		}
		case TIMES, DIV, MOD -> {
			if (leftType == Type.INT) {
				if (rightType == Type.INT)
					yield Type.INT;
				else if (rightType == Type.FLOAT) {
					leftExpr.setCoerceTo(Type.FLOAT);
					yield Type.FLOAT;
				} else if (rightType == Type.COLOR) {
					leftExpr.setCoerceTo(Type.COLOR);
					yield Type.COLOR;
				}
			} else if (leftType == Type.FLOAT) {
				if (rightType == Type.INT) {
					rightExpr.setCoerceTo(Type.FLOAT);
					yield Type.FLOAT;
				} else if (rightType == Type.FLOAT)
					yield Type.FLOAT;
				else if (rightType == Type.COLOR) {
					leftExpr.setCoerceTo(Type.COLORFLOAT);
					rightExpr.setCoerceTo(Type.COLORFLOAT);
					yield Type.COLORFLOAT;
				}
			} else if (leftType == Type.COLOR) {
				if (rightType == Type.COLOR)
					yield Type.COLOR;
				else if (rightType == Type.COLORFLOAT) {
					leftExpr.setCoerceTo(Type.COLORFLOAT);
					yield Type.COLORFLOAT;
				} else if (rightType == Type.INT) {
					rightExpr.setCoerceTo(Type.COLOR);
					yield Type.COLOR;
				} else if (rightType == Type.FLOAT) {
					leftExpr.setCoerceTo(Type.COLORFLOAT);
					rightExpr.setCoerceTo(Type.COLORFLOAT);
					yield Type.COLORFLOAT;
				}
			} else if (leftType == Type.COLORFLOAT) {
				if (rightType == Type.COLOR) {
					leftExpr.setCoerceTo(Type.COLORFLOAT);
					yield Type.COLORFLOAT;
				} else if (rightType == Type.COLORFLOAT)
					yield Type.COLORFLOAT;
			} else if (leftType == Type.IMAGE
					&& (rightType == Type.IMAGE || rightType == Type.INT || rightType == Type.FLOAT)) {
				yield Type.IMAGE;
			}
			throw new TypeCheckException("incompatible types for binaryExpr", binaryExpr.getSourceLoc());
		}
		case LT, LE, GT, GE -> {
			if (leftType == Type.INT) {
				if (rightType == Type.INT)
					yield Type.BOOLEAN;
				else if (rightType == Type.FLOAT) {
					leftExpr.setCoerceTo(Type.FLOAT);
					yield Type.BOOLEAN;
				}
			} else if (leftType == Type.FLOAT) {
				if (rightType == Type.INT) {
					rightExpr.setCoerceTo(Type.FLOAT);
					yield Type.BOOLEAN;
				} else if (rightType == Type.FLOAT)
					yield Type.BOOLEAN;
			}
			throw new TypeCheckException("incompatible types for binaryExpr", binaryExpr.getSourceLoc());
		}
		default -> throw new TypeCheckException("incompatible operation for binaryExpr", binaryExpr.getSourceLoc());
		};

		binaryExpr.setType(inferredType);
		return inferredType;

	}

	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
		String name = identExpr.getText();

		Declaration dec = symbolTable.lookup(name);
		check(dec != null, identExpr, "undefined identifier " + name);
		check(dec.isInitialized(), identExpr, "using uninitialized variable");
		identExpr.setDec(dec);

		Type type = dec.getType();
		identExpr.setType(type);
		return type;
	}

	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
		Type conditionalType = (Type) conditionalExpr.getCondition().visit(this, arg);
		check(conditionalType == Type.BOOLEAN, conditionalExpr, "Conditional must be of type boolean.");
		Type trueCaseType = (Type) conditionalExpr.getTrueCase().visit(this, arg);
		Type falseCaseType = (Type) conditionalExpr.getFalseCase().visit(this, arg);

		// TODO: why do true case and false case have to be equal ???
		check(trueCaseType == falseCaseType, conditionalExpr, "True case type and false case type must match.");

		conditionalExpr.setType(trueCaseType);
		return trueCaseType;
	}

	@Override
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {
		Type width = (Type) dimension.getWidth().visit(this, arg);
		Type height = (Type) dimension.getHeight().visit(this, arg);

		check(width == Type.INT && height == Type.INT, dimension, "width and height must be of type int");

		return null;
	}

	@Override
	// This method can only be used to check PixelSelector objects on the right hand
	// side of an assignment.
	// Either modify to pass in context info and add code to handle both cases, or
	// when on left side
	// of assignment, check fields from parent assignment statement.
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		Type xType = (Type) pixelSelector.getX().visit(this, arg);
		check(xType == Type.INT, pixelSelector.getX(), "only ints as pixel selector components");
		Type yType = (Type) pixelSelector.getY().visit(this, arg);
		check(yType == Type.INT, pixelSelector.getY(), "only ints as pixel selector components");
		return null;
	}

	Map<Pair<Type, Type>, Type> assignment_NotImage = Map.of(new Pair<Type, Type>(Type.INT, Type.FLOAT), Type.INT,
			new Pair<Type, Type>(Type.FLOAT, Type.INT), Type.FLOAT, new Pair<Type, Type>(Type.INT, Type.COLOR),
			Type.INT, new Pair<Type, Type>(Type.COLOR, Type.INT), Type.COLOR);

	Map<Pair<Type, Type>, Type> assignment_ImageNoSelector = Map.of(new Pair<Type, Type>(Type.IMAGE, Type.INT),
			Type.COLOR, new Pair<Type, Type>(Type.IMAGE, Type.FLOAT), Type.COLORFLOAT,
			new Pair<Type, Type>(Type.IMAGE, Type.COLOR), Type.COLOR, new Pair<Type, Type>(Type.IMAGE, Type.COLORFLOAT),
			Type.COLORFLOAT);

	@Override
	// This method several cases--you don't have to implement them all at once.
	// Work incrementally and systematically, testing as you go.
	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {

		// TODO: INCOMPLETE

		String name = assignmentStatement.getName();
		Declaration dec = symbolTable.lookup(name);
		check(dec != null, assignmentStatement, "undefined identifier " + name);
		Type targetType = dec.getType();
		Expr expr = assignmentStatement.getExpr();
		Type exprType = (Type) expr.visit(this, arg);
		dec.setInitialized(true);

		if (targetType != Type.IMAGE) {
			check(assignmentStatement.getSelector() == null, assignmentStatement, "Did not expect a pixel selector");
			Type coerceType = assignment_NotImage.get(new Pair<Type, Type>(targetType, exprType));
			if (targetType == exprType || coerceType != null) {
				if (targetType != exprType)
					expr.setCoerceTo(coerceType);
			} else {
				check(false, assignmentStatement, "incompatible types in assignment statement");
			}
		} else if (targetType == Type.IMAGE && assignmentStatement.getSelector() == null) {
			Type coerceType = assignment_ImageNoSelector.get(new Pair<Type, Type>(targetType, exprType));
			if (targetType == exprType || coerceType != null) {
				if (targetType != exprType)
					expr.setCoerceTo(coerceType);
			} else {
				check(false, assignmentStatement, "incompatible types in assignment statement");
			}
		} else if (targetType == Type.IMAGE && assignmentStatement.getSelector() != null) {
			assignmentStatement.getSelector().visit(this, arg);
			if (assignmentStatement.getSelector().getX().getType() != null && true) { // wronggnngngngng

			}

		}

		// TODO: implement this method
		throw new UnsupportedOperationException("Unimplemented visit method.");
	}

	@Override
	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		Type sourceType = (Type) writeStatement.getSource().visit(this, arg);
		Type destType = (Type) writeStatement.getDest().visit(this, arg);
		check(destType == Type.STRING || destType == Type.CONSOLE, writeStatement,
				"illegal destination type for write");
		check(sourceType != Type.CONSOLE, writeStatement, "illegal source type for write");
		return null;
	}

	@Override
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
		String name = readStatement.getName();
		Declaration dec = symbolTable.lookup(readStatement.getName());
		check(dec != null, readStatement, "undefined identifier " + name);
		Type rhsType = (Type) readStatement.getSource().visit(this, arg);

		check(readStatement.getSelector() == null, readStatement, "A read statement cannot have a PixelSelector");
		check(rhsType == Type.CONSOLE || rhsType == Type.STRING, readStatement,
				"The right hand side type must be CONSOLE or STRING");

		dec.setInitialized(true);
		readStatement.setTargetDec(dec);
		return null;
	}

	@Override
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {

		declaration.getNameDef().visit(this, arg);
		declaration.setInitialized(true);
		Type right = null;
		Type left = declaration.getNameDef().getType();
		if (declaration.getExpr() != null) {
			right = (Type) declaration.getExpr().visit(this, arg);
		}

		if (left == Type.IMAGE) {
			check(right == Type.IMAGE || declaration.getDim() != null, declaration,
					"images must have a dimension or image for initialization");
		}
		if (declaration.getDim() != null)
			declaration.getDim().visit(this, arg);

		if (declaration.getOp() != null) {
			if (declaration.getOp().getKind() == Kind.ASSIGN) {

			} else if (declaration.getOp().getKind() == Kind.LARROW) {
				check(right == Type.CONSOLE || right == Type.STRING, declaration, "must have type console or string");
			}
		}

		
		throw new UnsupportedOperationException("Unimplemented visit method.");
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		// TODO: this method is incomplete, finish it.

		// Save root of AST so return type can be accessed in return statements
		root = program;

		// Check declarations and statements
		List<ASTNode> decsAndStatements = program.getDecsAndStatements();
		for (ASTNode node : decsAndStatements) {
			node.visit(this, arg);
		}
		return program;
	}

	@Override
	public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
		if (symbolTable.insert(nameDef.getName(), nameDef) == false) {
			throw new TypeCheckException("variable " + nameDef.getName() + "already declared", nameDef.getSourceLoc());
		}
		return nameDef.getType();
	}

	@Override
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		if (symbolTable.insert(nameDefWithDim.getName(), nameDefWithDim) == false) {
			throw new TypeCheckException("variable " + nameDefWithDim.getName() + "already declared",
					nameDefWithDim.getSourceLoc());
		}
		return nameDefWithDim.getType();
	}

	@Override
	public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
		Type returnType = root.getReturnType(); // This is why we save program in visitProgram.
		Type expressionType = (Type) returnStatement.getExpr().visit(this, arg);
		check(returnType == expressionType, returnStatement, "return statement with invalid type");
		return null;
	}

	@Override
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		Type expType = (Type) unaryExprPostfix.getExpr().visit(this, arg);
		check(expType == Type.IMAGE, unaryExprPostfix, "pixel selector can only be applied to image");
		unaryExprPostfix.getSelector().visit(this, arg);
		unaryExprPostfix.setType(Type.INT);
		unaryExprPostfix.setCoerceTo(COLOR);
		return Type.COLOR;
	}

}
