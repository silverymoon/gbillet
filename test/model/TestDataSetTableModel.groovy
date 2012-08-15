
import groovy.util.GroovyTestCase
import groovy.sql.*
import billet.model.DataSetTableModel

class TestDataSetTableModel extends GroovyTestCase {

    def dstm
    def sql

    void setUp(){
        sql = Sql.newInstance("jdbc:h2:mem:", "", "", "org.h2.Driver")
        sql.execute("create table if not exists foo (id int auto_increment primary key, string varchar(100), nr double, mydate varchar(12), deleted boolean) as select * from csvread('fixtures/fixture.csv')")
        dstm = new DataSetTableModel(sql.dataSet("foo"), "foo", "id", ["string", "nr", "mydate"], ["Str", "Nr", "Myd"], ["2005": this.&filterByYear, "5ltrs": this.&filterByLetters], this.&filterSetter)
    }

    def filterSetter(val, row, col){
        if(col == 1){
            try {
                return val as int
            }
            catch(Exception e){
                return 0
            }
        }
        else {
            return val
        }
    }

    void tearDown(){
        sql.execute("drop table foo")
        sql.close()
    }

    void testRowCount(){
        assert dstm.getRowCount() == 5
    }

    void testColumnCount(){
        assert dstm.getColumnCount() == 3
    }

    void testGetValueAt(){
        assert dstm.getValueAt(0, 1) == 34
        assert dstm.getValueAt(0, 0) == "apple"
        // last row is special!
        assert dstm.getValueAt(4, 0) == ''
        assert dstm.getValueAt(4, 1) == 0
    }

    void testSetValueAt(){
        // update
        dstm.setValueAt("lalelu", 1,0)
        assert dstm.getValueAt(1, 0) == "lalelu"
        assert dstm.getValueAt(0, 0) == "apple"
        // bad value update
        dstm.setValueAt("foo", 2, 1)
        assert dstm.getValueAt(2, 1) == 0
        // insert
        dstm.setValueAt("42", 4, 1)
        assert dstm.getValueAt(5, 0) == ''
        assert dstm.getValueAt(4, 1) == 42
        assert dstm.getValueAt(4, 0) == null
        assert dstm.getRowCount() == 6
        dstm.setValueAt("2.2.2011", 4, 2)
        assert dstm.getValueAt(4, 2) == '2.2.2011'
        def rows = sql.rows("select * from foo where not deleted")
        assert rows[0][0] == 1
        assert rows[1][1] == "lalelu"
        assert rows[2][3] == "04.04.2005"
        assert rows[4][2] == 42
        assert rows[4][3] == "2.2.2011"
        // bad value insert
        dstm.setValueAt("foobar", 5, 1)
        assert dstm.getValueAt(5, 1) == 0
    }

    void testGetColumnClass(){
        assert dstm.getColumnClass(0) == String.class
        assert dstm.getColumnClass(1) == Double.class
        // check empty table
        dstm.deleteRow(0)
        dstm.deleteRow(0)
        dstm.deleteRow(0)
        dstm.deleteRow(0)
        assert dstm.getRowCount() == 1
        assert dstm.getColumnClass(0) == Object.class
        assert dstm.getColumnClass(1) == Object.class
    }

    void testDeleteRow(){
        dstm.deleteRow(1)
        assert dstm.getRowCount() == 4
        assert dstm.getValueAt(1, 0) == "§hoes"
        assert dstm.getValueAt(2, 0) == "Blabla"
    }

    void testAddRow(){
        dstm.addRow(["hi", 42, "lal"])
        assert dstm.getRowCount() == 6
        assert dstm.getValueAt(4, 0) == "hi"
        assert dstm.getValueAt(4, 1) == 42
        assert dstm.getValueAt(5, 0) == ""
    }

}