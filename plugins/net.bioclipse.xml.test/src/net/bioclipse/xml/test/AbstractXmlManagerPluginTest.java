/*******************************************************************************
 * Copyright (c) 2009  Egon Willighagen <egonw@users.sf.net>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contact: http://www.bioclipse.net/
 ******************************************************************************/
package net.bioclipse.xml.test;

import junit.framework.Assert;
import net.bioclipse.ui.business.UIManager;
import net.bioclipse.xml.business.IXmlManager;

import org.junit.Test;

public abstract class AbstractXmlManagerPluginTest {

    protected static IXmlManager managerNamespace;
    
    private static UIManager ui = new UIManager();
    
    @Test public void testIsValid() throws Exception {
        Assert.fail("Not implemented yet.");
    }

    @Test public void testIsWellFormed() throws Exception {
        String filename = "/Virtual/" + this.hashCode() + ".xml";
        ui.newFile(filename,
            "<foo/>"
        );
        Assert.assertTrue(managerNamespace.isWellFormed(filename));
    }

    @Test public void testIsNotWellFormed() throws Exception {
        String filename = "/Virtual/" + this.hashCode() + ".xml";
        ui.newFile(filename,
            "<foo>"
        );
        Assert.assertFalse(managerNamespace.isWellFormed(filename));
    }

    @Test public void testListNamespaces() {
        Assert.fail("Not implemented yet.");
    }

    @Test public void testValidateAgainstSchematron() {
        Assert.fail("Not implemented yet.");
    }

    @Test public void testValidateAgainstRelaxNG() {
        Assert.fail("Not implemented yet.");
    }

    @Test public void testValidateAgainstXMLSchema() {
        Assert.fail("Not implemented yet.");
    }

}
