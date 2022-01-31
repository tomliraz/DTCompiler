package dtcompiler.lexer.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import edu.ufl.cise.plc.ILexer;
import edu.ufl.cise.plc.IToken;
import edu.ufl.cise.plc.LexicalException;
import edu.ufl.cise.plc.IToken.Kind;

public class Lexer implements ILexer {

	List<Token> tokens;
	int position;

	final HashSet<String> type = new HashSet<>(
			Arrays.asList("string", "int", "float", "boolean", "color", "image", "void"));
	final HashSet<String> image_op = new HashSet<>(Arrays.asList("getWidth", "getHeight"));
	final HashSet<String> color_op = new HashSet<>(Arrays.asList("getRed", "getGreen", "getBlue"));
	final HashSet<String> color_const = new HashSet<>(Arrays.asList("BLACK", "BLUE", "CYAN", "DARK_GRAY", "GRAY",
			"GREEN", "LIGHT_GRAY", "MAGENTA", "ORANGE", "PINK", "RED", "WHITE", "YELLOW"));
	final HashSet<String> boolean_lit = new HashSet<>(Arrays.asList("true", "false"));

	public static enum State {
		START, IN_IDENT, IS_ERROR, DIGIT, START_ZERO, HAVE_DOT, IS_LT, IS_GT, IS_EQ, IN_STRING, IN_STRING_ESC, IS_DASH, IS_EP
	}

	public Lexer(String input) throws LexicalException {
		tokens = new ArrayList<Token>();
		position = 0;

		State state = State.START;
		int line = 0;
		int col = 0;
		int tokenLine = 0;
		int tokenCol = 0;
		String text = "";

		for (int i = 0; i < input.length(); i++) {
			char curr = input.charAt(i);

			switch (state) {

			case START -> { // where the state machine starts
				tokenLine = line;
				tokenCol = col;
				text = "";
				if ((curr >= 'a' && curr <= 'z') || (curr >= 'A' && curr <= 'Z') || curr == '_' || curr == '$') {
					state = State.IN_IDENT;
					text += curr;
					col++;
				} else if (curr >= '1' && curr <= '9') {
					state = State.DIGIT;
					text += curr;
					col++;
				} else if (curr == '0') {
					state = State.START_ZERO;
					text += curr;
					col++;
				} else if (curr == ' ') {
					col++;
				} else if (curr == '>') { // handles gt and gteq
					col++;
					text += curr;
					state = State.IS_GT;
				} else if (curr == '<') { // handles lt and lteq
					col++;
					text += curr;
					state = State.IS_LT;
				} else if (curr == '=') { // handles assign and equals
					col++;
					text += curr;
					state = State.IS_EQ;
				} else if (curr == '-') {
					col++;
					text += curr;
					state = State.IS_DASH;
				} else if (curr == '!') {
					col++;
					text += curr;
					state = State.IS_EP;
				} else if (curr == '\n') {
					line++;
					col = 0;
				} else if (curr == '\"') {
					state = State.IN_STRING;
					text += curr;
					col++;
				} else {
					tokens.add(new Token(Kind.ERROR, "Invalid token", tokenLine, tokenCol));
					i = input.length(); // breaks the loops=
					// throw new LexicalException("Invalid token");
				}

			}

			case IN_IDENT -> { // handles both reserved and identifier tokens

				if ((curr >= 'a' && curr <= 'z') || (curr >= 'A' && curr <= 'Z') || curr == '_' || curr == '$'
						|| (curr >= '0' && curr <= '9')) {
					text += curr;
					col++;
				} else {
					tokens.add(new Token(checkReserved(text), text, tokenLine, tokenCol));
					state = State.START;
					i--;
				}
				if (i == input.length() - 1) {
					tokens.add(new Token(checkReserved(text), text, tokenLine, tokenCol));
				}

			}

			case DIGIT -> {

				if (curr >= '0' && curr <= '9') {
					text += curr;
					col++;
				} else if (curr == '.') {
					state = State.HAVE_DOT;
					text += curr;
					col++;
				} else {
					tokens.add(new Token(Kind.INT_LIT, text, tokenLine, tokenCol));
					state = State.START;
					i--;
				}
				if (i == input.length() - 1) {
					tokens.add(new Token(Kind.INT_LIT, text, tokenLine, tokenCol));
				}

			}

			case START_ZERO -> {

				if (curr == '.') {
					state = State.HAVE_DOT;
					text += curr;
					col++;
				} else {
					tokens.add(new Token(Kind.INT_LIT, text, tokenLine, tokenCol));
					state = State.START;
					i--;
				}
				if (i == input.length() - 1) {
					tokens.add(new Token(Kind.INT_LIT, text, tokenLine, tokenCol));
				}
			}

			case HAVE_DOT -> {

				if (curr >= '0' && curr <= '9') {
					text += curr;
					col++;
				} else {
					tokens.add(new Token(Kind.FLOAT_LIT, text, tokenLine, tokenCol));
					state = State.START;
					i--;
				}
				if (i == input.length() - 1) {
					tokens.add(new Token(Kind.FLOAT_LIT, text, tokenLine, tokenCol));
				}
			}

			case IN_STRING -> {
				if (curr == '\n') {
					text += curr;
					col = 0;
					line++;
				} else if (curr != '\\' && curr != '\"') {
					text += curr;
					col++;
				} else if (curr == '\"') {
					text += curr;
					col++;
					tokens.add(new Token(Kind.STRING_LIT, text, tokenLine, tokenCol));
					state = State.START;
				} else if (curr == '\\') {
					text += curr;
					col++;
					state = State.IN_STRING_ESC;
				}
			}

			case IN_STRING_ESC -> {
				if (curr == 'b' || curr == 't' || curr == 'n' || curr == 'f' || curr == 'r' || curr == '\"'
						|| curr == '\'' || curr == '\\') {
					text += curr;
					state = State.IN_STRING;
				} else {
					tokens.add(new Token(Kind.ERROR, "Invalid string token", tokenLine, tokenCol));
					i = input.length();
				}
			}

			case IS_GT -> {

				if (curr == '=') {
					text += curr;
					tokens.add(new Token(Kind.GE, text, tokenLine, tokenCol));
					state = State.START;
					col++;
				} else if (curr == '>') {
					text += curr;
					tokens.add(new Token(Kind.RANGLE, text, tokenLine, tokenCol));
					state = State.START;
					col++;
				} else {
					tokens.add(new Token(Kind.GT, text, tokenLine, tokenCol));
					state = State.START;
					i--;
				}
				if (i == input.length() - 1) {
					tokens.add(new Token(Kind.GT, text, tokenLine, tokenCol));
				}
			}

			case IS_LT -> {
				if (curr == '=') {
					text += curr;
					tokens.add(new Token(Kind.LE, text, tokenLine, tokenCol));
					state = State.START;
					col++;
				} else if (curr == '-') {
					text += curr;
					tokens.add(new Token(Kind.LARROW, text, tokenLine, tokenCol));
					state = State.START;
					col++;
				} else if (curr == '<') {
					text += curr;
					tokens.add(new Token(Kind.LANGLE, text, tokenLine, tokenCol));
					state = State.START;
					col++;
				} else {
					tokens.add(new Token(Kind.LT, text, tokenLine, tokenCol));
					state = State.START;
					i--;
				}
				if (i == input.length() - 1) {
					tokens.add(new Token(Kind.LT, text, tokenLine, tokenCol));
				}
			}

			case IS_EQ -> {
				if (curr == '=') {
					text += curr;
					tokens.add(new Token(Kind.EQUALS, text, tokenLine, tokenCol));
					state = State.START;
					col++;
				} else {
					tokens.add(new Token(Kind.ASSIGN, text, tokenLine, tokenCol));
					state = State.START;
					i--;
				}
				if (i == input.length() - 1) {
					tokens.add(new Token(Kind.ASSIGN, text, tokenLine, tokenCol));
				}
			}

			case IS_DASH -> {
				if (curr == '>') {
					text += curr;
					tokens.add(new Token(Kind.RARROW, text, tokenLine, tokenCol));
					state = State.START;
					col++;
				} else {
					tokens.add(new Token(Kind.MINUS, text, tokenLine, tokenCol));
					state = State.START;
					i--;
				}
				if (i == input.length() - 1) {
					tokens.add(new Token(Kind.MINUS, text, tokenLine, tokenCol));
				}
			}
			
			case IS_EP -> {
				if (curr == '=') {
					text += curr;
					tokens.add(new Token(Kind.NOT_EQUALS, text, tokenLine, tokenCol));
					state = State.START;
					col++;
				} else {
					tokens.add(new Token(Kind.BANG, text, tokenLine, tokenCol));
					state = State.START;
					i--;
				}
				if (i == input.length() - 1) {
					tokens.add(new Token(Kind.BANG, text, tokenLine, tokenCol));
				}
			}
			
			}
		}
	}

	@Override
	public IToken next() throws LexicalException {
		if (position >= tokens.size())
			throw new LexicalException("Reached end of tokens.");
		IToken curr = tokens.get(position);
		position++;
		return curr;
	}

	@Override
	public IToken peek() throws LexicalException {
		if (position >= tokens.size())
			throw new LexicalException("Reached end of tokens.");
		return tokens.get(position);
	}

	private IToken.Kind checkReserved(String input) {
		if (type.contains(input)) {
			return Kind.TYPE;
		} else if (image_op.contains(input)) {
			return Kind.IMAGE_OP;
		} else if (color_op.contains(input)) {
			return Kind.COLOR_OP;
		} else if (color_const.contains(input)) {
			return Kind.COLOR_CONST;
		} else if (boolean_lit.contains(input)) {
			return Kind.BOOLEAN_LIT;
		} else if (input.equals("if")) {
			return Kind.KW_IF;
		} else if (input.equals("fi")) {
			return Kind.KW_FI;
		} else if (input.equals("else")) {
			return Kind.KW_ELSE;
		} else if (input.equals("write")) {
			return Kind.KW_WRITE;
		} else if (input.equals("console")) {
			return Kind.KW_CONSOLE;
		}
		return Kind.IDENT;
	}

}
