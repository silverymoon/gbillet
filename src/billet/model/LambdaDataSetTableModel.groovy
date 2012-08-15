package billet.model

import groovy.sql.DataSet

class LambdaDataSetTableModel extends DataSetTableModel {

    def lambdas
    def columnCount

    LambdaDataSetTableModel(DataSet ds, String tname, String idCol, List colNames, List     colHeaders, Map filters, Map lmbs, Closure fltrSetter){
        super(ds, tname, idCol, colNames, colHeaders, filters, fltrSetter)
        lambdas = lmbs
        colCount = colNames.size() + lambdas.keySet().size()
    }

    Object getValueAt(int row, int column){
        if(row == rowCount - 1){
            return getEmptyValueByColumnClass(column)
        }
        if(column < columnNames.size()){
            return super.getValueAt(row, column)
        }
        return lambdas[columnHeaders[column]](getRowAt(row))
    }

    Class<?> getColumnClass(int columnIndex){
        if(columnIndex >= columnNames.size()){
            if(getRowCount() > 0){
                return lambdas[columnHeaders[columnIndex]](getRowAt(0)).class
            }
            return Object.class
        }
        return super.getColumnClass(columnIndex)
    }

    void setValueAt(Object value, int row, int col){
        super.setValueAt(value, row, col)
        def i = 0
        def cols = columnNames.size()
        lambdas.each{
            fireTableCellUpdated(row, cols + i)
            i++
        }
    }


    boolean isCellEditable(int row, int col){
        if(col < columnNames.size()){0
            return true
        }
        return false
    }

}
