package billet

import javax.swing.RowFilter
import javax.swing.table.TableModel
import javax.swing.MutableComboBoxModel


interface IBilletController {

    TableModel getTicketTableModel()

    TableModel getEventTableModel()

    MutableComboBoxModel getComboBoxModelTable()

    MutableComboBoxModel getComboBoxModelButton()

    boolean isCapacityEnough(int tickets, int row, int column)

    def close()

    def save()

    def deleteTicket(row)

    RowFilter getDateFilter(date)

    RowFilter getPaidFilter()

    RowFilter getPackagedFilter()

    RowFilter getNameFilter(name)
}

