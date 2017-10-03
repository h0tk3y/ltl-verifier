data class KripkeState(val trueVars: Set<Variable>) {
    override fun toString() = trueVars.joinToString { it.name }
}

class KripkeModel(
        val states: Set<KripkeState>,
        val initialState: KripkeState,
        val transitions: Map<KripkeState, Set<KripkeState>>)

fun kripkeModelFromAutomaton(booleanAutomaton: BooleanAutomaton): KripkeModel {
    data class TransitionWithEffects(val to: State,
                                     val action: List<Action>,
                                     val event: Event)

    val transitions = booleanAutomaton.states.values.associate { state ->
        state to state.outgointTransitions.map { edge ->
            val to = booleanAutomaton.states.values.single { edge in it.incomingTransitions }
            TransitionWithEffects(to, edge.actions, edge.event)
        }
    }

    fun varOf(any: Any) = when (any) {
        is State -> Variable(any.name)
        is Action -> Variable(any.name)
        is Event -> Variable(any.name)
        else -> throw IllegalArgumentException()
    }

    val initialState = KripkeState(setOf(varOf(booleanAutomaton.startState)))
    val resultStates = mutableSetOf(initialState)
    val resultTransitions = mutableMapOf<KripkeState, MutableSet<KripkeState>>()

    do {
        var changed = false

        for (b in resultStates.toSet()) {
            val s = b.trueVars.singleOrNull()?.let { q -> booleanAutomaton.states.values.find { it.name == q.name } }
                    ?: continue

            // For each state that reaches this world, assume that the automaton makes a transition by one of its edge
            val possibleTransitions = transitions[s]!!

            for ((to, actions, event) in possibleTransitions) {
                var lastState = b
                val interWorlds = listOf(setOf(varOf(s), varOf(event))) + actions.map { setOf(varOf(s), varOf(event), varOf(it)) }
                for (interWorld in interWorlds) {
                    val interState = KripkeState(interWorld)
                    if (resultStates.none { it.trueVars == interWorld }) {
                        resultStates.add(interState)
                        changed = true
                    }
                    resultTransitions.getOrPut(lastState) { hashSetOf() }.add(interState)
                    lastState = interState
                }

                val toWorld = setOf(Variable(to.name))
                val toState = KripkeState(toWorld)

                if (resultStates.none { it.trueVars == toWorld }) {
                    resultStates.add(toState)
                    changed = true
                }

                resultTransitions.getOrPut(lastState) { hashSetOf() }.add(toState)
            }
        }
    } while (changed)

    return KripkeModel(resultStates, initialState, resultTransitions)
}