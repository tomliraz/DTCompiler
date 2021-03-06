package edu.ufl.cise.plc.lexer;

import edu.ufl.cise.plc.IToken;

public class Token implements IToken {

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
		if (kind == Kind.INT_LIT)
			return Integer.parseInt(text);
		return 0;
	}

	@Override
	public float getFloatValue() {
		if (kind == Kind.FLOAT_LIT)
			return Float.parseFloat(text);
		return 0f;
	}

	@Override
	public boolean getBooleanValue() {
		if (kind == Kind.BOOLEAN_LIT)
			return Boolean.parseBoolean(text);
		return false;
	}

	@Override
	public String getStringValue() {

		String output = "";
		if (kind == Kind.STRING_LIT) {

			for (int i = 0; i < text.length(); i++) {

				if (text.charAt(i) == '\\') {

					if (i != text.length() - 1) {

						if (text.charAt(i + 1) == 'b') {
							output += '\b';
						} else if (text.charAt(i + 1) == 't') {
							output += '\t';
						} else if (text.charAt(i + 1) == 'n') {
							output += '\n';
						} else if (text.charAt(i + 1) == 'f') {
							output += '\f';
						} else if (text.charAt(i + 1) == 'r') {
							output += '\r';
						} else {
							output += text.charAt(i + 1);
						}
						i++;

					}

				} else {
					output += text.charAt(i);
				}

			}
			output = output.substring(1, output.length() - 1);
			return output;
		}

		return null;
	}

}
