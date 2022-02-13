package dtcompiler.parser.impl;

import dtcompiler.lexer.impl.Lexer;
import edu.ufl.cise.plc.IParser;
import edu.ufl.cise.plc.IToken;
import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.LexicalException;
import edu.ufl.cise.plc.PLCException;
import edu.ufl.cise.plc.SyntaxException;
import edu.ufl.cise.plc.ast.*;
/*import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.ast.BinaryExpr;
import edu.ufl.cise.plc.ast.BooleanLitExpr;
import edu.ufl.cise.plc.ast.Expr;
import edu.ufl.cise.plc.ast.FloatLitExpr;
import edu.ufl.cise.plc.ast.IdentExpr;
import edu.ufl.cise.plc.ast.IntLitExpr;
import edu.ufl.cise.plc.ast.StringLitExpr;
import edu.ufl.cise.plc.ast.ConditionalExpr;
import edu.ufl.cise.plc.ast.UnaryExpr;
import edu.ufl.cise.plc.ast.UnaryExprPostfix;
import edu.ufl.cise.plc.ast.PixelSelector;*/

public class Parser implements IParser {

	Lexer l;
	IToken t;
	ASTNode node;

	public Parser(String input) throws LexicalException {
		l = new Lexer(input);
		t = l.next();
	}

	@Override
	public ASTNode parse() throws PLCException {
		return Expr();
	}

	Expr Expr() throws PLCException {
		if (t.getKind() == Kind.KW_IF) {
			return ConditionalExpr();
		} else {
			return LogicalOrExpr();
		}
	}

	ConditionalExpr ConditionalExpr() throws PLCException {
		IToken f = t;
		match(Kind.KW_IF);
		match(Kind.LPAREN);
		Expr conditional = Expr();
		match(Kind.RPAREN);
		Expr trueClause = Expr();
		match(Kind.KW_ELSE);
		Expr falseClause = Expr();
		match(Kind.KW_FI);
		return new ConditionalExpr(f, conditional, trueClause, falseClause);

	}

	Expr LogicalOrExpr() throws PLCException {
		IToken f = t;
		Expr expr = LogicalAndExpr();
		if (isKind(Kind.OR)) {
			return new BinaryExpr(f, expr, match(Kind.OR), LogicalOrExpr());
		}
		return expr;
	}

	Expr LogicalAndExpr() throws PLCException {
		IToken f = t;
		Expr expr = ComparisonExpr();
		if (isKind(Kind.AND)) {
			return new BinaryExpr(f, expr, match(Kind.AND), LogicalAndExpr());
		}
		return expr;
	}

	Expr ComparisonExpr() throws PLCException {
		IToken f = t;
		Expr expr = AdditiveExpr();
		if (isKind(Kind.GT, Kind.LT, Kind.GE, Kind.LE, Kind.EQUALS, Kind.NOT_EQUALS)) {
			return new BinaryExpr(f, expr, consume(), ComparisonExpr());
		}
		return expr;
	}

	Expr AdditiveExpr() throws PLCException {
		IToken f = t;
		Expr expr = MultiplicativeExpr();
		if (isKind(Kind.PLUS, Kind.MINUS)) {
			return new BinaryExpr(f, expr, consume(), AdditiveExpr());
		}
		return expr;
	}

	Expr MultiplicativeExpr() throws PLCException {
		IToken f = t;
		Expr expr = UnaryExpr();
		if (isKind(Kind.PLUS, Kind.MINUS)) {
			return new BinaryExpr(f, expr, consume(), MultiplicativeExpr());
		}
		return expr;
	}

	Expr UnaryExpr() throws PLCException {
		IToken f = t;
		if (isKind(Kind.BANG, Kind.MINUS, Kind.COLOR_OP, Kind.IMAGE_OP)) {
			return new UnaryExpr(f, consume(), UnaryExpr());
		} else {
			return UnaryExprPostfix();
		}
	}

	Expr UnaryExprPostfix() throws PLCException {
		IToken f = t;
		Expr primary = PrimaryExpr();
		PixelSelector selector = PixelSelector();
		
		if(selector != null)
			return new UnaryExprPostfix(f, PrimaryExpr(), PixelSelector());
		else
			return primary;
	}

	Expr PrimaryExpr() throws PLCException {
		IToken f = t;
		if (isKind(Kind.BOOLEAN_LIT)) {
			System.out.println(t.getKind());
			return new BooleanLitExpr(t);
		} else if (isKind(Kind.STRING_LIT)) {
			return new StringLitExpr(t);
		} else if (isKind(Kind.INT_LIT)) {
			return new IntLitExpr(t);
		} else if (isKind(Kind.FLOAT_LIT)) {
			return new FloatLitExpr(t);
		} else if (isKind(Kind.IDENT)) {
			return new IdentExpr(t);
		} else if (isKind(Kind.LPAREN)) {
			Expr temp;
			match(Kind.LPAREN);
			temp = Expr();
			match(Kind.RPAREN);
			return temp;
		} else {
			throw new SyntaxException("Illegal token.", f.getSourceLocation());
		}
	}

	PixelSelector PixelSelector() throws PLCException {
		if (isKind(Kind.LSQUARE)) {
			IToken f_selector = t;
			match(Kind.LSQUARE);
			Expr e1 = Expr();
			match(Kind.COMMA);
			Expr e2 = Expr();
			match(Kind.RSQUARE);
			return new PixelSelector(f_selector, e1, e2);
		}
		return null;
	}

	IToken match(Kind k) throws PLCException {
		if (t.getKind() == k) {
			return consume();
		} else
			throw new SyntaxException("Illegal Token.", t.getSourceLocation());
	}

	IToken consume() throws PLCException {
		t = l.next();
		return t;
	}

	protected boolean isKind(Kind kind) {
		return t.getKind() == kind;
	}

	protected boolean isKind(Kind... kinds) {
		for (Kind k : kinds) {
			if (k == t.getKind())
				return true;
		}
		return false;
	}
}
