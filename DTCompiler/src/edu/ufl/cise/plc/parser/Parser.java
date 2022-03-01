package edu.ufl.cise.plc.parser;

import java.util.ArrayList;
import java.util.List;

import edu.ufl.cise.plc.IParser;
import edu.ufl.cise.plc.IToken;
import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.LexicalException;
import edu.ufl.cise.plc.PLCException;
import edu.ufl.cise.plc.SyntaxException;
import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.lexer.Lexer;

public class Parser implements IParser {

	Lexer l;
	IToken t;
	//ASTNode node;

	public Parser(String input) throws LexicalException {
		l = new Lexer(input);
		t = l.peek();
	}

	@Override
	public ASTNode parse() throws PLCException {
		return Expr();
	}
	
	Program Program() throws PLCException {
        IToken type;
        IToken name;
        List<NameDef> params = new ArrayList<NameDef>();
        List<ASTNode> decsAndStatements = new ArrayList<ASTNode>();

        if (isKind(Kind.TYPE, Kind.KW_VOID)) {
            type = consume();
            name = match(Kind.IDENT);
            match(Kind.LPAREN);
            if (!isKind(Kind.RPAREN)) {
                params.add(NameDef());
                while (!isKind(Kind.RPAREN)) {
                    match(Kind.COMMA);
                    params.add(NameDef());
                }
            }
            while (!isKind(Kind.EOF)) {
                if (isKind(Kind.TYPE)) {
                    decsAndStatements.add(Declaration());
                } else {
                    decsAndStatements.add(Statement());
                }
                match(Kind.SEMI);
            }
            return new Program(name, Types.Type.toType(type.getText()), name.getText(), params, decsAndStatements);
        }

        throw new SyntaxException("Expected type.", t.getSourceLocation());

    }

    NameDef NameDef() throws PLCException {
        IToken type = match(Kind.TYPE);
        Dimension dim = Dimension();
        IToken name = consume();
        if (dim != null)
            return new NameDefWithDim(type, type.getText(), name.getText(), dim);
        return new NameDef(type, type.getText(), name.getText());
    }

    Dimension Dimension() throws PLCException {
        if (isKind(Kind.LSQUARE)) {
            IToken f_selector = t;
            match(Kind.LSQUARE);
            Expr e1 = Expr();
            match(Kind.COMMA);
            Expr e2 = Expr();
            match(Kind.RSQUARE);
            return new Dimension(f_selector, e1, e2);
        }
        return null;
    }

    Declaration Declaration() throws PLCException {
        IToken f = t;
        NameDef ndef = NameDef();
        IToken op = null;
        Expr expr = null;
        if (isKind(Kind.ASSIGN, Kind.LARROW)) {
            op = consume();
            expr = Expr();
        }
        return new VarDeclaration(f, ndef, op, expr);
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
		Expr left = null;
		Expr right = null;
		left = LogicalAndExpr();
		
		while (isKind(Kind.OR)) {
			IToken op = t;
			consume();
			right = LogicalAndExpr();
			left = new BinaryExpr(f, left, op, right);
			}
		return left;
	}

	Expr LogicalAndExpr() throws PLCException {
		IToken f = t;
		Expr left = null;
		Expr right = null;
		left = ComparisonExpr();
		
		while (isKind(Kind.AND)) {
			IToken op = t;
			consume();
			right = ComparisonExpr();
			left = new BinaryExpr(f, left, op, right);
			}
		return left;
	}

	Expr ComparisonExpr() throws PLCException {
		IToken f = t;
		Expr left = null;
		Expr right = null;
		left = AdditiveExpr();
		
		while (isKind(Kind.GE, Kind.LE, Kind.GT, Kind.LT, Kind.EQUALS, Kind.NOT_EQUALS)) {
			IToken op = t;
			consume();
			right = AdditiveExpr();
			left = new BinaryExpr(f, left, op, right);
			}
		return left;
	}

	Expr AdditiveExpr() throws PLCException {
		IToken f = t;
		Expr left = null;
		Expr right = null;
		left = MultiplicativeExpr();
		
		while (isKind(Kind.PLUS, Kind.MINUS)) {
			IToken op = t;
			consume();
			right = MultiplicativeExpr();
			left = new BinaryExpr(f, left, op, right);
			}
		return left;
	}

	Expr MultiplicativeExpr() throws PLCException {
		IToken f = t;
		Expr left = null;
		Expr right = null;
		left = UnaryExpr();
		
		while (isKind(Kind.TIMES, Kind.DIV, Kind.MOD)) {
			IToken op = t;
			consume();
			right = UnaryExpr();
			left = new BinaryExpr(f, left, op, right);
			}
		return left;
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
			return new UnaryExprPostfix(f, primary, selector);
		else
			return primary;
	}

	Expr PrimaryExpr() throws PLCException {
		IToken f = t;
		if (isKind(Kind.BOOLEAN_LIT)) {
			return new BooleanLitExpr(consume());
		} else if (isKind(Kind.STRING_LIT)) {
			return new StringLitExpr(consume());
		} else if (isKind(Kind.INT_LIT)) {
			return new IntLitExpr(consume());
		} else if (isKind(Kind.FLOAT_LIT)) {
			return new FloatLitExpr(consume());
		} else if (isKind(Kind.IDENT)) {
			return new IdentExpr(consume());
		} else if (isKind(Kind.COLOR_CONST)) {
			return new ColorConstExpr(consume());
		} else if (isKind(Kind.KW_CONSOLE)) {
			return new IdentExpr(consume());
		} else if (isKind(Kind.LANGLE)) {
			consume();
			Expr temp1 = Expr();
			match(Kind.COMMA);
			Expr temp2 = Expr();
			match(Kind.COMMA);
			Expr temp3 = Expr();
			match(Kind.RANGLE);
			return new ColorExpr(f, temp1, temp2, temp3);
		} else if (isKind(Kind.LPAREN)) {
			consume();
			Expr temp = Expr();
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
	
	Statement Statement() throws PLCException {
		IToken f = t;
		if (isKind(Kind.IDENT)) { //covers assignment and read
			
			String name = consume().getText();
			
			PixelSelector selector = PixelSelector();
			
			if (isKind(Kind.ASSIGN)) { //assignment
				consume();
				Expr Expr = Expr();
				return new AssignmentStatement(f, name, selector, Expr);
				
			} else { //read
				match(Kind.LARROW);
				Expr Expr = Expr();
				return new ReadStatement(f, name, selector, Expr);
			}
			
		} else if (isKind(Kind.KW_WRITE)) {
			consume();
			Expr Expr1 = Expr();
			match(Kind.RARROW);
			Expr Expr2 = Expr();
			return new WriteStatement(f, Expr1, Expr2);
			
		}
		match(Kind.RETURN);
		Expr Expr = Expr();
		return new ReturnStatement(f, Expr);
		
	}

	IToken match(Kind k) throws PLCException {
		//System.out.println("matched: " + t.getText() + " and " + k);
		if (t.getKind() == k) {
			return consume();
		} else
			throw new SyntaxException("Illegal Token.", t.getSourceLocation());
	}

	IToken consume() throws PLCException {
		IToken curr = l.next();
		//System.out.println(curr.getText());
		t = l.peek();
		return curr;
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
