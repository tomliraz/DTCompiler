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
	final HashSet<String> type = new HashSet<>(Arrays.asList("string", "int", "float", "boolean",
			"color", "image", "void"));
	final HashSet<String> image_op = new HashSet<>(Arrays.asList("getWidth", "getHeight"));
	final HashSet<String> color_op = new HashSet<>(Arrays.asList("getRed", "getGreen", "getBlue"));
	final HashSet<String> color_const = new HashSet<>(Arrays.asList("BLACK", "BLUE", "CYAN", "DARK_GRAY",
			"GRAY", "GREEN", "LIGHT_GRAY", "MAGENTA", "ORANGE", "PINK", "RED", "WHITE", "YELLOW"));
	final HashSet<String> boolean_lit = new HashSet<>(Arrays.asList("true", "false"));
	final HashSet<String> other_keywords = new HashSet<>(Arrays.asList("if", "else", "fi", "write", "console"));

	public static enum State {
		START, IN_IDENT, IS_ERROR
	}

	public Lexer(String input) {
		tokens = new ArrayList<Token>();

		State state = State.START;
		int line = 0;
		int col = 0;
		String text = "";

		for (int i = 0; i < input.length(); i++) {
			char curr = input.charAt(i);

			switch (state) {

			case START -> {
				if ((curr >= 'a' && curr <= 'z') || (curr >= 'A' && curr <= 'Z') || curr == '_' || curr == '$') {
					state = State.IN_IDENT;
					text += curr;
					line++;
				}
			}
			case IN_IDENT -> {
				if ((curr >= 'a' && curr <= 'z') || (curr >= 'A' && curr <= 'Z') || curr == '_' || curr == '$'
						|| (curr >= '0' && curr <= '9')) {
					text += curr;
					line++;
				} else {
					tokens.add(new Token(Kind.IDENT, text, line, col));
					i--;
				}
			}
			case HAVE_ZERO -> {
			}
			case HAVE_DOT -> {
			}
			}
		}
	}

	@Override
	public IToken next() throws LexicalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IToken peek() throws LexicalException {
		// TODO Auto-generated method stub
		return null;
	}

	private IToken.Kind checkReserved() {

	}

}
