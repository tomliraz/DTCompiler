package dtcompiler.lexer;

import dtcompiler.lexer.impl.Lexer;
import edu.ufl.cise.plc.LexicalException;

public class Main {

	public static void main(String[] args) throws LexicalException {
		// TODO Auto-generated method stub
		
		
		Lexer textLexer = new Lexer("abc*asd*afdg*uiy");
		
		for(int i = 0; i< 5; i++)
		System.out.println(textLexer.next().getText());
		
	}

}
