/*******************************************************************************
 * Copyright (c) 2004 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.core.model.erlang;

public interface IErlMessage extends IErlMember {

    String getMessage();

    String getData();

    enum MessageKind {
        INFO, WARNING, ERROR
    }

    MessageKind getMessageKind();

}
