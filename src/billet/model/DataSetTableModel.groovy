package billet.model

import javax.swing.table.AbstractTableModel
import javax.swing.event.TableModelEvent
import groovy.sql.DataSet

/** CAVEAT: the table needs to have autoincrement set! also, not null is not advised :)
 **/

class DataSetTableModel extends AbstractTableModel {

    def dataSet
    def rows
    def colCount
    def rowCount
    def tableName
    def columnNames
    def idColumn
    def columnHeaders
    def rowsById
    def filters
    def originalRows
    def currentFilters
    def filterSetter

    DataSetTableModel(DataSet ds, String tname, String idCol, List colNames, List colHeaders, Map fltrs, Closure fltrSetter){
        dataSet = ds
        tableName = tname
        rows = getRows()
        colCount = colNames.size()
        rowCount = rows.size() + 1
        columnNames = colNames
        idColumn = idCol
        columnHeaders = colHeaders
        rowsById = [:]
        rows.eachWithIndex{ row, idx -> rowsById.put(row['id'], idx) }
        currentFilters = []
        filters = fltrs
        if(fltrSetter == null){
            filterSetter = {val, row, col -> return val}
        }
        else{
            filterSetter = fltrSetter
        }
    }

    private getRows(){
        originalRows = dataSet.rows("select * from " + tableName + " where not deleted")
        return originalRows
    }

    Class<?> getColumnClass(int columnIndex){
        if(rows.size() > 0){
            def cls = rows[0].getProperty(columnNames[columnIndex])?.class
            if(cls == null){
                return Object.class
            }
            return cls
        }
        return Object.class
    }

    String getColumnName(int col){
        return columnHeaders[col]
    }

    int getRowCount(){
        return rowCount
    }

    int getColumnCount(){
        return colCount
    }

    def getRowAt(idx){
        return rows[idx]
    }

    Object getValueAt(int row, int column){
        if(row == rowCount - 1){
            return getEmptyValueByColumnClass(column)
        }
        return rows[row].getProperty(columnNames[column])
    }

    def getEmptyValueByColumnClass(column){
        def cls = getColumnClass(column)
        switch(cls){
            case Boolean:
                return false
            case Double:
                return 0.0
            case Integer:
                return 0
            default:
                return ""
        }
    }

    boolean isCellEditable(int row, int col){
        return true
    }

    void setValueAt(Object value, int row, int col){
        value = filterSetter(value, row, col)
        if(row == rowCount - 1){ // insert
            dataSet.executeInsert("insert into " + tableName + "(" + columnNames[col] + ", deleted) values('$value', false)")
            rows = getRows()
            rowCount++
            fireTableRowsInserted(rowCount - 2, rowCount - 1)
        }
        else { // update
            def colname = columnNames[col]
            rows[row].put(colname, value)
            dataSet.execute("update " + tableName + " set " + colname + " = '$value' where " + idColumn + " = " + rows[row].get(idColumn))
            fireTableCellUpdated(row, col)
        }
    }

    void addRow(values){
        setValueAt(values[0], rowCount - 1, 0)
        def r = rowCount - 2
        values.eachWithIndex { val, idx ->
          setValueAt(val, r, idx)}
    }

    void deleteRow(row){
        if(row < rowCount - 1){
            rowCount--
            fireTableRowsDeleted(row, row)
            dataSet.execute("update " + tableName + " set deleted = true where id = " + rows[row].id)
            rows = getRows()
        }
    }

    void fireTableCellWithIdUpdated(id, col){
        fireTableCellUpdated(rowsById[id], col)
    }

}