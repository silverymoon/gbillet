package billet

import billet.view.BilletView
import billet.model.BilletDAO
import javax.swing.JOptionPane

class BilletApp{
    def bview
    def bdao

    BilletApp(){
        bdao = new BilletDAO()
        bview = new BilletView(bdao)
    }

    def show(){
        def report = bdao.onStartUp()
        JOptionPane.showMessageDialog(bview.gui, "Anzahl importierter Daten: $report.success \nAnzahl schon vorhandener Daten: $report.doubles \n Probleme:$report.problems", "Import der Daten", JOptionPane.INFORMATION_MESSAGE)
        bview.show()
    }

    static void main(String[] args){
        def app = new BilletApp()
        app.show()
    }

}
