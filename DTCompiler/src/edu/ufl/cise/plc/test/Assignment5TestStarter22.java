package edu.ufl.cise.plc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import edu.ufl.cise.plc.CodeGenVisitor;
import edu.ufl.cise.plc.CompilerComponentFactory;
import edu.ufl.cise.plc.IParser;
//import edu.ufl.cise.plc.ImageResources;
import edu.ufl.cise.plc.TypeCheckException;
import edu.ufl.cise.plc.TypeCheckVisitor;
import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.ast.ASTVisitor;
import edu.ufl.cise.plc.ast.Program;
//import edu.ufl.cise.plc.runtime.ColorTuple;
import edu.ufl.cise.plc.runtime.ConsoleIO;
import edu.ufl.cise.plc.runtime.javaCompilerClassLoader.DynamicClassLoader;
import edu.ufl.cise.plc.runtime.javaCompilerClassLoader.DynamicCompiler;

class Assignment5TestStarter22 {

	// Default package name for generated code
	String packageName = "cop4020sp22Package";

	boolean VERBOSE = true;

	private void show(Object obj) {
		if (VERBOSE)
			System.out.println(obj);
	}

	static final float DELTA = .001f;

	String getName(ASTNode ast) throws Exception {
		if (ast instanceof Program) {
			return ((Program) ast).getName();
		} else
			throw new Exception("bug--expected ast was program");
	}

	String getReturnType(ASTNode ast) throws Exception {
		if (ast instanceof Program) {
			return ((Program) ast).getReturnType().toString();
		} else
			throw new Exception("bug--expected ast was program");
	}

	void checkResult(String input, Object expectedResult) throws Exception {
		checkResult(input, null, expectedResult);
	}

	void checkResult(String input, Object[] params, Object expectedResult) throws Exception {
		Object result = exec(input, params);
		show("result = " + result);
		assertEquals(expectedResult, result);
	}

	Object exec(String input, Object[] params) throws Exception {
		// Lex and parse, to get AST
		ASTNode ast = CompilerComponentFactory.getParser(input).parse();
		// Type check and decorate AST with declaration and type info
		ast.visit(CompilerComponentFactory.getTypeChecker(), null);
		// Generate Java code
		String className = ((Program) ast).getName();
		String fullyQualifiedName = packageName != "" ? packageName + '.' + className : className;
		String javaCode = (String) ast.visit(CompilerComponentFactory.getCodeGenerator(packageName), null);
		show(javaCode);
		// Invoke Java compiler to obtain bytecode
		byte[] byteCode = DynamicCompiler.compile(fullyQualifiedName, javaCode);
		// Load generated classfile and execute its apply method.
		Object result = DynamicClassLoader.loadClassAndRunMethod(byteCode, fullyQualifiedName, "apply", params);
		return result;
	}

	private void displayResult(String input, Object[] params) throws Exception {
		Object result = exec(input, params);
		show(result);
	}

	// need separate method to check floats due to DELTA. Note in simple cases, just
	// use normal check.
	void checkFloatResult(String input, float expectedResult) throws Exception {
		checkFloatResult(input, null, expectedResult);
	}

	void checkFloatResult(String input, Object[] params, float expectedResult) throws Exception {
		Object result = exec(input, params);
		show("result = " + result);
		assertEquals(expectedResult, (float) result, DELTA);
	}

	/* The first group of test return literal values of various types */

	@Test
	void exampleTest() throws Exception {
		String input = "int y() ^42;";
		checkResult(input, null, 42);
	}

	@Test
	void unaryExp1() throws Exception {
		String input = """
				int unaryExp1(int a, int b)
				int x = a + -b;
				^ x;
				""";
		int a = 33;
		int b = 24;
		Object[] params = { a, b };
		checkResult(input, params, a - b);
	}

	@Test
	void unaryExp2() throws Exception {
		String input = """
				int unaryExp2(int a, int b, int c)
				int x = a - -b + -c;
				^ x;
				""";
		int a = 33;
		int b = 24;
		int c = 2;
		Object[] params = { a, b, c };
		checkResult(input, params, a + b - c);
	}

	@Test
	void unaryExp3() throws Exception {
		String input = """
				int unaryExp3()
				int a = 33;
				int b = 24;
				int c = 2;
				int x = a - -b + -c;
				^ x;
				""";
		checkResult(input, null, 55);
	}

	@Test
	void testConditional1() throws Exception {
		String input = """
				int testConditional1(int a, int b)
				int x = a+b;
				^ if (x > 0) a else b fi;
				""";
		int a = 33;
		int b = 24;
		Object[] params = { a, b };
		checkResult(input, params, a);
	}

	@Test
	void testConditional2() throws Exception {
		String input = """
				boolean testConditional1()
				boolean x = false;
				^ if (!x) true else false fi;
				""";
		checkResult(input, null, true);
	}

	@Test
	void testFloat() throws Exception {
		String input = "float y() ^42.0;";
		checkResult(input, null, 42f);
	}

//The following two tests are not valid in the grammar given 
	/*//these are wrong, no returns allowed in if statements
	@Test
	void testReturnInCondition1() throws Exception {
		String input = """
				int f()
				   int a = 3;
				   int b = 4;
				   if (a == 3)
				      ^ b;
				   else
				      ^ a;
				   fi;
				""";
		checkResult(input, 4);
	}

	@Test
	void testReturnInCondition2() throws Exception {
		String input = """
				int f()
				   int a = 3;
				   int b = 4;
				   if (a == 4)
				      ^ b;
				   else
				      ^ a;
				   fi;
				""";
		checkResult(input, 3);
	}
	
	*/

	@Test
	void testConditional3() throws Exception {
		String input = """
				int f()
				   int a = 3;
				   int b = 4;
				  int c =
				  if (a == 4)
				       2
				    else
				      5
				   fi;
				^c;
				""";
		checkResult(input, 5);
	}

	@Test
	void testReturnInCondition4() throws Exception {
		String input = """
				float f()
				   int a = 3;
				   int b = (8 * 3) + a;
				   float c =
				   if (b == 27)
				      2.5
				   else
				      6.5
				   fi;

				  ^c;
				""";
		checkResult(input, 2.5f);
	}

	@Test
	void testCast() throws Exception {// a and d should be cast to a float type
		String input = """
				float f()
				   int a = 3;
				   float b = 4.2;
				   int d = 2;
				  float c =  (a + b) + d;

				^c;
				""";
		checkResult(input, 9.2f);
	}

	@Test
	void testp1() throws Exception {
		String input = """
				int a()
				int c = (5.1+5);
				^ c;
				""";
		checkResult(input, null, 10);
	}

	@Test
	void testp2() throws Exception {
		String input = """
				int a()
				int c = if (5>6) 5.1 else 4.1 fi;
				^ c;
				""";
		checkResult(input, null, 4);
	}

	@Test
	void testp3() throws Exception {
		String input = """
				int a()
				int c = -5.1;
				^ c;
				""";
		checkResult(input, null, -5);
	}

	@Test
	void testp4() throws Exception {
		String input = """
				float a()
				float c = 5+(if (5<6) 5.1 else 4.1 fi);
				^ c;
				""";
		checkResult(input, null, 10.1f);
	}

	@Test
	void testp5() throws Exception {
		String input = """
				int a()
				int c = (if (5<6) 5 else 4 fi)*6;
				^ c;
				""";
		checkResult(input, null, 30);
	}

	@Test
	void testCoerce() throws Exception {
		String input = """
				int a()
				boolean x = false;
				int y;
				float c = 5.2;
				float b = 3.7;
				int d;
				d = c + b;
				float z = if (!x) d else 5 fi;
				y = z;
				^ y;
				""";
		checkResult(input, null, 8);
	}

}
