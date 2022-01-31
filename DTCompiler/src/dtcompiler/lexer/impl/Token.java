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
		return kind;
	}

	@Override
	public String getText() {
		return text;
	}

	@Override
	public SourceLocation getSourceLocation() {
		return location;
	}

	@Override
	public int getIntValue() {
		if(kind == Kind.INT_LIT)
			return Integer.parseInt(text);
		return 0;
	}

	@Override
	public float getFloatValue() {
		if(kind == Kind.FLOAT_LIT)
			return Float.parseFloat(text);
		return 0f;
	}

	@Override
	public boolean getBooleanValue() {
		if(kind == Kind.BOOLEAN_LIT)
			return Boolean.parseBoolean(text);
		return false;
	}

	@Override
	public String getStringValue() {
		if(kind == Kind.STRING_LIT)
			return text;
		return null;
	}

}
