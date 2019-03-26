/**
 * 
 */
package org.mycompany.connector;

import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorValidationException;

/**
 * This abstract class is generated and should not be modified.
 */
public abstract class AbstractConcatenationImpl extends AbstractConnector {

	protected final static String INPUT1_INPUT_PARAMETER = "Input1";
	protected final static String INPUT2_INPUT_PARAMETER = "Input2";
	protected final static String INPUT3_INPUT_PARAMETER = "Input3";
	protected final String OUTPUT1_OUTPUT_PARAMETER = "Output1";

	protected final java.lang.String getInput1() {
		return (java.lang.String) getInputParameter(INPUT1_INPUT_PARAMETER);
	}

	protected final java.lang.String getInput2() {
		return (java.lang.String) getInputParameter(INPUT2_INPUT_PARAMETER);
	}

	protected final java.lang.String getInput3() {
		return (java.lang.String) getInputParameter(INPUT3_INPUT_PARAMETER);
	}

	protected final void setOutput1(java.lang.String output1) {
		setOutputParameter(OUTPUT1_OUTPUT_PARAMETER, output1);
	}

	@Override
	public void validateInputParameters() throws ConnectorValidationException {
		try {
			getInput1();
		} catch (ClassCastException cce) {
			throw new ConnectorValidationException("Input1 type is invalid");
		}
		try {
			getInput2();
		} catch (ClassCastException cce) {
			throw new ConnectorValidationException("Input2 type is invalid");
		}
		try {
			getInput3();
		} catch (ClassCastException cce) {
			throw new ConnectorValidationException("Input3 type is invalid");
		}

	}

}
