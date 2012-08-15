import groovy.util.AllTestSuite
import junit.framework.TestResult

def tst(){
  def tr = new TestResult()
  def test = new AllTestSuite()
  test.suite("test/model/", "*").run(tr)
  println "...ran ${tr.runCount()} tests, ${tr.errorCount()} errors, ${tr.failureCount()} failures"
  if(!tr.wasSuccessful()){

    if(tr.errorCount() > 0){
      println "! ---> ERRORS: "

      tr.errors().each(){ err -> println "$err"
                                 println err.trace() }
    }
    if(tr.failureCount() > 0){
      println "! ---> FAILURES: "
      tr.failures().each(){ fail -> println "$fail"
                                    println fail.trace() }
    }
  }
}

tst()