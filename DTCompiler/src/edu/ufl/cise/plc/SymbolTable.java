package edu.ufl.cise.plc;

import java.util.HashMap;

import edu.ufl.cise.plc.ast.Declaration;

public class SymbolTable {
	
	HashMap<String,Declaration> entries = new HashMap<>();

	public boolean insert(String name, Declaration declaration) {
		return (entries.putIfAbsent(name,declaration) == null);
	}
	
	public Declaration lookup(String name) {
		return entries.get(name);
	}

	public boolean remove(String name) {
		return (entries.remove(name) != null); //will return the old value if it exists (meaning true), returns null if it did not exist
	}
}
