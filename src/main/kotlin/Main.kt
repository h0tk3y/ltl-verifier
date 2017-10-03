import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.DefaultHelpFormatter
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import java.io.File

class CliArgs(parser: ArgParser) {
    val diagramXmlFile by parser.storing(
        names = "--xml",
        help = "Diagram XML file of the model",
        transform = ::File)

    val ltlFormulaString by parser.storing(
        names = "--ltl-string",
        help = "LTL formula to verify"
    ).default(null)

    val ltlFormulaeFile by parser.storing(
        names = "--ltl-file",
        help = "File containing lines each representing an LTL formula",
        transform = ::File
    ).default(null)
}

sealed class VerificationResult
object Correct : VerificationResult()
class CounterExample(val states: List<BuchiState<*>>) : VerificationResult()

fun showResult(formula: Formula, result: VerificationResult) {
    println("Formula: $formula")
    when (result) {
        is Correct -> println("Correct.")
        is CounterExample -> {
            println("Found counter-example:")
            val states = result.states
            val prefix = states.dropLast(1).dropLastWhile { it != states.last() }
            val cycle = states.dropLast(1).takeLastWhile { it != states.last() }.let {
                if (it.size == states.size - 1) emptyList() else it
            } + states.last()
            println("Path:\n${prefix.joinToString("\n") { (it.tag as Pair<*, *>).first.toString() }}")
            println("--- cycle:\n${cycle.joinToString("\n") { (it.tag as Pair<*, *>).first.toString() }}")
        }
    }
    println()
}

fun main(args: Array<String>) {
    mainBody {
        CliArgs(ArgParser(args, helpFormatter = DefaultHelpFormatter())).apply {
            val diagram = parseDiagram(diagramXmlFile.readText())
            if (ltlFormulaString != null) {
                val formula = parseLtlFormula(ltlFormulaString!!)
                val result = runVerification(diagram, formula)
                showResult(formula, result)
            } else if (ltlFormulaeFile != null) {
                ltlFormulaeFile!!.readLines().map { parseLtlFormula(it) }.forEach { formula ->
                    val result = runVerification(diagram, formula)
                    showResult(formula, result)
                }
            } else {
                print("Enter formula:")
                val formula = parseLtlFormula(readLine()!!)
                val result = runVerification(diagram, formula)
                showResult(formula, result)
            }
        }
    }
}

fun runVerification(diagram: Diagram, formula: Formula): VerificationResult {
    val automaton = automatonFromDiagram(diagram)
    val kripke = kripkeModelFromAutomaton(automaton)
    val buchi = buchiAutomatonFromKripkeModel(kripke)
    val buchiProp = variableAutomatonToPropAutomaton(buchi)

    return runVerificationOnAutomaton(buchiProp, formula)
}

fun runVerificationOnAutomaton(
    automaton: BuchiAutomaton<*, Set<Prop>>,
    formula: Formula
): VerificationResult {
    val negation = Not(formula)
    val nnf = negationNormalForm(negation)

    return verifyNnf(automaton, nnf)
}

fun verifyNnf(
    automaton: BuchiAutomaton<*, Set<Prop>>,
    nnf: Formula
): VerificationResult {

    val props = automaton.transitions.flatMap { it.value.flatMap { it.first } }.toSet()

    val generalizedBuchiForLtl = buchiAutomatonFromLtl(nnf)
    val buchiForLtl = degeneralize(generalizedBuchiForLtl)

    val crossAutomaton = crossIgnoringProps(automaton, buchiForLtl, props - nnf.variables())
    val cycle = findReachableCycle(crossAutomaton)

    return if (cycle != null) CounterExample(cycle) else Correct
}