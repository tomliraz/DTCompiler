package dtcompiler.lexer.impl;

import edu.ufl.cise.plc.IToken;

public class Token implements IToken{

	Kind kind;
	String text;
	
	SourceLocation location;
	
	public Token(Kind kind, String text, int line, int col) {
		this.kind = kind;
		this.text = text;
		location = new SourceLocation(line, col);
	}
	
	@Override
	public Kind getKind() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SourceLocation getSourceLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getIntValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getFloatValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean getBooleanValue() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getStringValue() {
		// TODO Auto-generated method stub
		return null;
	}

}
