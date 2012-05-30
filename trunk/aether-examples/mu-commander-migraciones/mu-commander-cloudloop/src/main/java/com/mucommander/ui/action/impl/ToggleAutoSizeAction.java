/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2010 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.ui.action.impl;

import com.mucommander.conf.impl.MuConfiguration;
import com.mucommander.ui.action.*;
import com.mucommander.ui.main.MainFrame;

import javax.swing.KeyStroke;
import java.util.Hashtable;

/**
 * This action toggles the 'auto-size columns' option on the currently active FileTable, which automatically resizes
 * columns to fit the table's width.
 *
 * @author Maxence Bernard
 */
public class ToggleAutoSizeAction extends MuAction {

    public ToggleAutoSizeAction(MainFrame mainFrame, Hashtable<String,Object> properties) {
        super(mainFrame, properties);
    }

    @Override
    public void performAction() {
        boolean enabled;

        mainFrame.setAutoSizeColumnsEnabled(enabled = !mainFrame.isAutoSizeColumnsEnabled());
        MuConfiguration.setVariable(MuConfiguration.AUTO_SIZE_COLUMNS, enabled);
    }
    
    public static class Factory implements ActionFactory {

		public MuAction createAction(MainFrame mainFrame, Hashtable<String,Object> properties) {
			return new ToggleAutoSizeAction(mainFrame, properties);
		}
    }
    
    public static class Descriptor extends AbstractActionDescriptor {
    	public static final String ACTION_ID = "ToggleAutoSize";
    	
		public String getId() { return ACTION_ID; }

		public ActionCategory getCategory() { return ActionCategories.VIEW; }

		public KeyStroke getDefaultAltKeyStroke() { return null; }

		public KeyStroke getDefaultKeyStroke() { return null; }
    }
}