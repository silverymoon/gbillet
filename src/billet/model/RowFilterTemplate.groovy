package billet.model
import javax.swing.RowFilter

class RowFilterTemplate extends RowFilter {
    def id
    def closure

    RowFilterTemplate(name, cls){
        this.id = name
        this.closure = cls
    }

    String toString(){
        return id
    }

    boolean include(RowFilter.Entry entry){
        return closure(entry)
    }

    int hashCode(){
        return id.hashCode()
    }

    boolean equals(obj){
        return obj?.id == id
    }
}
