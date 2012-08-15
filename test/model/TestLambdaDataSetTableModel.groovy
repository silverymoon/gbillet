import javax.swing.event.TableModelListener
import javax.swing.event.TableModelEvent
import java.util.concurrent.atomic.AtomicBoolean

import groovy.util.GroovyTestCase
import groovy.sql.Sql
import billet.model.LambdaDataSetTableModel

class TestLambdaDataSetTableModel extends GroovyTestCase {

    def ldstm
    def sql

    void setUp(){
        sql = Sql.newInstance("jdbc:h2:mem:", "", "", "org.h2.Driver")
        sql.execute("create table if not exists foo (id int auto_increment primary key, string varchar(100), nr double, mydate varchar(12), deleted boolean) as select * from csvread('fixtures/fixture.csv')")
        ldstm = new LambdaDataSetTableModel(sql.dataSet("foo"), "foo", "id", ["nr", "string"], ["Nr", "String", "Nrquadrat"], [:], ["Nrquadrat" : {row -> def x = row["nr"] as double; x * x }], null)
    }

    void testGetColumnCount(){
        assert ldstm.getColumnCount() == 3
    }

    void testIsCellEditable(){
        assert ldstm.isCellEditable(0, 0)
        assert !ldstm.isCellEditable(0, 2)
    }

    void testGetValueAt(){
        assert ldstm.getValueAt(0, 0) == 34.0
        assert ldstm.getValueAt(1, 0) == -1.5
        assert ldstm.getValueAt(0, 2)== 1156.0
        assert ldstm.getValueAt(1, 2) == 2.25
    }

    void testAddRowAndGetValue(){
        ldstm.setValueAt("4", 4, 0)
        assert ldstm.getValueAt(4, 2) == 16
        ldstm.setValueAt("foo", 5, 1)
        ldstm.setValueAt("8", 5, 0)
        assert ldstm.getValueAt(5, 2) == 64
    }

    void testListenerUpdate(){
        def AtomicBoolean listenerFired = new AtomicBoolean(false)
        def test = {listenerFired.set(true)} as TableModelListener
        ldstm.addTableModelListener(test)
        ldstm.setValueAt("3", 4, 0)
        assert listenerFired.get()
    }

    void testGetColumnClass(){
        assert ldstm.getColumnClass(1) == String.class
        assert ldstm.getColumnClass(2) == Double.class
    }

}