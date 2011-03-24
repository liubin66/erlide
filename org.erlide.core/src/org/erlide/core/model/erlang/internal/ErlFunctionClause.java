/*******************************************************************************
 * Copyright (c) 2005 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.core.model.erlang.internal;

import java.util.ArrayList;
import java.util.List;

import org.erlide.core.common.Util;
import org.erlide.core.model.erlang.IErlElement;
import org.erlide.core.model.erlang.IErlFunction;
import org.erlide.core.model.erlang.IErlFunctionClause;
import org.erlide.core.model.erlang.IParent;

import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;

public class ErlFunctionClause extends ErlMember implements IErlFunctionClause {

    final String head;
    final List<String> parameters;

    protected ErlFunctionClause(final IParent parent, final String name,
            final String head, final OtpErlangList parameters) {
        super(parent, name);
        this.head = head;
        this.parameters = getParameters(parameters);
    }

    public static ArrayList<String> getParameters(final OtpErlangList parameters) {
        final ArrayList<String> pars = new ArrayList<String>(parameters.arity());
        for (final OtpErlangObject i : parameters) {
            pars.add(Util.stringValue(i));
        }
        return pars;
    }

    public String getHead() {
        return head;
    }

    public Kind getKind() {
        return Kind.CLAUSE;
    }

    /**
     * @param arguments
     *            the arguments to set
     */
    // public void setArguments(final String arguments) {
    // this.arguments = arguments;
    // }
    /**
     * @param guards
     *            the guards to set
     */
    // public void setGuards(final OtpErlangList guards) {
    // this.guards = guards;
    // }
    @Override
    public String toString() {
        return head;
    }

    public String getFunctionName() {
        final IErlElement element = (IErlElement) getParent();
        return element.getName();
    }

    public int getArity() {
        final IErlFunction f = (IErlFunction) getParent();
        return f.getArity();
    }

    public List<String> getParameters() {
        return parameters;
    }
}
