/**

* This file is part of Base Modules.
 *
 * Copyright (c) 2010, Ben Fortuna [fortuna@micronode.com]
 *
 * Base Modules is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Base Modules is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Base Modules.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.mnode.rebound

import groovy.swing.SwingXBuilder

import groovy.swing.LookAndFeelHelper
import javax.swing.UIManager
import org.jdesktop.swingx.treetable.AbstractTreeTableModel
import java.awt.Insets
import java.awt.TrayIcon
import java.awt.SystemTray
import java.awt.PopupMenu
import java.awt.MenuItem
import java.awt.Component
import java.awt.event.MouseEvent
import org.jvnet.substance.SubstanceLookAndFeel
import org.jvnet.substance.api.SubstanceConstants
import javax.swing.filechooser.FileSystemView
import javax.swing.JFrame
import javax.swing.JTree
import javax.swing.tree.DefaultTreeCellRenderer
import com.ocpsoft.pretty.time.PrettyTime
import org.mnode.base.commons.FileComparator
import org.mnode.base.desktop.PaddedIcon
/*
@Grapes([
    @Grab(group='org.mnode.base', module='base-commons', version='0.0.1-SNAPSHOT'),
    @Grab(group='com.ocpsoft', module='ocpsoft-pretty-time', version='1.0.5'),
    @Grab(group='org.apache.xmlgraphics', module='batik-awt-util', version='1.7'),
//    @Grab(group='org.apache.xmlgraphics', module='batik-swing', version='1.7'),
    @Grab(group='org.mnode.base', module='base-desktop', version='0.0.1-SNAPSHOT', transitive=false),
    @Grab(group='org.codehaus.griffon.swingxbuilder', module='swingxbuilder', version='0.1.6', transitive=false),
    @Grab(group='net.java.dev.substance', module='substance', version='5.3'),
    @Grab(group='net.java.dev.substance', module='substance-swingx', version='5.3'),
    @Grab(group='org.swinglabs', module='swingx', version='0.9.2')])
    */
class Rebound {

     static void close(def frame, def exit) {
         if (exit) {
             System.exit(0)
         }
         else {
             frame.visible = false
         }
     }

    static void main(def args) {
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0))
        LookAndFeelHelper.instance.addLookAndFeelAlias('substance5', 'org.jvnet.substance.skin.SubstanceNebulaLookAndFeel')
        def swing = new SwingXBuilder()

        def openTab = { tabs, node ->
            if (tabs.tabCount > 0) {
                for (i in 0..tabs.tabCount - 1) {
                    if (tabs.getComponentAt(i).getClientProperty('rebound.node') == node) {
                        tabs.selectedComponent = tabs.getComponentAt(i)
                        return
                    }
                }
            }
            
            swing.edt {
                def newTab = panel(name: node.name)
                newTab.putClientProperty(SubstanceLookAndFeel.TABBED_PANE_CLOSE_BUTTONS_PROPERTY, true)
                newTab.putClientProperty('rebound.node', node)
                tabs.add newTab
                tabs.selectedComponent = newTab
                def tabIndex = tabs.indexOfComponent(newTab)
                tabs.setIconAt(tabIndex, new PaddedIcon(FileSystemView.fileSystemView.getSystemIcon(node), 16, 18))
                tabs.setToolTipTextAt(tabIndex, node.absolutePath)
            }
        }
        
        swing.edt {
            lookAndFeel('substance5', 'system')
            
            frame(title: 'Rebound', size: [550, 450], show: true, locationRelativeTo: null,
                defaultCloseOperation: JFrame.DO_NOTHING_ON_CLOSE, iconImage: imageIcon('/logo-16.png', id: 'logo').image, id: 'reboundFrame') {
                
                menuBar {
                    menu(text: "File", mnemonic: 'F') {
                    }
                    menu(text: "Edit", mnemonic: 'E') {
                    }
                    menu(text: "View", mnemonic: 'V') {
                    }
                    menu(text: "Help", mnemonic: 'H') {
                    }
                }
                
                borderLayout()
                tabbedPane(border: emptyBorder(5), id: 'tabs') {
                    panel(name: 'File System') {
                        borderLayout()
                        scrollPane(border: null) {
                            treeTable(id: 'explorerTree')
                            explorerTree.treeTableModel = new FileSystemTreeTableModel()
                            explorerTree.rootVisible = false
//                            explorerTree.showsRootHandles = false
                            explorerTree.treeCellRenderer = new FileSystemTreeCellRenderer()
                            explorerTree.mouseClicked = { e ->
                                if (e.button == MouseEvent.BUTTON1 && e.clickCount >= 2) {
                                    if (explorerTree.selectedRow) {
                                        def node = explorerTree.getPathForRow(explorerTree.selectedRow).lastPathComponent
                                        if (!node.isDirectory()) {
                                            openTab(tabs, node)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                tabs.putClientProperty(SubstanceLookAndFeel.TABBED_PANE_CONTENT_BORDER_KIND, SubstanceConstants.TabContentPaneBorderKind.SINGLE_FULL)
            }
          
          if (SystemTray.isSupported()) {
              TrayIcon trayIcon = new TrayIcon(logo.image, 'Rebound')
              trayIcon.imageAutoSize = false
              trayIcon.mousePressed = { event ->
                  if (event.button == MouseEvent.BUTTON1) {
                      evaluatorFrame.visible = true
                  }
              }
              
              PopupMenu popupMenu = new PopupMenu('Rebound')
              MenuItem openMenuItem = new MenuItem('Open')
              openMenuItem.actionPerformed = {
                  reboundFrame.visible = true
              }
              popupMenu.add(openMenuItem)
              popupMenu.addSeparator()
              MenuItem exitMenuItem = new MenuItem('Exit')
              exitMenuItem.actionPerformed = {
                  close(reboundFrame, true)
              }
              popupMenu.add(exitMenuItem)
              trayIcon.popupMenu = popupMenu
              
              SystemTray.systemTray.add(trayIcon)
          }
      reboundFrame.windowClosing = {
          close(reboundFrame, !SystemTray.isSupported())
      }
        }
    }
}

class FileSystemTreeTableModel extends AbstractTreeTableModel {

    def comparator = new FileComparator()
    def lastModifiedFormat = new PrettyTime()
    
    FileSystemTreeTableModel() {
        super(FileSystemView.fileSystemView)
    }
    
    int getRowCount() {
        root.roots.size()
    }
    
    int getColumnCount() {
        4
    }
    
    Class getColumnClass(int column) {
        if (column == 1) {
            return Long.class
        }
        return String.class
    }
    
    String getColumnName(int column) {
        switch(column) {
            case 0: return 'Name'
            case 1: return 'Size'
            case 2: return 'Type'
            case 3: return 'Modified'
        }
        return null
    }
    
    Object getValueAt(Object node, int column) {
        if (node instanceof FileSystemView) {
            return null
        }
        else {
            switch(column) {
                case 0: return root.getSystemDisplayName(node)
                case 1: return (!node.isDirectory()) ? node.length() : null
                case 2: return root.getSystemTypeDescription(node)
                case 3: return lastModifiedFormat.format(new Date(node.lastModified()))
            }
        }
        return null
    }
    
    Object getChild(Object parent, int index) {
        if (parent instanceof FileSystemView) {
            def roots = parent.roots
            Arrays.sort(roots, comparator)
            return roots[index]
        }
        else {
            def files = parent.listFiles()
            Arrays.sort(files, comparator)
            return files[index]
        }
    }
    
    int getChildCount(Object parent) {
        if (parent instanceof FileSystemView) {
            return parent.roots.length
        }
        else {
            return parent.listFiles().length
        }
    }
    
    int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof FileSystemView) {
            def roots = parent.roots
            Arrays.sort(roots, comparator)
            for (int i = 0; i < roots.length; i++) {
                if (child == roots[i]) {
                    return i
                }
            }
        }
        else {
            def files = parent.listFiles
            Arrays.sort(files, comparator)
            for (int i = 0; i < files.length; i++) {
                if (child == files[i]) {
                    return i
                }
            }
        }
    }
    
    boolean isLeaf(Object node) {
        return node instanceof File && !node.isDirectory()
    }
}

class FileSystemTreeCellRenderer extends DefaultTreeCellRenderer {

    def fsv = FileSystemView.fileSystemView
    
    Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
        if (value.exists()) {
            icon = fsv.getSystemIcon(value)
            text = fsv.getSystemDisplayName(value)
        }
        else {
            icon = null
        }
        return this
    }
}
