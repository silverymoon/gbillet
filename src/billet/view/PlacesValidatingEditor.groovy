package billet.view

import javax.swing.JTable
import java.awt.Component
import javax.swing.DefaultCellEditor
import javax.swing.JOptionPane
import billet.IBilletController


class PlacesValidatingEditor extends DefaultCellEditor{

    def controller
    def row
    def column

    PlacesValidatingEditor(IBilletController contr, comp){
        super(comp)
        controller = contr
    }

    boolean stopCellEditing(){
        def val = 0
        try {
          val = getCellEditorValue() as int
        }
        catch (Exception e) {}
        if(!controller.isCapacityEnough(val, row, column)){
            JOptionPane.showMessageDialog(editorComponent, "Es sind nicht mehr genügend Plätze frei!", "Achtung", JOptionPane.ERROR_MESSAGE)
            return false
        }
        return super.stopCellEditing()
    }

    Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int ro, int col){
        row = ro
        column = col
        return super.getTableCellEditorComponent(table, value, isSelected, row, column)
    }

}