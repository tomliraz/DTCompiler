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
	
	final HashSet<String> type = new HashSet<>(Arrays.asList("string", "int", "float", "boolean",
			"color", "image", "void"));
	final HashSet<String> image_op = new HashSet<>(Arrays.asList("getWidth", "getHeight"));
	final HashSet<String> color_op = new HashSet<>(Arrays.asList("getRed", "getGreen", "getBlue"));
	final HashSet<String> color_const = new HashSet<>(Arrays.asList("BLACK", "BLUE", "CYAN", "DARK_GRAY",
			"GRAY", "GREEN", "LIGHT_GRAY", "MAGENTA", "ORANGE", "PINK", "RED", "WHITE", "YELLOW"));
	final HashSet<String> boolean_lit = new HashSet<>(Arrays.asList("true", "false"));


	public static enum State {
		START, IN_IDENT, IS_ERROR
	}

	public Lexer(String input) {
		tokens = new ArrayList<Token>();
		position = 0;

		State state = State.START;
		int line = 0;
		int col = 0;
		String text = "";

		for (int i = 0; i < input.length(); i++) {
			char curr = input.charAt(i);

			switch (state) {

			case START -> {
				text = "";
				if ((curr >= 'a' && curr <= 'z') || (curr >= 'A' && curr <= 'Z') || curr == '_' || curr == '$') {
					state = State.IN_IDENT;
					text += curr;
					col++;
				}
			}
			case IN_IDENT -> {  //handles both reserved and identifier tokens
				if ((curr >= 'a' && curr <= 'z') || (curr >= 'A' && curr <= 'Z') || curr == '_' || curr == '$'
						|| (curr >= '0' && curr <= '9')) {
					text += curr;
					col++;
				} else {
					tokens.add(new Token(checkReserved(text), text, line, col));
					state = State.START;
				}
				if(i == input.length()-1) {
					tokens.add(new Token(checkReserved(text), text, line, col));
				}
			}
			/*
			case HAVE_ZERO -> {
			}
			case HAVE_DOT -> {
			}
			*/
			}
			
		}
	}

	@Override
	public IToken next() throws LexicalException {
		IToken curr = tokens.get(position);
		position++;
		return curr;
	}

	@Override
	public IToken peek() throws LexicalException {
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
		} else if (input == "if") {
			return Kind.KW_IF;
		} else if (input == "fi") {
			return Kind.KW_FI;
		} else if (input == "else") {
			return Kind.KW_ELSE;
		} else if (input == "write") {
			return Kind.KW_WRITE;
		} else if (input == "console") {
			return Kind.KW_CONSOLE;
		}
		return Kind.IDENT;
	}

}
