package billet.view

import javax.swing.JTable
import java.awt.Component
import java.awt.Color
import javax.swing.table.DefaultTableCellRenderer

class ColouredBGCellRenderer extends DefaultTableCellRenderer{

    def bgcol

    def ColouredBGCellRenderer(Color bg){
        bgcol = bg
    }

    def ColouredBGCellRenderer(){
        this(Color.LIGHT_GRAY)
    }

    Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,    boolean hasFocus, int row, int column){
        setBackground(bgcol)
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    }

}