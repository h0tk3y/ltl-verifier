
import java.io.File
import java.util.*

fun main(args: Array<String>) {
    test1()
//    test()

    val xmls = File("src/test/resources/automata").listFiles()
    for (x in xmls) {
        print("Read $x: ")
        val diagram = parseDiagram(x.readText())
        val automaton = automatonFromDiagram(diagram)
        val kripke = kripkeModelFromAutomaton(automaton)
        val buchi = buchiAutomatonFromKripkeModel(kripke)
        variableAutomatonToPropAutomaton(buchi)
        println("OK")
    }
}

fun test() {
    val diagram = parseDiagram(File("src/test/resources/automata/test1.xml").readText())
    val automaton = automatonFromDiagram(diagram)
    val kripke = kripkeModelFromAutomaton(automaton)
    val buchi = buchiAutomatonFromKripkeModel(kripke)
    val buchiProp = variableAutomatonToPropAutomaton(buchi)

    val formula = Not(parseLtlFormula("G(F(PRESTART))"))
    val buchiForLtl = buchiAutomatonFromLtl(negationNormalForm(formula))

    val crossAutomaton = cross(buchiProp, buchiForLtl)
    val cycle = findCycles(crossAutomaton)

    println(cycle)
}

fun test1() {
    val a1 = BuchiState(1)
    val a2 = BuchiState(2)
    val a3 = BuchiState(3)
    val a4 = BuchiState(4)
    val aa = BuchiAutomaton(a1, mapOf(
            a1 to listOf(emptySet<Prop>() to a2),
            a2 to listOf(emptySet<Prop>() to a3),
            a3 to listOf(emptySet<Prop>() to a1, emptySet<Prop>() to a4),
            a4 to listOf(setOf(Prop("p"), Prop("q")) to a1)
    ))

//    val x1 = BuchiState("s")
//    val x2 = BuchiState("r")
//    val xx = BuchiAutomaton(x1, mapOf(
//            x1 to listOf(setOf<Prop>() to x1,
//                         setOf(Prop("p")) to x1,
//                         setOf(Prop("q")) to x1,
//                         setOf(Prop("p"), Prop("q")) to x1,
//                         setOf(Prop("p")) to x2,
//                         setOf(Prop("p"), Prop("q")) to x2),
//            x2 to listOf(setOf<Prop>() to x2,
//                         setOf(Prop("p")) to x2)
//    ))

    val f = negationNormalForm(parseLtlFormula("F(p & X(G(!q)))"))
    val xx = buchiAutomatonFromLtl(f)
    val ax = cross(aa, xx)
    val cycle = findCycles(ax)
    println(cycle)
}

private enum class Color { WHITE, GREY, BLACK }

fun <T> findCycles(buchiAutomaton: BuchiAutomaton<T>): List<BuchiState>? {
    val colors = buchiAutomaton.transitions.keys.associateTo(hashMapOf()) { it to Color.WHITE }
    val nodesStack = Stack<BuchiState>()

    var foundCycle: List<BuchiState>? = null

    fun dfs(buchiState: BuchiState) {
        if (colors[buchiState] == Color.BLACK) return
        if (colors[buchiState] == Color.GREY) {
            foundCycle = nodesStack.toList();
            return@dfs
        }

        nodesStack.push(buchiState)

        colors[buchiState] = Color.GREY

        for ((_, to) in buchiAutomaton.transitions[buchiState]!!) {
            dfs(to)
        }

        colors[buchiState] = Color.BLACK
        nodesStack.pop()
    }

    for (s in buchiAutomaton.transitions.keys) {
        dfs(s)
    }

    return foundCycle
}