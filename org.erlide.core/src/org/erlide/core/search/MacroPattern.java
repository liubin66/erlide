package org.erlide.core.search;

import com.ericsson.otp.erlang.OtpErlangObject;

public class MacroPattern extends NamePattern {

	public MacroPattern(final String name, final int limitTo) {
		super(name, limitTo);
	}

	@Override
	public OtpErlangObject getSearchObject() {
		final String name = getName();
		return makeSPatternObject(MACRO_DEF_ATOM, MACRO_REF_ATOM, name, "?"
				+ name);
	}

	@Override
	public String toString() {
		return "MacroPattern [limitTo=" + limitTo + ", getName()=" + getName()
				+ "]";
	}

	@Override
	public int getSearchFor() {
		return SEARCHFOR_MACRO;
	}

	@Override
	public String labelString() {
		final String s = getName();
		if (!s.startsWith("?")) {
			return "?" + s;
		}
		return s;
	}

}