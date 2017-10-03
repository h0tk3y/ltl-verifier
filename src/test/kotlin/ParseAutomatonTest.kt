
import java.io.File

fun main1(args: Array<String>) {
//    test1()
    test()

//    val xmls = File("src/test/resources/automata").listFiles()
//    for (x in xmls) {
//        print("Read $x: ")
//        val diagram = parseDiagram(x.readText())
//        val automaton = automatonFromDiagram(diagram)
//        val kripke = kripkeModelFromAutomaton(automaton)
//        val buchi = buchiAutomatonFromKripkeModel(kripke)
//        variableAutomatonToPropAutomaton(buchi)
//        println("OK")
//    }
}

fun test() {
    val diagram = parseDiagram(File("src/test/resources/automata/test1.xml").readText())
    val automaton = automatonFromDiagram(diagram)
    val kripke = kripkeModelFromAutomaton(automaton)
    val buchi = buchiAutomatonFromKripkeModel(kripke)
    val buchiProp = variableAutomatonToPropAutomaton(buchi)
    val props = buchiProp.transitions.flatMap { it.value.flatMap { it.first } }.toSet()

    val formula = Not(parseLtlFormula("G(F(PRESTART))"))
    val buchiForLtl = buchiAutomatonFromLtl(negationNormalForm(formula)) // todo accepting states???

    val crossAutomaton = crossIgnoringProps(buchiProp, buchiForLtl, props - formula.variables())
    val cycle = findReachableCycle(crossAutomaton) // todo bad dfs algorithm

    println(cycle)
}

fun main(args: Array<String>) {
    val (a1, a2, a3, a4) = (1..4).map { BuchiState(it) }

    val aa = BuchiAutomaton(a1, mapOf(
            a1 to listOf(setOf(Prop("q")) to a2),
//            a1 to listOf(emptySet<Prop>() to a2),
            a2 to listOf(emptySet<Prop>() to a3),
            a3 to listOf(emptySet<Prop>() to a1, emptySet<Prop>() to a4),
            a4 to listOf(setOf(Prop("p"), Prop("q")) to a1)
    ), setOf(setOf(a1, a2, a3, a4)))

    verifyNnf(aa, negationNormalForm(parseLtlFormula("F(p & X(G(!q)))")))
}


