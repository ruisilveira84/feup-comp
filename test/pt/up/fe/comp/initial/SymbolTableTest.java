package pt.up.fe.comp.initial;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.specs.util.SpecsIo;

import static org.junit.Assert.assertEquals;

/**
 * Test variable lookup.
 */
public class SymbolTableTest {

    static JmmSemanticsResult getSemanticsResult(String filename) {
        return TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/initial/symboltable/"+filename));
    }
    
    static JmmSemanticsResult test(String filename, boolean fail) {
    	var semantics = getSemanticsResult(filename);
    	if(fail) {
    		TestUtils.mustFail(semantics.getReports());
    	}else 	{
       	 	TestUtils.noErrors(semantics.getReports());
    	}
    	return semantics;
    }
    

    
    @Test
    public void Class() {
    	var semantics = test("Class.jmm",false);
    	assertEquals("Class", semantics.getSymbolTable().getClassName());
    }
    

    
    @Test
    public void Methods() {		
    	var semantics = test("Methods.jmm",false);
    	var st = semantics.getSymbolTable();
    	var methods = st.getMethods();
    	assertEquals(1, methods.size());

		var method = methods.get(0);
		var ret = st.getReturnType(method);
		assertEquals("Method with return type int", "int", ret.getName());

		var numParameters = st.getParameters(method).size();
		assertEquals("Method "+method+" parameters",1,numParameters);
    }

}
