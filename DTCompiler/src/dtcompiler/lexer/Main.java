package dtcompiler.lexer;

import dtcompiler.lexer.impl.Lexer;
import edu.ufl.cise.plc.LexicalException;

public class Main {

	public static void main(String[] args) throws LexicalException {
		// TODO Auto-generated method stub

		Lexer textLexer = new Lexer("""
				int "yo
				
				y"
				== = <- >= < >
				yoyo 01.123""");

		for (int i = 0; i < 18; i++)
			System.out.println(textLexer.peek().getText() + "\t\ttype: " + textLexer.peek().getKind() + "\t\tloc: " + textLexer.next().getSourceLocation());

	}

}
