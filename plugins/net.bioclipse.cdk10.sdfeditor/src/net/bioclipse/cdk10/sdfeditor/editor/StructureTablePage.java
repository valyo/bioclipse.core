/*******************************************************************************
 * Copyright (c) 2008 The Bioclipse Project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Ola Spjuth
 *     
 ******************************************************************************/
package net.bioclipse.cdk10.sdfeditor.editor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.bioclipse.cdk10.jchempaint.ui.editor.JCPPage;
import net.bioclipse.cdk10.jchempaint.ui.editor.mdl.MDLMolfileEditor;
import net.bioclipse.core.util.LogUtils;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class StructureTablePage extends FormPage implements ISelectionListener{

    private static final Logger logger = Logger.getLogger(StructureTablePage.class);

    //Declare constants for use in table
    public static final int INDEX_COLUMN=0;
    public static final int STRUCTURE_COLUMN=1;

    private Table table;
    private TableViewer viewer;
    private String[] colHeaders;

    /** Registered listeners */
    private List<ISelectionChangedListener> selectionListeners;

    /** Store last selection */
    private StructureEntitySelection selectedRows;
    
    //Store the parent MPE
    SDFEditor sdfEditor;
    

    public StructureTablePage(FormEditor editor, String[] colHeaders) {
        super(editor, "bc.structuretable", "Structure table");
        this.colHeaders=colHeaders;
        selectionListeners=new ArrayList<ISelectionChangedListener>();
        
        if ( editor instanceof  SDFEditor) {
            sdfEditor = (SDFEditor) editor;
        }

//      selectedRows=new StructureEntitySelection();
    }

    /**
     * Add content to form
     */
    @Override
    protected void createFormContent(IManagedForm managedForm) {

        FormToolkit toolkit = managedForm.getToolkit();
        ScrolledForm form = managedForm.getForm();
        form.setText("Structure table");
//      form.setBackgroundImage(FormArticlePlugin.getDefault().getImage(FormArticlePlugin.IMG_FORM_BG));
        final Composite body = form.getBody();
        FillLayout layout=new FillLayout();
        body.setLayout(layout);

        viewer = new TableViewer(body, SWT.BORDER  | SWT.MULTI |  SWT.FULL_SELECTION | SWT.VIRTUAL);
        table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        toolkit.adapt(table, true, true);

        TableLayout tableLayout = new TableLayout();
        table.setLayout(tableLayout);

        //Add index column
        TableViewerColumn ixcol=new TableViewerColumn(viewer,SWT.BORDER);
        ixcol.getColumn().setText("Index");
        tableLayout.addColumnData(new ColumnPixelData(50));
        ColumnViewerSorter cSorter = new ColumnViewerSorter(viewer,ixcol) {
            protected int doCompare(Viewer viewer, Object e1, Object e2) {
                StructureTableEntry s1=(StructureTableEntry)e1;
                StructureTableEntry s2=(StructureTableEntry)e2;
                if (s1.index==s2.index) return 0;
                if (s1.index<s2.index) return -1;
                return +1;
            }
          };

        
        //Add Structure column
        TableViewerColumn col=new TableViewerColumn(viewer,SWT.BORDER);
        col.getColumn().setText("Structure");
        tableLayout.addColumnData(new ColumnPixelData(100));

        int colIndex=0;
        for (String colkey : colHeaders){
            TableViewerColumn col2=new TableViewerColumn(viewer,SWT.BORDER);
            col2.getColumn().setText(colkey);
            col2.getColumn().setAlignment(SWT.LEFT);
            tableLayout.addColumnData(new ColumnPixelData(100));
            new ColumnViewerSorter(viewer,col2, colIndex) {
                protected int doCompare(Viewer viewer, Object e1, Object e2) {
                    StructureTableEntry s1=(StructureTableEntry)e1;
                    StructureTableEntry s2=(StructureTableEntry)e2;
                    Object o1=s1.columns[getColIndex()];
                    Object o2=s2.columns[getColIndex()];
                    
                    return String.valueOf( o1 )
                        .compareTo( String.valueOf( o2 ));
                }
              };
              colIndex++;
        }
        
        viewer.addSelectionChangedListener( new ISelectionChangedListener(){
            public void selectionChanged( SelectionChangedEvent event ) {
                
                if ( event.getSelection() instanceof IStructuredSelection ) {
                    IStructuredSelection sel = (IStructuredSelection) event.getSelection();
                    Object obj=sel.getFirstElement();
                    if ( obj instanceof StructureTableEntry ) {
                        StructureTableEntry entry = (StructureTableEntry) obj;
                        sdfEditor.setCurrentModel( entry.index );
                    }
                }
                
            }
            
        });

        viewer.addDoubleClickListener( new IDoubleClickListener(){

            public void doubleClick( DoubleClickEvent event ) {
                    
                    int ix = getSelectedIndex( event.getSelection() );

//                    sdfEditor.setCurrentModel( ix );

                    if (ix<0){
                        //Should not happen
                        showMessage( "Index of double-clicked is negative." );
                        return;
                    }
                    
                    logger.debug( "DC detected on index: " + ix 
                                  + " in entry list" );
                    
                    sdfEditor.setCurrentModel( ix );
                    sdfEditor.pageChange( 1 );
                    sdfEditor.getActivePageInstance().setFocus();

            }


        });

        viewer.setContentProvider(new MoleculeListContentProvider());
        viewer.setLabelProvider(new MoleculeListLabelProviderNew());
        viewer.setUseHashlookup(true);
        OwnerDrawLabelProvider.setUpOwnerDraw(viewer);

        StructureTableEntry[] mlist=((SDFEditor)getEditor()).getEntries();
        if (mlist!=null){
            logger.debug("Setting table input with: " + mlist.length + " molecules.");
            viewer.setInput(mlist);
        }
        else{
            logger.debug("Editor moleculeList is empty.");
        }

        //Set sorter
        cSorter.setSorter(cSorter, ColumnViewerSorter.ASC);

        //Post selections in Table to Eclipse
        getSite().setSelectionProvider(viewer);

        
        getSite().getPage().addSelectionListener(this);

    }

//    protected IEditorInput createEditorInput( StructureTableEntry entry,
//                                              StructureTableEntry[] entries ) {
//
//        // TODO Auto-generated method stub
//        return null;
//    }

    /*
     * Below is for providing selections from table to e.g. outline view
     */

    public int getSelectedIndex( ISelection selection ) {

        if (!( selection instanceof IStructuredSelection ))
                return -1;

        IStructuredSelection ssel 
            = (IStructuredSelection) selection;
        Object obj=ssel.getFirstElement();
        if (!( obj instanceof StructureTableEntry )) {
            logger.debug("DC on something else than " +
                "StructureTableEntry. Not handled.");
            return -1;
        }

        if (!( getEditorInput() instanceof IFileEditorInput )) {
            showMessage( "This operation is only available " +
                "if backed by a File." );
            return -1;
        }
        
        //Our casted editor
        SDFEditor sdfEditor=(SDFEditor)getEditor();
        
        //This is the DC'ed entry
        StructureTableEntry entry = (StructureTableEntry) obj;

        //These are all entries in editor
        StructureTableEntry[] entries 
            = sdfEditor.entries;

        int ix=-1;
        //Find index of the DC'ed entry
        for (int i=0; i<sdfEditor.entries.length; i++){
            if (sdfEditor.entries[i].equals( entry )){
                ix=i;
            }
        }
        return ix;
    }
    
    
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        if(!selectionListeners.contains(listener))
        {
            selectionListeners.add(listener);
        }
    }

    public ISelection getSelection() {

        TableItem[] itm=(TableItem[])viewer.getTable().getSelection();
        if (itm==null || itm.length<=0) return null;

        //Hold new selection
        Set<StructureTableEntry> newSet=new HashSet<StructureTableEntry>();

        //Recurse
        for(TableItem item : itm){
            if (item.getData() instanceof StructureTableEntry) {
                StructureTableEntry entry = (StructureTableEntry) item.getData();
                System.out.println("** Added selected in structtab: " + entry.getMoleculeImpl().hashCode());
                newSet.add(entry);
            }
        }

        selectedRows=new StructureEntitySelection(newSet);
        return selectedRows;
        
    }

    public void removeSelectionChangedListener(
                                               ISelectionChangedListener listener) {
        if(selectionListeners.contains(listener))
            selectionListeners.remove(listener);
    }

    public void setSelection(ISelection selection) {
        if (!(selection instanceof StructureEntitySelection)) return;
        this.selectedRows=(StructureEntitySelection)selection;
        
        //Also set selected in viewer
        viewer.setSelection( (StructureEntitySelection)selection );
        
    }

    private void showMessage(String message) {
        MessageDialog.openInformation(
                                      viewer.getControl().getShell(),
                                      "Message",
                                      message);
    }


    public void selectionChanged( IWorkbenchPart part, ISelection selection ) {

        if (part.equals( this )) return;
        if (part.equals( sdfEditor )) return;

        if (!( selection instanceof IStructuredSelection )) return;
        IStructuredSelection sel = (IStructuredSelection) selection;

        if (selectedRows!=null)
            if (selectedRows.equals( selection )) return;
        
        System.out.println("Table listen: " + selection.toString());
        

        //Collect supported selected objects
        HashSet<StructureTableEntry> set=new HashSet<StructureTableEntry>();
        for (Object obj : sel.toList()){
            if ( obj instanceof StructureTableEntry ) {
                StructureTableEntry entry = (StructureTableEntry) obj;
                set.add( entry );
            }
        }

        //Set as selection in table
        selectedRows=new StructureEntitySelection(set);
        setSelection( selectedRows);
        
    }

}
