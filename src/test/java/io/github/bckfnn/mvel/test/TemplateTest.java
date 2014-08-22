package io.github.bckfnn.mvel.test;

import io.github.bckfnn.mvel.Template;
import io.github.bckfnn.mvel.TemplateCompiler;
import io.github.bckfnn.mvel.TemplateContext;
import io.github.bckfnn.mvel.template.Cback;
import io.github.bckfnn.mvel.template.io.ClassPathTemplateLoader;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;
import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.templates.TemplateRuntime;


public class TemplateTest extends TestCase {
    private Map<String, Object> map = new HashMap<String, Object>();
    private Foo foo = new Foo();
    private Base base = new Base();

    public TemplateTest() {
        map.put("_foo_", "Foo");
        map.put("_bar_", "Bar");

        ArrayList<String> list = new ArrayList<String>(3);
        list.add("Jane");
        list.add("John");
        list.add("Foo");

        map.put("arrayList", list);

        foo.setBar(new Bar());
        map.put("foo", foo);
        map.put("a", null);
        map.put("b", null);
        map.put("c", "cat");
        map.put("BWAH", "");

        map.put("pi", "3.14");
        map.put("hour", "60");
        map.put("zero", 0);

        //noinspection UnnecessaryBoxing
        map.put("doubleTen", new Double(10));

        map.put("variable_with_underscore", "HELLO");

        map.put("testImpl",
                new TestInterface() {

            public String getName() {
                return "FOOBAR!";
            }


            public boolean isFoo() {
                return true;
            }
        });
    }

    public Object test(String template) {
        return test(template, base, map);
    }

    public Object test(String template, Object context, Map<String, Object> model) {
        TemplateCompiler compiler = new TemplateCompiler(new ClassPathTemplateLoader("", ""));
        Template t = compiler.compile(template.toCharArray());

        VariableResolverFactory vrf = new MapVariableResolverFactory(model);
        io.github.bckfnn.mvel.TemplateRuntime rt = new io.github.bckfnn.mvel.TemplateRuntime(t, context, vrf);
        rt.exec();
        return rt.getOutput();
        
    }
    @Test
    public void testPassThru() {
        String s = "foobar!";
        assertEquals("foobar!", test(s));
    }

    @Test
    public void testBasicParsing() {
        String s = "foo: @{_foo_}--@{_bar_}!";
        assertEquals("foo: Foo--Bar!", test(s));
    }

    @Test
    public void testIfStatement() {
        String s = "@if(_foo_=='Foo')Hello@end";
        assertEquals("Hello", test(s));
    }

    @Test
    public void testIfStatement2() {
        String s = "@if(_foo_=='Bar')Hello@else(_foo_=='Foo')Goodbye@end";
        assertEquals("Goodbye", test(s));
    }

    @Test
    public void testIfStatement3() {
        String s = "@if(_foo_=='Bar')Hello@else(_foo_=='foo')Goodbye@else()Nope@end";
        assertEquals("Nope", test(s));
    }

    @Test
    public void testIfStatement4() {
        String s = "@if(_foo_=='Foo')Hello@else(_foo_=='foo')Goodbye@else()Nope@end()End";
        assertEquals("HelloEnd", test(s));
    }

    @Test
    public void testIfStatement5() {
        String s = "@if(_foo_=='foo')Hello@end()Goodbye";
        assertEquals("Goodbye", test(s));
    }

    @Test
    public void testIfNesting() {
        String s = "@if(_foo_=='Foo')Hello@if(_bar_=='Bar')Bar@end()@else(_foo_=='foo')Goodbye@else()Nope@end()";
        assertEquals("HelloBar", test(s));
    }

    @Test
    public void testForEach() {
        String s = "List:@foreach(item = arrayList)@{item}@end";
        assertEquals("List:JaneJohnFoo", test(s));
    }

    @Test
    public void testForEachMulti() {
        // TODO
        //String s = "Multi:@foreach(item = arrayList, item2 = arrayList)@{item}-@{item2}@end(','):Multi";
        //assertEquals("Multi:Jane-Jane,John-John,Foo-Foo:Multi", test(s));
    }

    @Test
    public void testComplexTemplate() {
        String s = "@foreach(item = arrayList)@if(item[0] == 'J')@{item}@end()@end()";
        assertEquals("JaneJohn", test(s));
    }

    /*
	@Test
	public void testFileBasedEval() {
		assertEquals("Foo::Bar", TemplateRuntime.eval(new File("src/test/java/org/mvel2/tests/templates/templateTest.mv"),
				base, new MapVariableResolverFactory(map), null));
	}
     */

    @Test
    public void testInclusionOfTemplateFile() {
        String s = "<<@include('templateTest.mv')>>";
        assertEquals("<<Foo::Bar>>", test(s));
    }

    @Test
    public void testInclusionOfTemplateFile2() {
        String s = "<<@include('templateError.mv')>>";
        try {
            test(s);
        } catch (CompileException e) {
            //System.out.println(e.toString());
            return;
        }
        assertTrue(false);
    }

    @Test
    public void testForEachException1() {
        String s = "<<@foreach(arrayList)@{item}@end()>>";
        try {
            test(s);
        }
        catch (Exception e) {
            //System.out.println(e.getMessage());
            return;
        }
        assertTrue(false);
    }

    @Test
    public void testForEachException2() {
        String s = "<<@foreach(item:arrayList)@{item}>>";
        try {
            test(s);
        }
        catch (Exception e) {
            //System.out.println(e.toString());
            return;
        }
        assertTrue(false);
    }

    /*
	@Test
	public void testTemplateFile() {
		String s = (String) TemplateRuntime.eval(new File("src/test/java/org/mvel2/tests/templates/templateIfTest.mv"),
				base, new MapVariableResolverFactory(map), null);

		System.out.println(s);
	}
     */

    /* TODO port to mvel-async
	@Test
	public void testInclusionOfNamedTemplate() {
		SimpleTemplateRegistry registry = new SimpleTemplateRegistry();
		registry.addNamedTemplate("footemplate", compileTemplate("@{_foo_}@{_bar_}"));
		registry.addNamedTemplate("bartemplate", compileTemplate("@{_bar_}@{_foo_}"));

		String s = "@includeNamed('footemplate') :: @includeNamed('bartemplate')";
		assertEquals("FooBar :: BarFoo", TemplateRuntime.eval(s, map, registry));
	}
     */

    @Test
    public void testExpressions() {
        String s = "@(_foo_.length())";
        Object r = test(s);
        assertEquals("3", r);
    }

    @Test
    public void testCode() {
        String s = "@code(a = 'foo'; b = 'bar')@a@b";
        assertEquals("foobar", test(s));
    }

    @Test
    public void testInlineDeclarations() {
        String s = "@declare('fudge')Hello @{name}!@end@includeNamed($='fudge'; name='John') -- @includeNamed($='fudge'; name='Mary')";
        assertEquals("Hello John! -- Hello Mary!", test(s));
    }

    @Test
    public void testInlineDeclarations2() {
        String s = "@declare('fudge')Hello @name!@end@code(toInclude='fudge')@includeNamed($=toInclude; name='John') -- @includeNamed($=toInclude; name='Mary')";
        assertEquals("Hello John! -- Hello Mary!", test(s));
    }

    @Test
    public void testPluginNode() {
        /* TODO port over to mvel-async
		Map<String, Class<? extends dk.innovasion.mvel.template.Node>> plugins = new HashMap<String, Class<? extends dk.innovasion.mvel.template.Node>>();
		plugins.put("testNode", TestPluginNode.class);

		TemplateCompiler compiler = new TemplateCompiler("Foo:@testNode()!!");
		CompiledTemplate compiled = compiler.compile();

		assertEquals("Foo:THIS_IS_A_TEST!!", TemplateRuntime.execute(compiled));
         */
    }


    @Test
    public void testComments() {
        assertEquals("Foo", test("@comment( This section is commented )@{_foo_}"));
    }

    /**
     * Integration of old tests
     */

    @Test
    public void testPassThru2() {
        assertEquals("foo@bar.com", TemplateRuntime.eval("foo@bar.com", map));
    }

    @Test
    public void testMethodOnValue() {
        assertEquals("DOG", test("@(foo.bar.name.toUpperCase())"));
    }

    @Test
    public void testSimpleProperty() {
        assertEquals("dog", test("@foo.bar.name"));
    }

    @Test
    public void testBooleanOperator() {
        assertEquals("true", test("@(foo.bar.woof == true)"));
    }

    @Test
    public void testBooleanOperator2() {
        assertEquals("false", test("@(foo.bar.woof == false)"));
    }

    @Test
    public void testTextComparison() {
        assertEquals("true", test("@(foo.bar.name == 'dog')"));
    }

    @Test
    public void testNETextComparison() {
        assertEquals("true", test("@(foo.bar.name != 'foo')"));
    }

    @Test
    public void testChor() {
        assertEquals("cat", test("@(a or b or c)"));
    }

    @Test
    public void testChorWithLiteral() {
        assertEquals("fubar", test("@(a or 'fubar')"));
    }

    @Test
    public void testNullCompare() {
        assertEquals("true", test("@(c != null)"));
    }

    @Test
    public void testAnd() {
        assertEquals("true", test("@(c != null && foo.bar.name == 'dog' && foo.bar.woof)"));
    }

    @Test
    public void testMath() {
        assertEquals("188.4", test("@(pi * hour)"));
    }

    @Test
    public void testTemplating() {
        assertEquals("dogDOGGIE133.5", test("@(foo.bar.name)DOGGIE@(hour*2.225+1-1)"));
    }

    @Test
    public void testComplexAnd() {
        assertEquals("true", test("@((pi * hour) > 0 && foo.happy() == 'happyBar')"));
    }

    @Test
    public void testModulus() {
        assertEquals("0", test("@(38392 % 2)"));
    }

    @Test
    public void testLessThan() {
        assertEquals("true", test("@{pi < 3.15}"));
        assertEquals("true", test("@{pi <= 3.14}"));
        assertEquals("false", test("@{pi > 3.14}"));
        assertEquals("true", test("@{pi >= 3.14}"));
    }

    @Test
    public void testMethodAccess() {
        assertEquals("happyBar", test("@{foo.happy()}"));
    }

    @Test
    public void testMethodAccess2() {
        assertEquals("FUBAR", test("@{foo.toUC('fubar')}"));
    }

    @Test
    public void testMethodAccess3() {
        assertEquals("true", test("@{equalityCheck(c, 'cat')}"));
    }

    @Test
    public void testMethodAccess4() {
        assertEquals("null", test("@{readBack(null)}"));
    }

    @Test
    public void testMethodAccess5() {
        assertEquals("nulltest", test("@{appendTwoStrings(null, 'test')}"));
    }

    @Test
    public void testMethodAccess6() {
        assertEquals("false", test("@{!foo.bar.isWoof()}"));
    }

    @Test
    public void testNegation() {
        assertEquals("true", test("@{!fun && !fun}"));
    }

    @Test
    public void testNegation2() {
        assertEquals("false", test("@{fun && !fun}"));
    }

    @Test
    public void testNegation3() {
        assertEquals("true", test("@{!(fun && fun)}"));
    }

    @Test
    public void testNegation4() {
        assertEquals("false", test("@{(fun && fun)}"));
    }

    @Test
    public void testMultiStatement() {
        assertEquals("true", test("@{populate(); barfoo == 'sarah'}"));
    }

    @Test
    public void testAssignment2() {
        assertEquals("sarah", test("@{populate(); blahfoo = barfoo}"));
    }

    @Test
    public void testOr() {
        assertEquals("true", test("@{fun || true}"));
    }

    @Test
    public void testLiteralPassThrough() {
        assertEquals("true", test("@{true}"));
    }

    @Test
    public void testLiteralPassThrough2() {
        assertEquals("false", test("@{false}"));
    }

    @Test
    public void testLiteralPassThrough3() {
        assertEquals("null", test("@{null}"));
    }

    @Test
    public void testControlLoopList() {
        assertEquals("HappyHappy!JoyJoy!",
                test(
                        "@foreach(item = list)" +
                                "@{item}" +
                                "@end"
                        ));
    }

    @Test
    public void testControlLoopArray() {
        assertEquals("Happy0Happy!1Joy2Joy!3",
                test(
                        "@code(i=0)@foreach(item = array)" +
                                "@{item}@{i++}" +
                                "@end"
                        ));
    }

    @Test
    public void testMultiCollectionControlLoop() {
        // TODO
        /*
		assertEquals("0=Happy:Happy,1=Happy!:Happy!,2=Joy:Joy,3=Joy!:Joy!",
				test(
						"@code{i=0}@foreach{item : list, listItem : array}" +
								"@{i++}=@{item}:@{listItem}" +
								"@end{','}"
						));
         */
    }

    @Test
    public void testControlLoopListMultiple() {
        for (int i = 0; i < 100; i++) {
            testControlLoopList();
        }
    }

    @Test
    public void testControlLoopArrayMultiple() {
        for (int i = 0; i < 100; i++) {
            testControlLoopArray();
        }
    }

    public static interface TestInterface {
        public String getName();

        public boolean isFoo();
    }

    @Test
    public void testControlLoop2() {
        assertEquals("HappyHappy!JoyJoy!",
                test(
                        "@foreach(item = list)" +
                                "@item" +
                                "@end"
                        ));
    }

    @Test
    public void testControlLoop3() {
        assertEquals("HappyHappy!JoyJoy!",
                test(
                        "@foreach(item = list )" +
                                "@item" +
                                "@end"
                        ));
    }

    @Test
    public void testIfStatement6() {
        assertEquals("sarah", test("@if('fun' == 'fun')sarah@end"));
    }

    @Test
    public void testIfStatement7() {
        assertEquals("poo", test("@if('fun' == 'bar')sarah@else()poo@end"));
    }

    @Test
    public void testRegEx() {
        assertEquals("true", test("@{foo.bar.name ~= '[a-z].+'}"));
    }

    @Test
    public void testRegExNegate() {
        assertEquals("false", test("@{!(foo.bar.name ~= '[a-z].+')}"));
    }

    @Test
    public void testRegEx2() {
        assertEquals("true", test("@{foo.bar.name ~= '[a-z].+' && foo.bar.name != null}"));
    }

    @Test
    public void testBlank() {
        assertEquals("true", test("@{'' == empty}"));
    }

    @Test
    public void testBlank2() {
        assertEquals("true", test("@{BWAH == empty}"));
    }

    @Test
    public void testTernary() {
        assertEquals("foobie", test("@{zero==0?'foobie':zero}"));
    }

    @Test
    public void testTernary2() {
        assertEquals("blimpie", test("@{zero==1?'foobie':'blimpie'}"));
    }

    @Test
    public void testTernary3() {
        assertEquals("foobiebarbie", test("@{zero==1?'foobie':'foobie'+'barbie'}"));
    }

    @Test
    public void testTernary4() {
        assertEquals("no", test("@{ackbar ? 'yes' : 'no'}"));
    }

    @Test
    public void testStrAppend() {
        assertEquals("foobarcar", test("@{'foo' + 'bar' + 'car'}"));
    }

    @Test
    public void testStrAppend2() {
        assertEquals("foobarcar1", test("@{'foobar' + 'car' + 1}"));
    }

    @Test
    public void testInstanceCheck1() {
        assertEquals("true", test("@{c is java.lang.String}"));
    }

    @Test
    public void testInstanceCheck2() {
        assertEquals("false", test("@{pi is java.lang.Integer}"));
    }

    @Test
    public void testBitwiseOr1() {
        assertEquals("6", test("@{2 | 4}"));
    }

    @Test
    public void testBitwiseOr2() {
        assertEquals("true", test("@{(2 | 1) > 0}"));
    }

    @Test
    public void testBitwiseOr3() {
        assertEquals("true", test("@{(2 | 1) == 3}"));
    }

    @Test
    public void testBitwiseAnd1() {
        assertEquals("2", test("@{2 & 3}"));
    }

    @Test
    public void testShiftLeft() {
        assertEquals("4", test("@{2 << 1}"));
    }

    @Test
    public void testUnsignedShiftLeft() {
        assertEquals("2", test("@{-2 <<< 0}"));
    }

    @Test
    public void testShiftRight() {
        assertEquals("128", test("@{256 >> 1}"));
    }

    @Test
    public void testXOR() {
        assertEquals("3", test("@{1 ^ 2}"));
    }

    @Test
    public void testContains1() {
        assertEquals("true", test("@{list contains 'Happy!'}"));
    }

    @Test
    public void testContains2() {
        assertEquals("false", test("@{list contains 'Foobie'}"));
    }

    @Test
    public void testContains3() {
        assertEquals("true", test("@{sentence contains 'fox'}"));
    }

    @Test
    public void testContains4() {
        assertEquals("false", test("@{sentence contains 'mike'}"));
    }

    @Test
    public void testContains5() {
        assertEquals("true", test("@{!(sentence contains 'mike')}"));
    }

    @Test
    public void testTokenMethodAccess() {
        assertEquals(String.class.toString(), test("@{a = 'foo'; a.getClass()}"));
    }

    @Test
    public void testArrayCreationWithLength() {
        assertEquals("2", test("@{Array.getLength({'foo', 'bar'})}"));
    }

    @Test
    public void testMapCreation() {
        assertEquals("sarah", test("@{map = ['mike':'sarah','tom':'jacquelin']; map['mike']}"));
    }

    @Test
    public void testProjectionSupport() {
        assertEquals("true", test("@{(name in things) contains 'Bob'}"));
    }

    @Test
    public void testProjectionSupport2() {
        assertEquals("3", test("@{(name in things).size()}"));
    }

    @Test
    public void testObjectInstantiation() {
        assertEquals("foobie", test("@{new java.lang.String('foobie')}"));
    }

    @Test
    public void testObjectInstantiationWithMethodCall() {
        assertEquals("foobie", test("@{new String('foobie').toString()}"));
    }

    @Test
    public void testObjectInstantiation2() {
        test("@{new String() is String}");
    }

    @Test
    public void testArrayCoercion() {
        assertEquals("gonk", test("@{funMethod( {'gonk', 'foo'} )}"));
    }

    @Test
    public void testMapAccess() {
        assertEquals("dog", test("@{funMap['foo'].bar.name}"));
    }

    @Test
    public void testMapAccess2() {
        assertEquals("dog", test("@{funMap.foo.bar.name}"));
    }

    @Test
    public void testSoundex() {
        assertEquals("true", test("@{'foobar' soundslike 'fubar'}"));
    }

    @Test
    public void testSoundex2() {
        assertEquals("false", test("@{'flexbar' soundslike 'fubar'}"));
    }

    @Test
    public void testThisReference() {
        // TODO
        //assertEquals("true", test("@{this}") instanceof Base);
    }

    @Test
    public void testIfLoopInTemplate() {
        assertEquals("ONETWOTHREE", test("@foreach(item = things)@if(item.name=='Bob')ONE@else(item.name=='Smith')TWO@else(item.name=='Cow')THREE@end@end"));
    }

    @Test
    public void testStringEscaping() {
        assertEquals("\"Mike Brock\"", test("@{\"\\\"Mike Brock\\\"\"}"));
    }

    @Test
    public void testStringEscaping2() {
        assertEquals("MVEL's Parser is Fast", test("@{'MVEL\\'s Parser is Fast'}"));
    }

    @Test
    public void testNestedAtSymbol() {
        assertEquals("email:foo@foo.com", test("email:@{'foo@foo.com'}"));
    }

    @Test
    public void testEscape() {
        assertEquals("foo@foo.com", test("foo@@@{'foo.com'}"));
    }

    @Test
    public void testEvalNodes() {
        //TODO: support eval assertEquals("foo", test("@eval{\"@{'foo'}\"}"));
    }

    @Test
    public void testIteration1() {
        List<String> list = new ArrayList<String>();
        list.add("a1");
        list.add("a2");
        list.add("a3");

        String template = "@foreach{item : list}a@end{}";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("list", list);
        String r = (String) TemplateRuntime.eval(template, map);

        assertEquals("aaa", r);
    }

    @Test
    public void testIteration2() {
        Folder f1 = new Folder("f1", null);

        String template = "@{name} @foreach{item : children}a@end{}";
        String r = (String) TemplateRuntime.eval(template, f1);
        assertEquals("f1 aaa", r);
    }

    @Test
    public void testIteration3() {
        Folder f = new Folder("a1", null);
        List<Page> list = f.getChildren();

        String template = "@foreach{item : list}a@end{}";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("list", list);
        String r = (String) TemplateRuntime.eval(template, map);

        assertEquals("aaa", r);
    }

    @Test
    public void testIteration4() {
        Folder f = new Folder("a1", null);

        String template = "@foreach{item : f.children}a@end{}";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("f", f);
        String r = (String) TemplateRuntime.eval(template, map);

        assertEquals("aaa", r);
    }

    @Test
    public void testMVEL197() {
        Map<String, Object> context = new HashMap<String, Object>();
        Object[] args = new Object[1];
        TestMVEL197 test = new TestMVEL197();
        test.setName1("name1");
        test.setName2("name2");
        args[0] = test;
        context.put("args", args);
        String template = "${(args[0].name1=='name1'&&args[0].name2=='name2')?'a':'b'}";
        Object value = TemplateRuntime.eval(template, context);

        assertEquals("a", value);
    }

    @Test
    public void testEscaping() {
        String template = "@@{'foo'}ABC";
        assertEquals("@{'foo'}ABC", TemplateRuntime.eval(template, new Object()));
    }

    public class Page {
        String name;
        Folder parent;

        public Page(String name, Folder parent) {
            this.name = name;
            this.parent = parent;
        }

        public String getName() {
            return name;
        }

        public Folder getParent() {
            return parent;
        }
    }

    public class Folder extends Page {
        public Folder(String name, Folder parent) {
            super(name, parent);
        }

        public List<Page> getChildren() {
            List<Page> list = new ArrayList<Page>();
            list.add(new Page("a1", this));
            list.add(new Page("a2", this));
            list.add(new Page("a3", this));
            return list;
        }
    }

    @Test
    public void testMVEL229() {
        //final Object context = new Object();
        final String template = "@code(sumText = 0)@(sumText)";
        assertEquals("0", test(template));
    }

    @Test
    public void testOutputStream1() {
        // Note: mvel-async  does not support outputstream.
        String template = "@foreach(['foo','far'])@{$}@end";

        assertEquals("foofar", test(template, new Object(), new HashMap<String, Object>()));
    }

    private Map<String, Object> setupVarsMVEL219() {
        Map<String, Object> vars = new LinkedHashMap<String, Object>();
        vars.put("bal", new BigDecimal("999.99"));
        vars.put("word", "ball");
        vars.put("object", new Dog());
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("foo", "bar");
        map.put("fu", new Dog());
        map.put("trueValue", true);
        map.put("falseValue", false);
        map.put("one", 1);
        map.put("zero", 0);
        vars.put("map", map);

        return vars;
    }

    private Map<String, Object> setupVarsMVEL220() {
        Map<String, Object> vars = new LinkedHashMap<String, Object>();
        vars.put("word", "ball");
        vars.put("object", new Dog());
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("foo", "bar");
        map.put("fu", new Dog());
        map.put("trueValue", true);
        map.put("falseValue", false);
        map.put("one", 1);
        map.put("zero", 0);
        map.put("list", "john,paul,ringo,george");
        vars.put("map", map);

        return vars;
    }

    String[] testCasesMVEL220 = {
            "map[\"foundIt\"] = !(map['list']).contains(\"john\")",
            "map[\"foundIt\"] = !(map['list'].contains(\"john\"))",
    };
    String[] templateTestCasesMVEL220 = {
            "@{map[\"foundIt\"] = !(map['list']).contains(\"john\")}",
            "@{map[\"foundIt\"] = !(map['list'].contains(\"john\"))}"
    };

    @Test
    public void testEvalMVEL220() {
        Map<String, Object> vars = setupVarsMVEL220();

        //System.out.println("Evaluation=====================");

        for (String expr : testCasesMVEL220) {
            //System.out.println("Evaluating '" + expr + "': ......");
            Object ret = MVEL.eval(expr, vars);
            //System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }

        //System.out.println("Evaluation=====================");
    }

    @Test
    public void testCompiledMVEL220() {
        Map<String, Object> vars = setupVarsMVEL220();

        //System.out.println("Compilation=====================");

        for (String expr : testCasesMVEL220) {
            //System.out.println("Compiling '" + expr + "': ......");
            Serializable compiled = MVEL.compileExpression(expr);
            Boolean ret = (Boolean) MVEL.executeExpression(compiled, vars);
            //System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }
        //System.out.println("Compilation=====================");
    }

    @Test
    public void testTemplateMVEL220() {
        Map<String, Object> vars = setupVarsMVEL220();

        //System.out.println("Templates=====================");

        for (String expr : templateTestCasesMVEL220) {
            //System.out.println("Templating '" + expr + "': ......");
            Object ret = TemplateRuntime.eval(expr, vars);
            //System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }

        //System.out.println("Templates=====================");
    }

    String[] testCasesMVEL219 = {
            "map['foo']==map['foo']", // ok
            "(map['one'] > 0)", // ok
            "(map['one'] > 0) && (map['foo'] == map['foo'])", // ok
            "(map['one'] > 0) && (map['foo']==map['foo'])", // broken
    };
    String[] templateTestCasesMVEL219 = {
            "@{map['foo']==map['foo']}", // ok
            "@(map['one'] > 0)}", // ok
            "@{(map['one'] > 0) && (map['foo'] == map['foo'])}", // ok
            "@{(map['one'] > 0) && (map['foo']==map['foo'])}" // broken
    };

    @Test
    public void testEvalMVEL219() {
        Map<String, Object> vars = setupVarsMVEL219();

        for (String expr : testCasesMVEL219) {
            //System.out.println("Evaluating '" + expr + "': ......");
            Object ret = MVEL.eval(expr, vars);
            //System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }
    }

    @Test
    public void testCompiledMVEL219() {
        Map<String, Object> vars = setupVarsMVEL219();

        for (String expr : testCasesMVEL219) {
            //System.out.println("Compiling '" + expr + "': ......");
            Serializable compiled = MVEL.compileExpression(expr);
            Boolean ret = (Boolean) MVEL.executeExpression(compiled, vars);
            //System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }
    }

    @Test
    public void testTemplateMVEL219() {
        Map<String, Object> vars = setupVarsMVEL219();

        for (String expr : templateTestCasesMVEL219) {
            //System.out.println("Templating '" + expr + "': ......");
            Object ret = TemplateRuntime.eval(expr, vars);
            //System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }
    }

    @Test
    public void testTemplateStringCoercion() {
        String expr = "@code{ buffer = new StringBuilder(); i = 10; buffer.append( i + \"blah\" );}@{buffer.toString()}";
        Map<String, Object> vars = setupVarsMVEL219();
        //System.out.println("Templating '" + expr + "': ......");
        Object ret = TemplateRuntime.eval(expr, vars);
        //System.out.println("'" + expr + " ' = " + ret.toString());
        assertEquals("10blah", ret);
    }

    @Test
    public void testMVEL244() {
        Foo244 foo = new Foo244("plop");

        String template = "@foreach(foo.liste[0].liste) plop @end";

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("foo", foo);
        
        assertEquals(" plop  plop  plop  plop ", test(template, new Object(), model));
    }

    @Test
    public void testImportsInTemplate() {
        String template = "@code{import java.util.HashMap; i = 10;}_____________@code{new HashMap().toString() + i}";

        Map<String, Object> map = new HashMap<String, Object>();
        Object result = TemplateRuntime.eval(template, map);
        assertNotNull("result cannot be null", result);
        assertEquals("result did not return string", String.class, result.getClass());
    }



    public static class Foo244 {
        private List<Foo244> liste = new ArrayList<Foo244>();

        private String val = "";

        public Foo244() {
        }

        public Foo244(String plop) {
            liste.add(new Foo244());
            liste.add(new Foo244());
            liste.add(new Foo244());
            liste.add(new Foo244());

            liste.get(0).getListe().add(new Foo244());
            liste.get(0).getListe().add(new Foo244());
            liste.get(0).getListe().add(new Foo244());
            liste.get(0).getListe().add(new Foo244());
        }

        public List<Foo244> getListe() {
            return liste;
        }

        public void setListe(List<Foo244> liste) {
            this.liste = liste;
        }

        public String getVal() {
            return val;
        }

        public void setVal(String val) {
            this.val = val;
        }
    }

    public static class Node {
        public Node(int base, List<Node> list) {
            this.base = base;
            this.list = list;
        }

        public int base;
        public List<Node> list;
    }

    @Test
    public void testDRLTemplate() {
        /*
         * TODO

        String template = "@declare{\"drl\"}@includeNamed{\"ced\"; node=root }@end{}" +
                "" +
                "@declare{\"ced\"}" +
                "@if{ node.base==1 } @includeNamed{ \"cedX\"; connect=\"AND\"; args=node.list }" +
                "@elseif{ node.base ==2 }@includeNamed{ \"cedX\"; connect=\"OR\"; args=node.list }" +
                "@end{}" +
                "@end{}" +
                "" +
                "@declare{\"cedX\"}@{connect}@foreach{child : args}" +
                "@includeNamed{\"ced\"; node=child; }@end{} @{connect}@end{}";

        TemplateRegistry reportRegistry = new SimpleTemplateRegistry();

        reportRegistry.addNamedTemplate("drl", TemplateCompiler.compileTemplate(template));
        TemplateRuntime.execute(reportRegistry.getNamedTemplate("drl"), null, reportRegistry);

        Map<String, Object> context = new HashMap<String, Object>();
        context.put( "root", new Node( 2,
                Arrays.asList( new Node( 1,
                        Collections.EMPTY_LIST ) ) ) );


        String result = (String) TemplateRuntime.execute( reportRegistry.getNamedTemplate( "drl" ),
                null,
                new MapVariableResolverFactory( context ),
                reportRegistry );

        assertEquals("OR AND AND OR", result);
        */
    }

    public static class Pet {
        public void run() {
        }
    }

    public static class Dog extends Pet {
        @Override
        public void run() {
            System.out.println("dog is running");
        }
    }

    public class TestMVEL197 {

        private String name1;

        private String name2;

        public String getName1() {
            return name1;
        }

        public void setName1(String name1) {
            this.name1 = name1;
        }

        public String getName2() {
            return name2;
        }

        public void setName2(String name2) {
            this.name2 = name2;
        }
    }

    public static class TestPluginNode extends io.github.bckfnn.mvel.template.Node {

        public boolean eval(io.github.bckfnn.mvel.TemplateRuntime runtime, Object ctx, VariableResolverFactory factory, Cback callback) {
            //appender.append("THIS_IS_A_TEST");
            return callback.handle(getNext());
        }

        public boolean demarc(TemplateContext context, Node terminatingNode) {
            return false;
        }
    }
}
