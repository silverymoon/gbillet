import javax.swing.event.TableModelListener
import javax.swing.event.TableModelEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.RowFilter

import groovy.util.GroovyTestCase
import groovy.sql.*
import billet.model.BilletDAO

class TestBilletDAO extends GroovyTestCase {

    def bdao
    def sql

    void setUp(){
        sql = Sql.newInstance("jdbc:h2:mem:", "org.h2.Driver")
        bdao = new BilletDAO(sql)
        sql.execute("insert into event select * from csvread('fixtures/events.csv')")
        sql.execute("insert into ticket select * from csvread('fixtures/tickets.csv')")
        bdao.prepareModels()
    }

    void testGetPrice(){
        def rows = sql.rows("select * from ticket order by id")
        assert bdao.getPrice(rows[0]) == 0
        assert bdao.getPrice(rows[1]) == 54
        assert bdao.getPrice(rows[2]) == -6
        assert bdao.getPrice(rows[3]) == 9
        assert bdao.getPrice(rows[4]) == 0
    }

    void testGetCapacityFood(){
        def rows = sql.rows("select * from event order by id")
        assert bdao.getCapacityFood(rows[0]) == 4
        assert bdao.getCapacityFood(rows[1]) == 0
    }

    void testGetCapacityShow(){
        def rows = sql.rows("select * from event order by id")
        assert bdao.getCapacityShow(rows[0]) == 2
        assert bdao.getCapacityShow(rows[1]) == 5
    }

    void testTicketListenerUpdate(){
        def AtomicInteger listenerFired = new AtomicInteger(0)
        def test = {TableModelEvent tme -> listenerFired.addAndGet(tme.getFirstRow())} as TableModelListener
        bdao.getEventTableModel().addTableModelListener(test)
        def tme = new TableModelEvent(bdao.getTicketTableModel(), 1, 1, 5)
        bdao.ticketUpdated(tme)
        assert listenerFired.get() == 3
    }

    void testEventListenerUpdate(){
        def AtomicInteger listenerFired = new AtomicInteger(0)
        def test = {TableModelEvent tme -> listenerFired.addAndGet(tme.getFirstRow())} as TableModelListener
        bdao.getTicketTableModel().addTableModelListener(test)
        def tme = new TableModelEvent(bdao.getEventTableModel(), 1, 1, 4)
        bdao.eventUpdated(tme)
        assert listenerFired.get() == 4
    }

    void testisCapacityEnough(){
        assert !bdao.isCapacityEnough(16, 0, 4)
        assert !bdao.isCapacityEnough(6, 1, 5)
        assert bdao.isCapacityEnough(0, 0, 3)
        assert bdao.isCapacityEnough(1, 4, 2)
    }

    void testComboBoxModel(){
        def cbm = bdao.getComboBoxModelButton()
        assert cbm.getSize() == 2
        assert cbm.getElementAt(0) == "12.10.2011"
        assert cbm.getElementAt(1) == "13.10.2011"
        bdao.getEventTableModel().setValueAt("heute", 0, 0)
        assert cbm.getElementAt(0) == "heute"
    }

    def makeEntry(row, id){
        return [
            getIdentifier: {id},
            getModel: {bdao.getTicketTableModel()},
            getValue: {idx -> row[idx]},
            getValueCount: {42},
        ] as RowFilter.Entry

    }

    void testFilterDate(){
        def rf = bdao.getDateFilter("12.10.2011")
        def rows = sql.rows("select * from ticket where not deleted order by id")
        assert rf.include(makeEntry(rows[0], 0))
        assert !rf.include(makeEntry(rows[1], 1))
        assert rf.include(makeEntry(rows[2], 2))
    }

    void testFilterPaid(){
        def rf = bdao.getPaidFilter()
        def rows = sql.rows("select * from ticket where not deleted order by id")
        assert rf.include(makeEntry(rows[0], 0))
        assert !rf.include(makeEntry(rows[1], 1))
        assert rf.include(makeEntry(rows[2], 2))
        assert !rf.include(makeEntry(rows[3], 3))
    }

    void testFilterPackaged(){
        def rf = bdao.getPackagedFilter()
        def rows = sql.rows("select * from ticket where not deleted order by id")
        assert !rf.include(makeEntry(rows[0], 0))
        assert rf.include(makeEntry(rows[2], 2))
        assert !rf.include(makeEntry(rows[3], 3))
        assert rf.include(makeEntry(rows[4], 4))
    }

    void testFilterName(){
        def rf = bdao.getNameFilter("S")
        def rows = sql.rows("select * from ticket where not deleted order by id")
        assert !rf.include(makeEntry(rows[0], 0))
        assert rf.include(makeEntry(rows[1], 1))
        assert !rf.include(makeEntry(rows[2], 2))
        assert rf.include(makeEntry(rows[3], 3))
    }

    void testFilterSetter(){
        bdao.ticketmodel.setValueAt("4", 1, 5)
        assert bdao.ticketmodel.getValueAt(1, 5) == 4
        bdao.eventmodel.setValueAt("57", 2, 2)
        assert bdao.eventmodel.getValueAt(2, 2) == 57
        bdao.ticketmodel.setValueAt("foo", 1, 5)
        assert bdao.ticketmodel.getValueAt(1, 5) == 0
        bdao.eventmodel.setValueAt("bar", 3, 2)
        assert bdao.eventmodel.getValueAt(3, 2) == 0
        // check inserting doubles!
        bdao.eventmodel.setValueAt("3.14", 2, 4)
        assert bdao.eventmodel.getValueAt(2, 4) == 3.14
    }

    void testCheckColumnExists(){
        bdao.checkColumnExists("ticket", "name", "int")
        bdao.checkColumnExists("ticket", "foo", "int default 42")
        assert sql.rows("select foo from ticket where id = 1")[0].foo == 42
    }

    /*void testGetNewDataFromWebsite(){
        assert bdao.getNewDataFromWebsite().size() == 3
    }*/

    void testImportJsonData(){
        def AtomicInteger listenerFired = new AtomicInteger(0)
        def test = {TableModelEvent tme -> listenerFired.addAndGet(tme.getFirstRow())} as TableModelListener
        bdao.getTicketTableModel().addTableModelListener(test)
        bdao.importJsonData('[{"ip": "129.27.110.28", "phone": "0316 873 7515 oder 0316 391856", "num_tickets": "Karten-2", "date": "12-10-2011", "id": 43, "tickettype": "classique-gourmet", "name": "Herr Rudolf Eichinger", "submitted": "2010-07-21 15:39:14", "reduced": " ", "note": "Hallo!", "email": "r.eichinger@tugraz.at"}, {"ip": "178.191.204.139", "phone": "461986", "num_tickets": "Karten-4", "date": "13-10-2011", "id": 44, "tickettype": "classique-pur-student", "name": "Herr Adalbert Lernpeiss", "submitted": "2010-07-22 12:57:50", "note": "", "email": "verena.lernpeiss@student.tugraz.at"}]')
        assert listenerFired.get() > 1
        assert sql.rows("select * from ticket where reservation_nr = '44'")[0].num_show_tickets_reduced == 4
        assert sql.rows("select * from ticket where reservation_nr = '43'")[0].num_food_tickets == 2
    }

    void testAddTicketFromWebsite(){
        def AtomicInteger listenerFired = new AtomicInteger(0)
        def test = {TableModelEvent tme -> listenerFired.addAndGet(tme.getFirstRow())} as TableModelListener
        bdao.getTicketTableModel().addTableModelListener(test)
        def report = [problems: [], success: 0, doubles: 0]
        bdao.addTicketFromWebsite([phone: '123', email: 'b@b.b', num_tickets: 'Karten-3', date: '12-10-2011', id: '1337', tickettype: 'classique-pur', name: 'J. Bond', note: 'Q is da bomb'], report)
        // check new ticket in model
        def row = sql.rows("select * from ticket where reservation_nr = '1337'")[0]
        assert row.num_show_tickets == 3
        assert row.name == 'J. Bond'
        assert row.event_date == '12.10.2011'
        assert row.contact == 'b@b.b // 123'
        assert row.num_food_tickets_reduced == 0
        assert report.success == 1
        // and check the event!
        assert listenerFired.get() > 1
        bdao.addTicketFromWebsite([phone: 'a', email: 'b', num_tickets: 'Karten-3', date: '13-10-2011', id: '1337', tickettype: 'classique-pur', name: 'Susi', note: 'DOPPELT'], report)
        assert sql.rows("select * from ticket where reservation_nr = '1337'").size() == 1
        assert report.doubles == 1
        bdao.addTicketFromWebsite([phone: 'a', email: 'b', num_tickets: 'Karten-3', date: 'blablabla', id: '42235', tickettype: 'classique-pur', name: 'Futzbutz', note: 'Q is da bomb'], report)
        assert sql.rows("select * from ticket where reservation_nr = '42235'").size() == 0
        assert report.problems[0].indexOf("Karten für Futzbutz mit Reservierungsnr. 42235") > 0
    }

    void testParseDate(){
        assert bdao.parseDate("12-10-2011") == "12.10.2011"
        assert bdao.parseDate("12.10.2011") == "12.10.2011"
        assert bdao.parseDate("foo") == null
    }

    void testParseNumber(){
        assert bdao.parseNumber("classique-pur-student", "Karten-2") == [0, 2, 0, 0]
        assert bdao.parseNumber("classique-gourmet-student", "Karten-4") == [0, 0, 0, 4]
        assert bdao.parseNumber("classique-pur-student", "Karten-7") == [0, 7, 0, 0]
        assert bdao.parseNumber("classique-gourmet", "Karten-3") == [0, 0, 3, 0]
        assert bdao.parseNumber("classique-gourmet-student", "Karten-1") == [0, 0, 0, 1]
        assert bdao.parseNumber("classique-gourmet", "Karten-0") == [0, 0, 0, 0]
        assert bdao.parseNumber("classique-pur-student", "Karten-0") == [0, 0, 0, 0]
    }

}