package billet.view

import java.awt.Component
import java.awt.Dimension
import java.awt.Color
import javax.swing.WindowConstants
import javax.swing.UIManager
import javax.swing.JTable
import javax.swing.JCheckBox
import javax.swing.JScrollPane
import javax.swing.table.TableColumn
import javax.swing.table.TableCellRenderer
import javax.swing.DefaultCellEditor
import javax.swing.RowFilter
import javax.swing.table.TableRowSorter

import groovy.swing.SwingBuilder

import billet.model.DataSetTableModel
import billet.IBilletController

class BilletView {

    private gui
    private controller
    private datecbxmodel
    private tickettable
    private rowInTTablePopup
    private namefield
    private rowSorter
    private currentFilters = [] as Set


    BilletView(IBilletController contr){
        controller = contr
        def evmodel = controller.getEventTableModel()
        def tmodel = controller.getTicketTableModel()
        datecbxmodel = controller.getComboBoxModelButton()
        JTable eventtable
        JScrollPane epane
        JScrollPane tpane
        gui = new SwingBuilder().frame(id: "gui", title: "Billet Version 0.9",
         defaultCloseOperation: WindowConstants.EXIT_ON_CLOSE, size: [640, 800] ){
             vbox{
                 label(text: "Veranstaltungen")
                 epane = scrollPane(preferredSize: [600, 150]){
                     eventtable = table(model: evmodel)
                 }
                 hbox{
                     label(text: "Anzeige:")
                     checkBox(text: "mit Namen", actionPerformed: this.&filterByName)
                     namefield = textField(columns: 8)
                     hglue{}
                 }
                 hbox{
                     checkBox(text: "nur bezahlte", actionPerformed: this.&filterByPaid)
                     checkBox(text: "nur im Umschlag", actionPerformed: this.&filterByPackaged )
                     checkBox(text: "nur für ausgewähltes Datum", actionPerformed: this.&filterByDate)
                     comboBox(model: controller.getComboBoxModelButton())
                     hglue{}
                 }
                 label(text: "Tickets")
                 tpane = scrollPane(preferredSize: [600, 400]){
                    tickettable = table(model: tmodel, mousePressed: this.&mouseEventInTable, mouseReleased: this.&mouseEventInTable)
                 }
                 vglue{}
                 hbox {
                    button(text: "Speichern",
                        actionPerformed: this.&save)
                    button(text: "Beenden",
                        actionPerformed: this.&exit)
                }
             }
        }
        setupEditors(tickettable)
        setupRenderers(tickettable, eventtable)
        rowSorter = new TableRowSorter(controller.getTicketTableModel())
        tickettable.setRowSorter(rowSorter)
        def ew = setupColWidths(eventtable)
        def tw = setupColWidths(tickettable)
        def width = Math.max(ew, tw) as int
        epane.setPreferredSize(new Dimension(width, 150))
        tpane.setPreferredSize(new Dimension(width, 400))
        gui.setPreferredSize(new Dimension(width + 40, 600))
        gui.pack()
        gui.setLocationRelativeTo(null)
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }

    def setupColWidths(table){
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
        def sum = 0
        (0..table.getColumnCount() - 1).each{ colnr ->
          TableColumn col = table.getColumnModel().getColumn(colnr)
          TableCellRenderer renderer = col.getHeaderRenderer()
          if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer()
          }
          Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0)
          def width = comp.getPreferredSize().width as int
          // add margin
          width += 6
          col.setPreferredWidth(width)
          sum += width
        }
        return sum
    }

    def setupEditors(tickettable){
        TableColumn dateCol = tickettable.getColumnModel().getColumn(1)
        dateCol.setCellEditor(new DefaultCellEditor(editDates()))
        def placesEditor = new PlacesValidatingEditor(controller, textField())
        (5..8).each {colnr ->
            TableColumn col = tickettable.getColumnModel().getColumn(colnr)
            col.setCellEditor(placesEditor)
        }
    }

    def setupRenderers(tickettable, eventtable){
        tickettable.getColumnModel().getColumn(11).setCellRenderer(new PriceCellRenderer(Color.LIGHT_GRAY))
        def prend = new PriceCellRenderer()
        tickettable.getColumnModel().getColumn(9).setCellRenderer(prend)
        (3..6).each{ colnr ->
            eventtable.getColumnModel().getColumn(colnr).setCellRenderer(prend)
        }
        def crend = new ColouredBGCellRenderer()
        (7..8).each{ colnr ->
            eventtable.getColumnModel().getColumn(colnr).setCellRenderer(crend)
        }

    }

    def textField(){
        return new SwingBuilder().textField()
    }

    def editDates(){
        return new SwingBuilder().comboBox(model: controller.getComboBoxModelTable())
    }

    def popUp(){
        return new SwingBuilder().popupMenu {
            menuItem{
                action(name:'Löschen', closure:this.&deleteLine)
            }
        }
    }

    def exit(event){
        controller.close()
        System.exit(0)
    }

    def save(event){
        controller.save()
    }

    def mouseEventInTable(event){
        if (event.isPopupTrigger()) {
            rowInTTablePopup = rowSorter.convertRowIndexToModel(tickettable.rowAtPoint(event.getPoint()))
            popUp().show(event.getComponent(), event.getX(), event.getY())
        }
    }

    def deleteLine(event){
        controller.deleteTicket(rowInTTablePopup)
    }

    def filter(RowFilter filter, boolean selected){
        if(selected){
            currentFilters << filter
        }
        else{
            currentFilters = currentFilters - filter
        }
        if(currentFilters.size() > 0){
            rowSorter.setRowFilter(RowFilter.andFilter(currentFilters))
        }
        else{
            rowSorter.setRowFilter(null)
        }
    }

    def filterByPaid(event){
        def cbx = event.getSource() as JCheckBox
        filter(controller.getPaidFilter(), cbx.isSelected())
    }

    def filterByDate(event){
        def cbx = event.getSource() as JCheckBox
        filter(controller.getDateFilter(datecbxmodel.getSelectedItem()), cbx.isSelected())
    }

    def filterByPackaged(event){
        def cbx = event.getSource() as JCheckBox
        filter(controller.getPackagedFilter(), cbx.isSelected())
    }

    def filterByName(event){
        def cbx = event.getSource() as JCheckBox
        filter(controller.getNameFilter(namefield.getText()), cbx.isSelected())
    }

    def show(){
        gui.show()
    }
}