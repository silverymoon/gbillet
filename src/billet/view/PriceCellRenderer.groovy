package billet.view

import javax.swing.JTable
import java.awt.Component
import java.awt.Color
import javax.swing.table.DefaultTableCellRenderer
import java.text.DecimalFormat

class PriceCellRenderer extends ColouredBGCellRenderer {

    def formatter = new DecimalFormat("#0.00€")

    def PriceCellRenderer(Color bg){
        super(bg)
    }

    def PriceCellRenderer(){
        super(Color.WHITE)
    }

    Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,    boolean hasFocus, int row, int column){

        if(value == null || value == "" || (value as String).matches(".*?[^0-9.,].*")){
            value = 0.0
        }
        return super.getTableCellRendererComponent(table, formatter.format(value), isSelected, hasFocus, row, column)
    }

}