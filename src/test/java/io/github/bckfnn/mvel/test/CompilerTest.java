package io.github.bckfnn.mvel.test;

import io.github.bckfnn.mvel.Template;
import io.github.bckfnn.mvel.TemplateCompiler;
import io.github.bckfnn.mvel.TemplateRuntime;
import io.github.bckfnn.mvel.template.Node;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mvel2.integration.impl.MapVariableResolverFactory;

public class CompilerTest {
	@Test
	public void testEmpty() throws Exception {
		compileTemplate("");
	}
	
	@Test
	public void testText() throws Exception {
		compileTemplate("abc", text("abc"));
	}
	
	@Test
	public void testAt1() throws Exception {
		compileTemplate("@@", text("@"));
	}

	@Test
	public void testAt2() throws Exception {
		compileTemplate("abc@@", text("abc@"));
	}

	@Test
	public void testAt3() throws Exception {
		compileTemplate("@@abc", text("@"), text("abc"));
	}

	@Test
	public void testAt4() throws Exception {
		compileTemplate("abc@@def", text("abc@"), text("def"));
	}

	@Test
	public void testExpr1() throws Exception {
		compileTemplate("@abc", expr("abc"));
	}

	@Test
	public void testExpr2() throws Exception {
		compileTemplate("@{abc}", expr("abc"));
	}

	@Test
	public void testExprText1() throws Exception {
		compileTemplate("abc@{abc}", text("abc"), expr("abc"));
	}
	
	@Test
	public void testExprText2() throws Exception {
		compileTemplate("@{abc}def", expr("abc"), text("def"));
	}
	
	@Test
	public void testForEach() throws Exception {
		compileTemplate("@foreach(items) @i @end END", 
				"ForEachNode[items]",  
				//text(" "), expr("i"), text(" "), 
				"EndNode[]", text(" END"));
	}
	
	@Test
	public void testForEach2() throws Exception {
		runTemplate("@foreach(items)@$@end()END", "abcEND", "items", Arrays.asList("a", "b", "c"));
	}

	@Test
	public void testIf1() throws Exception {
		runTemplate("@if(true)TRUE@end", "TRUE");
	}

	@Test
	public void testIf2() throws Exception {
		runTemplate("@if(v)TRUE@end", "TRUE", "v", true);
		runTemplate("@if(!v)TRUE@end", "TRUE", "v", false);
	}
	
	@Test
	public void testIf3() throws Exception {
		runTemplate("@if(v)TRUE@else()FALSE@end", "TRUE", "v", true);
		runTemplate("@if(v)TRUE@else()FALSE@end", "FALSE", "v", false);
	}
	
	@Test
	public void testCode1() throws Exception {
		runTemplate("@code(age=23; name='John Doe')@name is @age years old", "John Doe is 23 years old");
	}

	String text(String arg) {
		return "TextNode[" + arg + "]";
	}

	String expr(String arg) {
		return "ExprNode[" + arg + "]";
	}


	private void compileTemplate(String template, String... args) throws Exception {
	    TemplateCompiler compiler = new TemplateCompiler(null);
		Template t = compiler.compile(template.toCharArray());

		Assert.assertNotNull(t.getRoot());
		Node n = t.getRoot().getNext();
		for (String s : args) {
			//System.out.println(s + " " + n);
			if (s == null) {
				Assert.assertNull(s);
			} else {
				Assert.assertEquals(s, n.toString());
			}
			n = n.getNext();
		}
		Assert.assertNull(n);
	}
	
	private void runTemplate(String template, String result, Object... args) throws Exception {
	    TemplateCompiler compiler = new TemplateCompiler(null);
	    Template t = compiler.compile(template.toCharArray());

		Map<String, Object> vars = new HashMap<String, Object>();
		for (int i = 0; i < args.length; i += 2) {
			vars.put(args[i].toString(), args[i + 1]);
		}
		//System.out.println(vars);

		TemplateRuntime rt = new TemplateRuntime(t, vars, new MapVariableResolverFactory(vars));
		rt.exec();
		
		Assert.assertEquals(result, rt.getOutput());
	}
}
