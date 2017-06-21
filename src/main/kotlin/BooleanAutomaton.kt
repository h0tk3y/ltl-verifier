data class State(val id: Int,
                 val name: String,
                 val incomingTransitions: List<Edge>,
                 val outgointTransitions: List<Edge>)

data class Variable(val name: String, val volatile: Boolean = false)

data class Assignment(val variable: Variable, val value: Int)

data class Edge(val id: Int,
                val code: List<Assignment>,
                val guard: GuardExpression?,
                val actions: List<Action>,
                val event: Event)

class BooleanAutomaton(val variables: Map<Variable, Int>,
                       val events: Map<String, Event>,
                       val states: Map<Int, State>,
                       val startState: State,
                       val edges: Map<Int, Edge>)

fun parseCode(string: String, variables: Set<Variable>): List<Assignment> =
        string.lines().filter { !it.isBlank() }.map {
            val (l, r) = it.trim().removeSuffix(";").split("\\s+=\\s+".toRegex())
            Assignment(variables.find { it.name == l }!!, r.toInt())
        }

fun parseDecl(decl: String): Pair<Variable, Int> {
    val isVolatile = decl.startsWith("volatile")
    val (name, value) = decl
            .removePrefix("volatile ")
            .removePrefix("bool ")
            .removeSuffix(";")
            .split("\\s+=\\s+".toRegex())
    return Variable(name, isVolatile) to value.toInt()
}

fun automatonFromDiagram(diagram: Diagram): BooleanAutomaton {
    val variables = diagram.data.Statemachine.variable.orEmpty().associate { parseDecl(it.decl) }

    val events = diagram.data.Statemachine.event.orEmpty().associateBy { it.name }

    val edges = diagram.widget
            .filter { it.type == "Transition" }
            .map {
                Edge(it.id,
                     parseCode(it.attributes.code ?: "", variables.keys),
                     it.attributes.guard?.let { parseGuardExpression(it, variables.keys) },
                     it.attributes.action.orEmpty(),
                     it.attributes.event!!)
            }
            .associateBy { it.id }

    val states = diagram.widget.filter { it.type == "State" }.map {
        State(it.id,
              it.attributes.name!!,
              it.attributes.incoming.orEmpty().map { edges[it.id]!! },
              it.attributes.outgoing.orEmpty().map { edges[it.id]!! })
    }.associateBy { it.id }

    return BooleanAutomaton(variables, events, states, states[0]!!, edges)
}