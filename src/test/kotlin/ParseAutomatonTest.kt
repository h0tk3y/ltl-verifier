
import java.io.File

fun main(args: Array<String>) {
    val xmls = File("src/test/resources/automata").listFiles()
    for (x in xmls) {
        print("Read $x: ")
        val diagram = parseDiagram(x.readText())
        val automaton = automatonFromDiagram(diagram)
        val kripke = buchiFromBooleanAutomaton(automaton)
        println("OK")
    }
}

