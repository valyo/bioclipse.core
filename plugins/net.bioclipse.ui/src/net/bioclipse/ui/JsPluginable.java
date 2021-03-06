/* *****************************************************************************
 *Copyright (c) 2008-2009 The Bioclipse Team and others.
 *All rights reserved. This program and the accompanying materials
 *are made available under the terms of the Eclipse Public License v1.0
 *which accompanies this distribution, and is available at
 *http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package net.bioclipse.ui;

import java.io.IOException;
import java.util.List;

import net.bioclipse.scripting.OutputProvider;

public interface JsPluginable {

    void eval(List<Object> al) throws IOException;

    void setOutputProvider(OutputProvider outputProvider);
}