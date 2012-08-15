package billet.model

import groovy.sql.Sql
import groovy.sql.DataSet
import groovy.json.JsonSlurper
import java.sql.SQLException
import billet.IBilletController
import billet.model.DataSetTableModel
import javax.swing.DefaultComboBoxModel
import javax.swing.MutableComboBoxModel
import javax.swing.table.TableModel
import javax.swing.event.TableModelListener
import javax.swing.event.TableModelEvent
import javax.swing.RowFilter

class BilletDAO implements IBilletController{

    def sql
    def ticketmodel
    def eventmodel
    def comboboxmodeltable
    def comboboxmodelbutton

    BilletDAO(){
        this(Sql.newInstance("jdbc:h2:billet", "org.h2.Driver"))
    }

    BilletDAO(sqlh){
        sql = sqlh
        prepareDatabase()
        prepareModels()
    }

    TableModel getTicketTableModel(){
        return ticketmodel
    }

    TableModel getEventTableModel(){
        return eventmodel
    }

    MutableComboBoxModel getComboBoxModelTable(){
        return comboboxmodeltable
    }

    MutableComboBoxModel getComboBoxModelButton(){
        return comboboxmodelbutton
    }

    MutableComboBoxModel makeComboBoxModel(){
        def combobox = new DefaultComboBoxModel()
        sql.eachRow("select event_date from event"){
            row ->
              combobox.addElement(row['event_date'])
        }
        return combobox
    }

    def eventUpdated(TableModelEvent tme){
        // Preisänderung an die Ticket-Tabelle
        if(tme.getColumn() < 7 && tme.getColumn() > 2){
            def date = eventmodel.getValueAt(tme.getFirstRow(), 0)
            sql.eachRow("select * from ticket where event_date = $date and not deleted"){
                row ->
                    ticketmodel.fireTableCellWithIdUpdated(row['id'] , 7)
            }
        }// Datum geändert an die Combobox
        if(tme.getColumn() == 0){
            changeComboBoxModel(comboboxmodeltable, tme.getFirstRow())
            changeComboBoxModel(comboboxmodelbutton, tme.getFirstRow())
        }
    }

    def changeComboBoxModel(cbxmdl, row){
        if(cbxmdl.getSize() > row){
            cbxmdl.removeElementAt(row)
        }
        cbxmdl.insertElementAt(eventmodel.getValueAt(row, 0), row)
    }

    def ticketUpdated(TableModelEvent tme){
        if(tme.getColumn() >= 5 && tme.getColumn() <= 8){ // Anzahl geändert
            def val = ticketmodel.getValueAt(tme.getFirstRow(), 1)
            if(val != null){
                def row = getEventForDate(val)
                if(row != null){
                    def id = row['id']
                    eventmodel.fireTableCellWithIdUpdated(id, 7)
                    eventmodel.fireTableCellWithIdUpdated(id, 8)
                    eventmodel.fireTableCellWithIdUpdated(id, 9)
                }
            }
        }
        else if(tme.getType() == TableModelEvent.DELETE){
            eventmodel.fireTableDataChanged()
        }
    }

    boolean isCapacityEnough(int tickets, int row, int column){
        def val = ticketmodel.getValueAt(row, 1)
        if(val != null){
            def erow = getEventForDate(val)
            if(erow != null){
                if(column < 4){ // with food - easy
                    return getCapacityFood(erow) >= tickets
                }
                else{ // just show, but we can always give food places over to just show guys
                    def capshow = getCapacityShow(erow)
                    def capfood = getCapacityFood(erow)
                    return capshow + capfood >= tickets
                }
            }
        }// weird, no row / event, but let's believe the user
        return true
    }

    int asInt(val){
        try {
            return val as int
        }
        catch (Exception e) {}
        return 0
    }

    def deleteTicket(row){
        ticketmodel.deleteRow(row)
    }

    int getPrice(row){
        def event = getEventForDate(row['event_date'])
        def price = 0
        def (nft, nftr, nst, nstr) = [0, 0, 0, 0]
        nft = asInt(row.num_food_tickets)
        nftr = asInt(row.num_food_tickets_reduced)
        nst = asInt(row.num_show_tickets)
        nstr = asInt(row.num_show_tickets_reduced)
        if(event != null){
            price += event.price_food_full * nft
            price += event.price_food_reduced * nftr
            price += event.price_show_full * nst
            price += event.price_show_reduced * nstr
        }
        def paid = 0
        try {
           paid = row['amount_paid'] as double
        }
        catch(Exception e){}
        if(paid == null || paid == ''){
            paid = 0
        }
        return price - paid
    }

    def getEventForDate(String date){
        def rows = sql.rows("select * from event where event_date = $date")
        if(rows.size() > 0){
            return rows[0]
        }
        return null
    }

    def getTicketsForDate(String date){
        return sql.rows("select * from ticket where event_date = $date and not deleted")
    }

    int getCapacity(row, cap_col_name, tick_col_name){
        def capacity = 0
        try {
            capacity = row[cap_col_name] as int
        }
        catch (Exception e) {}
        sql.eachRow("select * from ticket where event_date = '" + row['event_date'] + "' and not deleted"){nrow ->
            capacity -= nrow[tick_col_name]
            capacity -= nrow[tick_col_name + "_reduced"]
        }
        return capacity
    }

    int getCapacityFood(row){
        return getCapacity(row, "capacity_food", "num_food_tickets")
    }

    int getCapacityShow(row){
        return getCapacity(row, "capacity_show", "num_show_tickets")
    }

    int getFull(row){
        def total = row["capacity_food"] + row['capacity_show']
        return total - (getCapacityFood(row) + getCapacityShow(row))
    }

    def isLastRow(entry){
        return entry.getIdentifier() == ticketmodel.getRowCount() - 1
    }


    RowFilter getDateFilter(date){
        return new RowFilterTemplate("DATE", {entry -> isLastRow(entry) || ticketmodel.getRowAt(entry.getIdentifier()).event_date == date})
    }

    RowFilter getPaidFilter(){
        return new RowFilterTemplate("PAID", {entry -> isLastRow(entry) || getPrice(ticketmodel.getRowAt(entry.getIdentifier())) <= 0})
    }

    RowFilter getPackagedFilter(){
        return new RowFilterTemplate("PACKAGED", {entry -> isLastRow(entry) || ticketmodel.getRowAt(entry.getIdentifier()).packaged})
    }

    RowFilter getNameFilter(name){
        return new RowFilterTemplate("NAME", {entry -> isLastRow(entry) || ticketmodel.getRowAt(entry.getIdentifier()).name.contains(name)})
    }

    def filterSetter(intCols, dblCols){
        return {val, row, col ->
            if(intCols.contains(col)){
                return asInt(val)
            }
            else if(dblCols.contains(col)){
                try{
                    return val as double
                }
                catch(Exception e){
                    return 0
                }
            }
            return val
        }
    }

    def save(){
        sql.commit()
        def bak = "blt-bak-" + new Date().format("yyyyMMdd_HHmmss")
        sql.execute("backup to '" + bak + ".zip'")
        sql.execute("call csvwrite ('" + bak + ".csv', 'select * from ticket where not deleted')")
    }

    def close(){
        sql.close()
    }

    def onStartUp(){
        try{
            def report = importJsonData(getNewDataFromWebsite())
            return report
        }
        catch(Throwable t){
            t.printStackTrace()
            return [success: 0, doubles: 0, problems: ["Es gab ein Problem beim Import der neuen Daten: $t.message"]]
        }
    }

    def getNewDataFromWebsite(){
        return new URL("http", "www.silverymoon.de", 80, "/O2qApbbYfMXtGHZunSf0fMRrH8EbcVjgu4tj0BODJcX16d/V6Wod8E/kycOWzgQh/sJvQP6VkYrbtomYbagtfzSKQvA/sOF6exIovi5bvXSwuc1FyikVv8TttWD7XhZFK").getText()
    }

    def importJsonData(jsonStr){
        def slurper = new JsonSlurper()
        def results = slurper.parseText(jsonStr)
        def report = [success:0, doubles: 0, problems: []]
        results.each{ tick ->
          addTicketFromWebsite(tick, report)
        }
        return report
    }

    def addTicketFromWebsite(tickdict, report){
        def parsed_date = parseDate(tickdict.date)
        if(parsed_date != null){
            def (nts, ntsr, ntf, ntfr) = parseNumber(tickdict.tickettype, tickdict.num_tickets)
            def res_nr = tickdict.id as String
            def contact = "$tickdict.email // $tickdict.phone"
            if(sql.rows("select * from ticket where reservation_nr = $res_nr").size() == 0){
                ticketmodel.addRow([tickdict.name, parsed_date, asInt(res_nr), contact, tickdict.note, ntf, ntfr, nts, ntsr, 0, false])
                report.success++
            }
            else {
                report.doubles++
            }
        }
        else {
            report.problems << "Datum nicht gefunden: $tickdict.date, Karten für $tickdict.name mit Reservierungsnr. $tickdict.id"
        }
    }

    def parseDate(date){
        def mstr = date.replaceAll("-", "%")
        def drow = sql.rows("select event_date from event where event_date like $mstr")
        if(drow.size() > 0){
            return drow[0].event_date
        }
        return null
    }

    def parseNumber(type, number){
        def num = asInt(number.replaceAll("Karten-", ""))
        switch(type){
            case "classique-gourmet": return [0, 0, num, 0]
            case "classique-pur": return [num, 0, 0, 0]
            case "classique-gourmet-student": return [0, 0, 0, num]
            case "classique-pur-student": return [0, num, 0, 0]
            default: return [0, 0, 0, 0]
        }
    }

    def checkDatabase(){
        checkColumnExists("ticket", "packaged", "boolean default false")
        checkColumnExists("ticket", "reservation_nr", "varchar(5) default ''")
        checkColumnExists("ticket", "note", "varchar(500) default ''")
        checkColumnExists("ticket", "contact", "varchar(64) default ''")
        checkColumnExists("ticket", "deleted", "boolean default false")
        checkColumnExists("event", "deleted", "boolean default false")
    }

    def checkColumnExists(table, column, type){
        try{
            sql.execute("select " + column + " from " + table)
        }
        catch(SQLException e){
            sql.execute("alter table " + table + " add column " + column + " " + type)
        }
    }

    def prepareModels(){
        def tds = sql.dataSet("ticket")
        ticketmodel = new LambdaDataSetTableModel(tds, "ticket", "id", ["name", "event_date", "reservation_nr", "contact", "note", "num_food_tickets", "num_food_tickets_reduced", "num_show_tickets", "num_show_tickets_reduced", "amount_paid", "packaged"], ["Name                                                 ", "Datum         ", "Resnr.", "Kontakt", "Notiz                                   ", "Gourmet", "erm. Gourmet", "Pur", "erm. Pur", "bezahlt", "im Umschlag", "noch zu zahlen"], ["filterPaid": this.&filterPaid, "filterDate": this.&filterDate, "filterPackaged" : this.&filterPackaged, "filterName": this.&filterName], ["noch zu zahlen" : this.&getPrice], this.filterSetter(5..8, [9]))
        def eds = sql.dataSet("event")
        eventmodel = new LambdaDataSetTableModel(eds, "event", "id", ["event_date", "capacity_food", "capacity_show", "price_food_full", "price_food_reduced", "price_show_full", "price_show_reduced"], ["Datum         ", "Plätze Gourmet", "Plätze Pur", "Preis Gourmet", "Preis erm. Gourmet", "Preis Pur", "Preis erm. Pur", "frei Gourmet", "frei Pur", "gesamt belegt"], [:], ["frei Gourmet": this.&getCapacityFood, "frei Pur": this.&getCapacityShow, "gesamt belegt": this.&getFull], this.filterSetter([1, 2], 3..6))
        def etmc = this.&eventUpdated as TableModelListener
        eventmodel.addTableModelListener(etmc)
        def ttmc = this.&ticketUpdated as TableModelListener
        ticketmodel.addTableModelListener(ttmc)
        comboboxmodeltable = makeComboBoxModel()
        comboboxmodelbutton = makeComboBoxModel()
    }

    def prepareDatabase(){
        sql.execute("""
        create table if not exists event(
          id integer not null auto_increment primary key,
          event_date varchar(12),
          capacity_food integer default 0,
          capacity_show integer default 0,
          price_food_full double default 0,
          price_food_reduced double default 0,
          price_show_full double default 0,
          price_show_reduced double default 0,
          deleted boolean default false)
        """)
        sql.execute("""
        create table if not exists ticket(
          id integer not null auto_increment primary key,
          name varchar(256),
          event_date varchar(12) references event(event_date) on update cascade,
          reservation_nr varchar(5) default '',
          note varchar(500) default '',
          contact varchar(64) default '',
          num_food_tickets integer default 0,
          num_food_tickets_reduced integer default 0,
          num_show_tickets integer default 0,
          num_show_tickets_reduced integer default 0,
          amount_paid double default 0,
          packaged boolean default false,
          deleted boolean default false)
        """)
        checkDatabase()
    }

}

